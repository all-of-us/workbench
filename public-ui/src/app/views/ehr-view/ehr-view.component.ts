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
  title ;
  subTitle;
  dbDomain;
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  searchResult: ConceptListResponse;
  items: any[] = [];
  standardConcepts: any[] = [];
  loading = true;
  minParticipantCount = 0;
  totalParticipants;
  top10Results = [];
  screenWidth: any;

  private searchRequest: SearchConceptsRequest;
  private subscriptions: ISubscription[] = [];

  constructor(private route: ActivatedRoute,
              private api: DataBrowserService
              //public responsiveSizeInfoRx: ResponsiveSizeInfoRx,
              //public userAgentInfoRx: UserAgentInfoRx
  ) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    this.screenWidth = window.innerWidth;
    console.log('Screen width', this.screenWidth);

    // Connect responsize listeners
    /* In progress Testing
     this.subscriptions.push(
      this.responsiveSizeInfoRx.getResponsiveSize.subscribe((data) => {
        console.log('this.responsiveSizeInfoRx.getResponsiveSize ===>', data);
      }, (err) => {
        console.log('Error', err);
      })
    );
    this.subscriptions.push(
      this.userAgentInfoRx.getUserAgent.subscribe((data) => {
        console.log('this.userAgentInfoRx.getUserAgent ===>', data);
      }, (err) => {
        console.log('Error', err);
      })
    );
    this.responsiveSizeInfoRx.connect();
    this.userAgentInfoRx.connect();
    */

    this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);
    this.items = [];

    // Get search result from localStorage
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
      this.title = 'Domain Search Results: ' + 'Error - no result domain selected';
    }

    if (this.dbDomain) {
      // Run search initially filter to domain, a empty search returns top ordered by count_value desc
      this.subscriptions.push(this.searchDomain(this.prevSearchText).subscribe(results =>
        this.searchCallback(results)));

      // Add value changed event to search when value changes
      this.subscriptions.push(this.searchText.valueChanges
        .debounceTime(200)
        .distinctUntilChanged()
        .switchMap((query) => this.searchDomain(query))
        .subscribe(results => this.searchCallback(results)));
    }
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
    /*this.responsiveSizeInfoRx.disconnect();
    this.userAgentInfoRx.disconnect();*/
  }

  private searchCallback(results: any) {
    this.searchResult = results;
    this.items = this.searchResult.items;
    if (this.searchResult.standardConcepts) {
      this.standardConcepts = this.searchResult.standardConcepts;
    }
    this.top10Results = this.searchResult.items.slice(0, 10);
    // Set the localStorage to empty so making a new search here does not follow them if they hit back button
    localStorage.setItem('searchText', '');
    this.loading = false;
  }
  private searchDomain(query: string) {
    let maxResults = 100;
    this.loading = true;
    if (query.length) {
      this.searchRequest = {
          query: query,
          domain: this.dbDomain.domainId.toUpperCase(),
          standardConceptFilter: StandardConceptFilter.STANDARDORCODEIDMATCH,
          maxResults: null,
          minCount: 1
      };
      this.prevSearchText = query;
      return this.api.searchConcepts(this.searchRequest);
    }

      this.searchRequest = {
          query: query,
          domain: this.dbDomain.domainId.toUpperCase(),
          standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS,
          maxResults: 100,
          minCount: 1
      };
    this.prevSearchText = '';
    return this.api.searchConceptsEmptyQuery(this.searchRequest);

  }



  public toggleSources(row) {
    console.log('search result ' , this.searchResult);
    if (row.showSources) {
      row.showSources = false;
    } else {
      row.showSources = true;
      row.expanded = true;
    }
  }

}
