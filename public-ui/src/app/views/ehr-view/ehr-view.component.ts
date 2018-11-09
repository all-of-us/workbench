import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {ConceptListResponse} from '../../../publicGenerated/model/conceptListResponse';
import {SearchConceptsRequest} from '../../../publicGenerated/model/searchConceptsRequest';
import {StandardConceptFilter} from '../../../publicGenerated/model/standardConceptFilter';

import { FormControl } from '@angular/forms';
import {
  BrowserInfoRx,
  ResponsiveSizeInfoRx, UserAgentInfoRx
} from 'ngx-responsive';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import { ISubscription } from 'rxjs/Subscription';

/* This displays concept search for a Domain. */

@Component({
  selector: 'app-ehr-view',
  templateUrl: './ehr-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './ehr-view.component.css']
})
export class EhrViewComponent implements OnInit, OnDestroy {
  domainId: string;
  title: string ;
  subTitle: string;
  ehrDomain: any;
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  searchResult: ConceptListResponse;
  items: any[] = [];
  standardConcepts: any[] = [];
  loading = true;
  totalParticipants: number;
  top10Results: any[] = []; // We graph top10 results
  private searchRequest: SearchConceptsRequest;
  private subscriptions: ISubscription[] = [];
  private initSearchSubscription: ISubscription = null;

  /* Show different graphs depending on domain we are in */
  // defaults,  most domains
  showAge = true;
  showGender = true;
  showGenderIdentity = false;
  showSources = true;
  showMeasurementGenderBins = false;
  domainHelpText = {'condition': 'Medical concepts that describe the ' +
    'health status of an individual, ' +
    'such as medical diagnoses, are found in the conditions domain.',
    'drug': 'Medical concepts that capture information about the utilization of a ' +
    'drug when ingested or otherwise introduced into ' +
    'the body are captured by the drug exposures domain.',
    'measurement': 'Medical concepts that capture values resulting from ' +
    'examinations or tests are captured by the measurements domain. ' +
    'The measurements domain may include vital signs, lab values, ' +
    'quantitative findings from pathology reports, etc.',
    'procedure': 'Medical concepts that capture information related to activities or ' +
    'processes that are ordered or carried out on individuals for ' +
    'diagnostic or therapeutic purposes are captured by the procedures domain.'};
  conceptCodeHelpText = 'The concept code is an additional piece of information that\n' +
    'can be utilized to find medical concepts in the AoU data set. ' +
    'Concept codes are specific to the\n' +
    'AoU Research Program data and are assigned to all medical concepts.\n' +
    'In some instances,\n' +
    'a medical concept may not be assigned a source or standard vocabulary code.\n' +
    'In these instances, the concept code can be utilized to\n' +
    'query the data for the medical concept.';


  constructor(private route: ActivatedRoute,
              private api: DataBrowserService
              // public responsiveSizeInfoRx: ResponsiveSizeInfoRx,
              // public userAgentInfoRx: UserAgentInfoRx
  ) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    // Get total participants
    this.subscriptions.push(
      this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue)
    );

    this.items = [];

    // Get search text from localStorage
    this.prevSearchText = localStorage.getItem('searchText');
    if (!this.prevSearchText) {
      this.prevSearchText = '';
    }
    this.searchText.setValue(this.prevSearchText);
    const obj = localStorage.getItem('ehrDomain');
    if (obj) {
      this.ehrDomain = JSON.parse(obj);
      this.subTitle = 'Keyword: ' + this.searchText;
      this.title = 'Domain Search Results: ' + this.ehrDomain.name;
    } else {
      /* Error. We need a db Domain object. */
      this.title   = 'Keyword: ' + this.searchText;
      this.title = 'Domain Search Results: ' + 'Error - no result for domain selected';
    }

    if (this.ehrDomain) {
      // Set the graphs we want to show for this domain
      this.setGraphsToDisplay();
      // Run search initially to filter to domain,
      // a empty search returns top ordered by count_value desc
      // Note, we save this in its own subscription so we can unsubscribe when they start typing
      // and these results don't trump the search results in case they come back slower
      this.initSearchSubscription = this.searchDomain(this.prevSearchText).subscribe(results =>
        this.searchCallback(results));

      // Add value changed event to search when value changes
      this.subscriptions.push(this.searchText.valueChanges
        .debounceTime(200)
        .distinctUntilChanged()
        .switchMap((query) => this.searchDomain(query))
        .subscribe({
          next: results => this.searchCallback(results),
          error: err => {
            console.log('Error searching: ', err);
            this.loading = false;
          }}));

      // Set to loading as long as they are typing
      this.subscriptions.push(this.searchText.valueChanges.subscribe(
        (query) => this.loading = true ));
    }
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
    this.initSearchSubscription.unsubscribe();
  }

  private setGraphsToDisplay() {
    if (this.ehrDomain.name === 'Measurements') {
      this.showGender = false;
      this.showMeasurementGenderBins = true;
    }
  }
  private searchCallback(results: any) {
    this.searchResult = results;
    this.items = this.searchResult.items;
    if (this.searchResult.standardConcepts) {
      this.standardConcepts = this.searchResult.standardConcepts;
    }
    this.top10Results = this.searchResult.items.slice(0, 10);
    // Set the localStorage to empty so making a new search here does not follow to other pages
    localStorage.setItem('searchText', '');
    this.loading = false;
  }
  private searchDomain(query: string) {
    // Unsubscribe from our initial search subscription if this is called again
    if (this.initSearchSubscription) {
      this.initSearchSubscription.unsubscribe();
    }
    const maxResults = 100;
    this.loading = true;

    this.searchRequest = {
      query: query,
      domain: this.ehrDomain.domain.toUpperCase(),
      standardConceptFilter: StandardConceptFilter.STANDARDORCODEIDMATCH,
      maxResults: maxResults,
      minCount: 1
    };
    this.prevSearchText = query;
    return this.api.searchConcepts(this.searchRequest);

  }

  public toggleSources(row) {
    if (row.showSources) {
      row.showSources = false;
    } else {
      row.showSources = true;
      row.expanded = true;
      row.viewSynonyms = true;
    }
  }

  public selectGenderGraph(g) {
    if (g === 'Gender Identity') {
      this.showGenderIdentity = true;
      this.showGender = false;
    } else {
      this.showGender = true;
      this.showGenderIdentity = false;
    }
  }

}
