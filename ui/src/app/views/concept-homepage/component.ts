import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';


import {
  Concept,
  ConceptsService,
  Domain,
  DomainInfo,
  StandardConceptFilter,
} from 'generated';

interface ConceptCacheSet {
  domain: Domain;

  items: Array<Concept>;
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

  @ViewChild(ConceptTableComponent)
  conceptTable: ConceptTableComponent;

  @ViewChild(ConceptAddModalComponent)
  conceptAddModal: ConceptAddModalComponent;

  conceptDomainList: Array<DomainInfo> = [];

  concepts: Array<Concept> = [];
  conceptsCache: Array<ConceptCacheSet> = [];

  wsNamespace: string;
  wsId: string;

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
      console.log(this.conceptDomainList);
      this.conceptDomainList.forEach((domain) => {
        this.conceptsCache.push({
          domain: domain.domain,
          items: []
        });
      });
      this.loadingDomains = false;
      this.selectedDomain = this.conceptDomainList[0].domain;
    });
  }

  openAddModal(): void {
    this.conceptAddModal.open();
  }

  selectDomain(domain: Domain) {
    this.selectedDomain = domain;
    this.setConcepts();
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
    console.log(this.standardConceptsOnly);
    this.searching = true;
    this.searchLoading = true;
    let standardConceptFilter: StandardConceptFilter;
    if (this.standardConceptsOnly) {
      standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
    } else {
      standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
    }

    let numCalls = 0;
    const numCallsSubject = new BehaviorSubject<number>(0);
    const numCallsSubject$ = numCallsSubject.asObservable();

    numCallsSubject$.subscribe((finishedCalls) => {
      if (finishedCalls === this.conceptsCache.length) {
        this.searchLoading = false;
        this.setConcepts();
      }
    });

    this.conceptsCache.forEach((conceptDomain) => {
      const request = {
        query: this.currentSearchString,
        standardConceptFilter: standardConceptFilter,
        domain: conceptDomain.domain
      };
      this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
        .subscribe((response) => {
          numCalls += 1;
          numCallsSubject.next(numCalls);
          conceptDomain.items = response.items;
      });
    });
  }

  setConcepts() {
    this.concepts = this.conceptsCache.find(
      conceptDomain => conceptDomain.domain === this.selectedDomain).items;
  }

  get getSearchDisabled() {
    return this.searchLoading || this.loadingDomains || this.searchTermNotLongEnough;
  }

  get searchTermNotLongEnough() {
    return this.searchTerm === undefined || this.searchTerm.length < 3;
  }
}
