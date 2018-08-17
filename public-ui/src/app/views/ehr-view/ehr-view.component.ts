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

@Component({
  selector: 'app-ehr-view',
  templateUrl: './ehr-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './ehr-view.component.css']
})
export class EhrViewComponent implements OnInit, OnDestroy {
  domainId: string;
  title: string ;
  subTitle: string;
  dbDomain: any;
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

  /* Show different graphs depending on domain we are in */
  // defaults,  most domains
  showAge = true;
  showGender = true;
  showSources = true;
  showMeasurementGenderBins = false;


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
    this.subscriptions.push(this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue));
    this.items = [];

    // Get search text from localStorage
    this.prevSearchText = localStorage.getItem('searchText');
    if (!this.prevSearchText) {
      this.prevSearchText = '';
    }
    this.searchText.setValue(this.prevSearchText);
    const obj = localStorage.getItem('dbDomain');
    if (obj) {
      this.dbDomain = JSON.parse(obj);
      this.subTitle = 'Keyword: ' + this.searchText;
      this.title = 'Domain Search Results: ' + this.dbDomain.domainDisplay;
    } else {
      /* Error. We need a db Domain object. */
      this.title   = 'Keyword: ' + this.searchText;
      this.title = 'Domain Search Results: ' + 'Error - no result for domain selected';
    }

    if (this.dbDomain) {
      // Set the graphs we want to show for this domain
      this.setGraphsToDisplay();
      // Run search initially to filter to domain,
      // a empty search returns top ordered by count_value desc
      this.subscriptions.push(this.searchDomain(this.prevSearchText).subscribe(results =>
        this.searchCallback(results)));

      // Add value changed event to search when value changes
      this.subscriptions.push(this.searchText.valueChanges
        .debounceTime(200)
        .distinctUntilChanged()
        .switchMap((query) => this.searchDomain(query))
        .subscribe(results => this.searchCallback(results)));

      // Set to loading as long as they are typing
      this.subscriptions.push(this.searchText.valueChanges.subscribe(
        (query) => this.loading = true ));
    }
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  private setGraphsToDisplay() {
    if (this.dbDomain.domainId === 'Measurement') {
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
    const maxResults = 100;
    this.loading = true;

    this.searchRequest = {
        query: query,
        domain: this.dbDomain.domainId.toUpperCase(),
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
    }
  }

}
