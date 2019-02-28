import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Concept} from '../../../publicGenerated/model/concept';
import {ConceptListResponse} from '../../../publicGenerated/model/conceptListResponse';
import {SearchConceptsRequest} from '../../../publicGenerated/model/searchConceptsRequest';
import {StandardConceptFilter} from '../../../publicGenerated/model/standardConceptFilter';
import {GraphType} from '../../utils/enum-defs';

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
  /* Show more synonyms when toggled */
  showMoreSynonyms = {};
  synonymString = {};
  ageChartHelpText = 'The age at occurrence bar chart provides a binned age \n' +
    'distribution for participants at the time the medical concept ' +
    'being queried occurred in their records. \n' +
    'If an individual’s record contains more than one mention of a concept, \n' +
    'the age at occurrence is included for each mention. \n' +
    'As a result, a participant may be counted more ' +
    'than once in the distribution. ';
  sourcesChartHelpText = 'Individual health records often contain medical ' +
    'information that means the same thing ' +
    'but may be recorded in many different ways. \n' +
    'The sources represent the many different ways that the standard medical concept ' +
    'returned in the search results has been recorded in patient records. \n' +
    'The sources bar chart provides the top 10 source concepts from the All of Us data.';
  matchingConceptsHelptText = 'Medical concepts are similar to medical terms; ' +
    'they capture medical information\n' +
    'in an individual’s records and may sometimes have values associated with them.\n' +
    'For example, “height” is a medical concept that has a measurement value (in centimeters).\n' +
    'These concepts are categorized into different domains. ' +
    'Domains are types of medical information.\n' +
    'The Data Browser searches the All of Us public data for medical concepts that\n' +
    'match the keyword or code entered in the search bar.\n' +
    'The Data Browser counts how many participants have at least\n' +
    'one mention of the matching medical concepts in their records.\n' +
    'Matching medical concepts that have the highest participant counts ' +
    'are returned at the top of the list.';

  /* Show different graphs depending on domain we are in */
  graphToShow = GraphType.BiologicalSex;
  showTopConcepts = false;
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
    'can be utilized to find medical concepts in the All of Us data set. ' +
    'Concept codes are specific to the\n' +
    'All of Us Research Program data and are assigned to all medical concepts.\n' +
    'In some instances,\n' +
    'a medical concept may not be assigned a source or standard vocabulary code.\n' +
    'In these instances, the concept code can be utilized to\n' +
    'query the data for the medical concept.';
  @ViewChild('chartElement') chartEl: ElementRef;


  constructor(private route: ActivatedRoute,
              private api: DataBrowserService
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

  private searchCallback(results: any) {
    this.searchResult = results;
    this.items = this.searchResult.items;
    for (const concept of this.items) {
      this.synonymString[concept.conceptId] = concept.conceptSynonyms.join(', ');
    }
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

  public selectGraph(g) {
    this.chartEl.nativeElement.scrollIntoView(
      { behavior: 'smooth', block: 'nearest', inline: 'start' });
    this.resetSelectedGraphs();
    this.graphToShow = g;
    if (this.ehrDomain.name === 'Measurements' && this.graphToShow === GraphType.BiologicalSex) {
      this.graphToShow = GraphType.MeasurementBins;
    }
  }

  public toggleSynonyms(conceptId) {
    this.showMoreSynonyms[conceptId] = !this.showMoreSynonyms[conceptId];
  }

  public showToolTip(g) {
    if (g === 'Biological Sex' || g === 'Gender Identity') {
      return 'Gender chart';
    } else if (g === 'Age at Occurrence') {
      return this.ageChartHelpText;
    } else if (g === 'Sources') {
      return this.sourcesChartHelpText;
    }
  }

  public resetSelectedGraphs() {
    this.graphToShow = GraphType.None;
  }

  public expandRow(concepts: any[], r: any) {
    if (r.expanded) {
      r.expanded = false;
      return;
    }
    this.resetSelectedGraphs();
    // In the case of measurements we show the histogram of
    // values in the place of normal gender graph.
    if (this.ehrDomain.name === 'Measurements') {
      this.graphToShow = GraphType.MeasurementBins;
    } else {
      this.graphToShow = GraphType.BiologicalSex;
    }
    concepts.forEach(concept => concept.expanded = false);
    r.expanded = true;
  }

  public toggleTopConcepts() {
    this.showTopConcepts = !this.showTopConcepts;
  }
}
