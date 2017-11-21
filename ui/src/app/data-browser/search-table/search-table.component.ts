import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { AchillesService } from '../services/achilles.service'
import { NgForm, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';

// import {Search} from '../SearchClass'

@Component({
  selector: 'app-search-table',
  templateUrl: './search-table.component.html',
  styleUrls: ['./search-table.component.css']
})

export class SearchTableComponent implements OnInit {
  sourceConceptFilters = [];
  standardConceptFilters = [{ vocabulary_id: 'SNOMED' }, { vocabulary_id: 'RxNorm' }, { vocabulary_id: 'LOINC' }, { vocabulary_id: 'CPT4' }, { vocabulary_id: 'Visit' }];
  filterValueAr = []
  source_vocabs_model = {}
  standard_vocabs_model = {} // model of standard for form
  conceptResults;
  conceptCard;
  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses
  showCard = false;
  conceptArr
  analyses = []
  topTenAnalysis
  conceptsArray = []
  resetAnalyses;
  routeId: string;
  colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4']
  counter = 0;
  pager = { page_len: 10, cur_page: 1 };
  loading: boolean = true;
  totalItems: number;
  searchParams: any = null;
  showAdvanced:boolean
  retainedString
  @Output() onItemSelected: EventEmitter<any>;
  @Output() emittSearchString: EventEmitter<any>;
  @Input() savedSearchString
  @Input() itemFromHeader
  myForm: FormGroup;

  // Route domain map -- todo , these can be more than one eventually
  routeDomain = {
    'conditions': 'Condition',
    'procedure': 'Procedure',
    'drug': 'Drug',
    'all': 'all',
    'icd9cm':'ICD9CM',
    'icd9':'ICD9',
    'icd10':'ICD10',
    'icd10cm':'ICD10CM',
    'cpt4':'cpt4',
    'snomed':'snomed',
  }
  pageDomainId = null;


  constructor(private achillesService: AchillesService, private route: ActivatedRoute, fb: FormBuilder) {
    // instantiate our event emitter Output
    this.onItemSelected = new EventEmitter();
    this.emittSearchString = new EventEmitter();
    // Build the form
    let standard_vocabs_fg = {}
    for (let v of this.standardConceptFilters) {
      standard_vocabs_fg[v.vocabulary_id] = '';
      this.standard_vocabs_model[v.vocabulary_id] = { checked: false };
    }
    this.myForm = fb.group({
      search: [''],
      standard_concept: ['s'], // Default to searching only standard concepts
      observed: 'true',
      standard_vocabs: fb.group(standard_vocabs_fg)
    })

    this.route.params.subscribe(params => {
      //get parameter from URL...  example:  http://localhost:4200/data-browser/drug  <--drug is params.id, which is defined in router.\
      // Set the domain id for the page if this route is dombain specific
      this.routeId = params.id;
      if (this.routeDomain[this.routeId]) {
        this.pageDomainId = this.routeDomain[this.routeId];
      }

      //
    })

    this.achillesService.getSearchVocabFilters()
      .subscribe(results => {
        this.sourceConceptFilters = results;
      })

  }
  ngOnChanges(){
    this.myForm.value.concept_name = this.savedSearchString
    this.searchData(this.myForm.value)

  }

  // Pager clicked on grid, sends state object
  refresh(state: any) {
    // Note , this calls search on page load . So if it is first time, initialize search params with form defaults
    if (!this.searchParams) {
      this.searchParams = this.myForm.value;
    }
    this.loading = true;
    // We convert the filters from an array to a map,
    // because that's what our backend-calling service is expecting
    // May want to have filters
    /*let filters:{[prop:string]: any[]} = {};
    if (state.filters) {
        for (let filter of state.filters) {
            let {property, value} = <{property: string, value: string}>filter;
            filters[property] = [value];
        }
    }*/

    this.searchParams.page_len = state.page.size;
    this.searchParams.page_from = state.page.from;
    this.searchParams.page_to = state.page.to;
    let curPage = 1;
    if (state.page.from > 0) {
      curPage = state.page.from / state.page.size + 1;
    }
    this.searchParams.page = curPage;
    this.searchData(this.searchParams);

  }

  ngOnInit() {
    this.retainedString = this.savedSearchString
  }

  itemClick(concept: any) {
    this.onItemSelected.emit(concept);
  }
  sendString(string) {
    this.emittSearchString.emit(string);

  }

  searchData(params) {
    this.searchParams = params;
    if (!this.searchParams.search) {
      this.searchParams.search = this.savedSearchString

    }
    // Set up params for search

    // Concept_name goes in search for now. may expand search outside of just concept name
    this.searchParams.concept_name = this.searchParams.search;

    // Set domain id for search if we have one for this page
    if (this.pageDomainId) {
      this.searchParams.domain_id = this.pageDomainId;
    }
    //hardcode the standard concept fo the simple search-table
    this.searchParams.standard_concept = "s"
    this.achillesService.getConceptResults(this.searchParams)
      .subscribe(results => {
        this.conceptResults = results.data;
        this.totalItems = results.totalItems;
        //
        this.loading = false;
      });

  }

}
