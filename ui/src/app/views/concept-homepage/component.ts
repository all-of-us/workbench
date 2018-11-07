import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subject} from 'rxjs/Subject';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';


import {
  Concept,
  ConceptsService,
  Domain,
  DomainCount,
  DomainInfo,
  StandardConceptFilter,
  VocabularyCount,
} from 'generated';
import {ToolTipComponent} from '../tooltip/component';

interface ConceptCacheSet {
  domain: Domain;

  vocabularyList: Array<VocabularyCount>;

  items: Array<ConceptInfo>;
}

interface VocabularyCountSelected extends VocabularyCount {
  selected: boolean;
}
interface ConceptInfo extends Concept {
  selected: boolean;
}


@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    '../../styles/tooltip.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptHomepageComponent implements OnInit {
  loadingDomains = true;
  searchTerm = '';
  standardConceptsOnly = true;
  searching = false;
  currentSearchString = '';
  searchLoading = false;
  selectedDomain: DomainCount = {
    name: '',
    domain: undefined,
    conceptCount: 0
  };
  showSearchError = false;


  @ViewChild(ConceptTableComponent)
  conceptTable: ConceptTableComponent;

  @ViewChild(ConceptAddModalComponent)
  conceptAddModal: ConceptAddModalComponent;

  @ViewChild(ToolTipComponent)
  toolTip: ToolTipComponent;

  conceptDomainList: Array<DomainInfo> = [];
  conceptDomainCounts: Array<DomainCount> = [];

  // Maps Domain Name with number of concept selected
  selectedConceptDomainMap: Map<String, number>  = new Map<string, number>();
  concepts: Array<ConceptInfo> = [];
  conceptsCache: Array<ConceptCacheSet> = [];
  completedDomainSearches: Array<Domain> = [];
  placeholderValue = '';
  vocabularies: Array<VocabularyCountSelected> = [];
  wsNamespace: string;
  wsId: string;
  selectConceptMap = new Map<number, ConceptInfo>();

  // For some reason clr checkboxes trigger click events twice on click. This
  // is a workaround to not allow multiple filter events to get triggered.
  blockMultipleSearchFromFilter = true;
  maxConceptFetch = 100;
  conceptsSavedText = '';

  constructor(
    private conceptsService: ConceptsService,
    private route: ActivatedRoute,
  ) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  ngOnInit(): void {
    this.loadingDomains = true;
    this.conceptsService.getDomainInfo(this.wsNamespace, this.wsId).subscribe((response) => {
      this.conceptDomainList = response.items;
      this.conceptDomainList.forEach((domain) => {
        this.conceptsCache.push({
          domain: domain.domain,
          items: [],
          vocabularyList: []
        });
        this.conceptDomainCounts.push({
          domain: domain.domain,
          name: domain.name,
          conceptCount: 0
        });
        this.selectedDomain = this.conceptDomainCounts[0];
      });
      if (this.route.snapshot.queryParams['domain'] !== undefined) {
        this.browseDomain(
          this.conceptDomainList.find(
            domainInfo => domainInfo.domain === this.route.snapshot.queryParams['domain']));
      }
      this.loadingDomains = false;
    });
  }

  openAddModal(): void {
    this.conceptAddModal.open();
  }

  selectDomain(domainCount: DomainCount) {
    if (!this.selectedConceptDomainMap[domainCount.domain]) {
      this.selectedConceptDomainMap[domainCount.domain] = 0;
    }
    this.selectedDomain = domainCount;
    this.placeholderValue = this.noConceptsConstant;
    this.setConceptsAndVocabularies();
  }

  searchButton() {
    const searchTermLength = this.searchTerm.trim().length;
    if (searchTermLength === 0) {
      this.clearSearch();
      return;
    }
    if (searchTermLength < 3) {
      this.showSearchError = true;
      return;
    }
    this.showSearchError = false;
    this.currentSearchString = this.searchTerm;
    this.reset();
    this.searchConcepts();
  }

  returnToConcepts() {
    this.clearSearch();
    this.searching = false;
  }

  private rebuildSelectedConceptDomainMap() {
    this.selectedConceptDomainMap = new Map<string, number>();
    this.conceptDomainList.forEach((domain) => {
      this.selectedConceptDomainMap.set(domain.name, 0);
    });
  }

  reset() {
    this.rebuildSelectedConceptDomainMap();
    this.selectConceptMap.clear();
    this.conceptDomainCounts = [];
  }

  clearSearch() {
    this.searchTerm = '';
    this.currentSearchString = '';
    this.rebuildSelectedConceptDomainMap();
    this.searchConcepts();
  }

  browseDomain(domain: DomainInfo) {
    this.currentSearchString = '';
    this.selectedDomain =
      this.conceptDomainCounts.find(domainCount => domainCount.domain === domain.domain);
    this.searchConcepts();
  }

  searchConcepts() {
    if (this.conceptTable) {
      this.selectedConcept = [];
      this.conceptTable.selectedConcepts = [];
      this.rebuildSelectedConceptDomainMap();
    }
    this.searching = true;
    this.searchLoading = true;
    this.placeholderValue = this.noConceptsConstant;
    let standardConceptFilter: StandardConceptFilter;
    if (this.standardConceptsOnly) {
      standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
    } else {
      standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
    }
    this.completedDomainSearches = [];

    let numCalls = 0;
    const numCallsSubject = new Subject<Domain>();
    const numCallsSubject$ = numCallsSubject.asObservable();

    numCallsSubject$.subscribe((finishedCall) => {
      this.completedDomainSearches.push(finishedCall);
    });

    this.conceptsCache.forEach((conceptDomain) => {
      const activeTabSearch = conceptDomain.domain === this.selectedDomain.domain;
      const request = {
        query: this.currentSearchString,
        standardConceptFilter: standardConceptFilter,
        domain: conceptDomain.domain,
        includeDomainCounts: activeTabSearch,
        includeVocabularyCounts: true,
        maxResults: this.maxConceptFetch
      };
      this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
        .subscribe((response) => {
          numCalls += 1;
          numCallsSubject.next(conceptDomain.domain);
          conceptDomain.items = response.items.map((concept) => {
            return {
              ...concept,
              selected: false
            };
          });
          this.filterConceptSelection(conceptDomain.items);

          conceptDomain.vocabularyList = response.vocabularyCounts;
          if (activeTabSearch) {
            this.searchLoading = false;
            this.conceptDomainCounts = response.domainCounts;
            this.selectedDomain =
              this.conceptDomainCounts.find(domainCount => domainCount.domain === request.domain);
            this.setConceptsAndVocabularies();
          }
        });
    });
  }

  setConceptsAndVocabularies() {
    const cacheItem = this.conceptsCache.find(
      conceptDomain => conceptDomain.domain === this.selectedDomain.domain);
    this.concepts = cacheItem.items.map((concept) => {
      return {
        ...concept,
        selected: false
      };
    });

    this.filterConceptSelection(this.concepts);

    this.vocabularies = [];
    this.vocabularies = cacheItem.vocabularyList.map((vocabulary) => {
      return {
        ...vocabulary,
        selected: true
      };
    });
  }

  get getSearchDisabled() {
    return this.searchLoading || this.loadingDomains || this.searchTermNotLongEnough;
  }

  get searchTermNotLongEnough() {
    return this.searchTerm === undefined || this.searchTerm.length < 3;
  }

  filterList() {
    if (this.blockMultipleSearchFromFilter) {
      this.blockMultipleSearchFromFilter = false;
      return;
    }
    if (this.vocabularies.filter(vocabulary => vocabulary.selected).length === 0) {
      this.concepts = [];
      this.placeholderValue = 'No vocabularies selected. Please select at least one vocabulary.';
      return;
    }
    this.blockMultipleSearchFromFilter = true;
    let standardConceptFilter: StandardConceptFilter;
    if (this.standardConceptsOnly) {
      standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
    } else {
      standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
    }
    this.searchLoading = true;
    this.placeholderValue = this.noConceptsConstant;

    const request = {
      query: this.currentSearchString,
      standardConceptFilter: standardConceptFilter,
      domain: this.selectedDomain.domain,
      vocabularyIds: this.vocabularies
        .filter(vocabulary => vocabulary.selected).map(vocabulary => vocabulary.vocabularyId),
      maxResults: this.maxConceptFetch
    };
    this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
      .subscribe((response) => {
        this.searchLoading = false;
        this.concepts = response.items.map((vocabulary) => {
          return {
            ...vocabulary,
            selected: false
          };
        });
        this.selectedConcept.every((concept) => concept.selected = false);
        this.concepts.forEach((concept) => {
          const index = this.selectedConcept.find
            (select => select.conceptId === concept.conceptId);
          if (index) {
            concept.selected = true;
          }
        });
        this.conceptTable.selectedConcepts = [];
      });
  }

  selectConcept(concepts) {
    // TODO Check why clr-datagrid is sending empty and duplicate values
    concepts = concepts.filter((value) => !(value === undefined || value === null));
    this.selectConceptMap.clear();
    concepts.forEach((concept) => {
      this.selectConceptMap.set(concept.conceptId, concept);
    });
    this.selectedConcept =  Array.from(this.selectConceptMap.values());
    concepts = this.selectedConcept;

    const domainName = this.selectedDomain.domain;
    if (concepts && concepts.length > 0 ) {
      const filterConceptsCount = concepts
        .filter(concept => {
          return concept.domainId.toLowerCase() ===
            this.selectedDomain.domain.toString().toLowerCase();
<<<<<<< HEAD
        })
=======
          })
>>>>>>> PR COmments I
        .length;
      this.selectedConceptDomainMap[domainName] = filterConceptsCount;
    } else {
      this.selectedConceptDomainMap[domainName] = 0;
    }
  }

  afterConceptsSaved() {
    this.setConceptsSaveText();

    // Once concepts are saved clear the selection from concept homepage for active Domain
    this.conceptTable.selectedConcepts.length = 0;
    this.selectedConceptDomainMap[this.selectedDomain.domain] = 0;
    this.cloneCacheConcepts();
  }

  setConceptsSaveText() {
    const conceptsCount = this.selectedConceptDomainMap[this.selectedDomain.domain];
    this.conceptsSavedText = conceptsCount + ' ' + this.selectedDomain.name +
      (conceptsCount > 1 ? ' concepts ' : ' concept ') + 'have been added ';
    setTimeout(() => {
      this.conceptsSavedText = '';
    }, 5000);
  }

  /* This is done because clr-datagrid has a bug which causes unselected entries to
   appear as selected on refresh*/
  cloneCacheConcepts() {
    const cacheItem = this.conceptsCache.find(
      conceptDomain => conceptDomain.domain === this.selectedDomain.domain);
    const cloneConcepts = cacheItem.items.map(x => Object.assign({}, x));
    cacheItem.items = cloneConcepts;
  }

  get activeSelectedConceptCount(): number {
    if (!this.selectedDomain
      || !this.selectedDomain.domain
      || this.selectedConceptDomainMap.size === 0) {
      return 0;
    }
    return this.selectedConceptDomainMap[this.selectedDomain.domain];
  }

  get noConceptsConstant() {
    return 'No concepts found for domain \'' + this.selectedDomain.name + '\' this search.';
  }

  get addToSetText(): string {
    const count = this.activeSelectedConceptCount;
    return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
  }

  domainLoading(domain) {
    return this.searchLoading || !this.completedDomainSearches.includes(domain.domain);
  }

  filterConceptSelection(concepts) {
    concepts.forEach((concept) => {
      const exist = this.selectedConcept
          .find(select => select.conceptId === concept.conceptId);
      if (exist) {
        concept.selected = true;
      }
    });
  }

}
