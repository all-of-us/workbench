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

interface ConceptCacheSet {
  domain: Domain;

  vocabularyList: Array<VocabularyCount>;

  items: Array<Concept>;
}

interface VocabularyCountSelected extends VocabularyCount {
  selected: boolean;
}

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptHomepageComponent implements OnInit {
  loadingDomains = true;
  searchTerm: string;
  standardConceptsOnly = true;
  searching = false;
  currentSearchString: string;
  searchLoading = false;
  selectedDomain: Domain;
  addTextHovering = false;
  conceptSelected = false;
  selectedConcept: Concept[] = [];

  @ViewChild(ConceptTableComponent)
  conceptTable: ConceptTableComponent;

  @ViewChild(ConceptAddModalComponent)
  conceptAddModal: ConceptAddModalComponent;

  conceptDomainList: Array<DomainInfo> = [];
  conceptDomainCounts: Array<DomainCount> = [];

  concepts: Array<Concept> = [];
  conceptsCache: Array<ConceptCacheSet> = [];

  completedDomainSearches: Array<Domain> = [];

  vocabularies: Array<VocabularyCountSelected> = [];

  wsNamespace: string;
  wsId: string;

  // For some reason clr checkboxes trigger click events twice on click. This
  // is a workaround to not allow multiple filter events to get triggered.
  blockMultipleSearchFromFilter = true;


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
      });
      this.loadingDomains = false;
      this.selectedDomain = this.conceptDomainList[0].domain;
    });
  }

  openAddModal(): void {
    this.selectedConcept = this.conceptTable.selectedConcepts;
    this.conceptAddModal.open();
  }

  selectDomain(domain: Domain) {
    this.selectedDomain = domain;
    this.setConceptsAndVocabularies();
  }

  searchButton() {
    this.currentSearchString = this.searchTerm;
    this.searchConcepts();
  }

  browseDomain(domain: DomainInfo) {
    this.currentSearchString = '';
    this.selectedDomain = domain.domain;
    this.searchConcepts();
  }

  searchConcepts() {
    this.searching = true;
    this.searchLoading = true;
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
      const activeTabSearch = conceptDomain.domain === this.selectedDomain;
      const request = {
        query: this.currentSearchString,
        standardConceptFilter: standardConceptFilter,
        domain: conceptDomain.domain,
        includeDomainCounts: activeTabSearch,
        includeVocabularyCounts: true
      };
      this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
        .subscribe((response) => {
          numCalls += 1;
          numCallsSubject.next(conceptDomain.domain);
          conceptDomain.items = response.items;
          conceptDomain.vocabularyList = response.vocabularyCounts;
          if (activeTabSearch) {
            this.searchLoading = false;
            this.conceptDomainCounts = response.domainCounts;
            this.setConceptsAndVocabularies();
          }
      });
    });
  }

  setConceptsAndVocabularies() {
    const cacheItem = this.conceptsCache.find(
      conceptDomain => conceptDomain.domain === this.selectedDomain);
    this.concepts = cacheItem.items;
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
    this.blockMultipleSearchFromFilter = true;
    let standardConceptFilter: StandardConceptFilter;
    if (this.standardConceptsOnly) {
      standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
    } else {
      standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
    }
    this.searchLoading = true;
    const request = {
      query: this.currentSearchString,
      standardConceptFilter: standardConceptFilter,
      domain: this.selectedDomain,
      vocabularyIds: this.vocabularies
        .filter(vocabulary => vocabulary.selected).map(vocabulary => vocabulary.vocabularyId),
    };
    this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
      .subscribe((response) => {
        this.searchLoading = false;
        this.concepts = response.items;
      });
  }

  selectConcept(selectedConcepts) {
    this.selectedConcept = selectedConcepts;
    if (this.selectedConcept && this.selectedConcept.length > 0 ) {
      this.conceptSelected = true;
    }
  }
}
