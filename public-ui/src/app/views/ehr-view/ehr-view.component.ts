import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {SearchConceptsRequest} from '../../../publicGenerated/model/searchConceptsRequest';
import {StandardConceptFilter} from '../../../publicGenerated/model/standardConceptFilter';

import { FormControl } from '@angular/forms';
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
  searchResults = [];
  loading = true;
  minParticipantCount = 0;
  totalParticipants;
  private searchRequest: SearchConceptsRequest;
  private subscription: ISubscription;
  private subscription2: ISubscription;


  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);

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
      this.title = 'View Full Results: ' + this.dbDomain.domainDisplay;
    } else {
      /* Error. We need a db Domain object. */
      this.title   = 'Keyword: ' + this.searchText;
      this.title = 'View Full Results: ' + 'Error - no result domain selected';
    }

    if (this.dbDomain) {

      // Run search initially filter to domain, a empty search returns top ordered by count_value desc
      this.subscription = this.searchDomain(this.prevSearchText).subscribe(results => {
        this.searchResults = results.items;
        // Set our min partipant count
        if (this.searchResults.length > 0) {
          this.minParticipantCount = this.searchResults[0].countValue;
        }
        this.loading = false;
      });

      this.subscription2 = this.searchText.valueChanges
        .debounceTime(200)
        .distinctUntilChanged()
        .switchMap((query) => this.searchDomain(query))
        .subscribe(results => {
          this.searchResults = results.items;
          // Set our min partipant count
          if (this.searchResults.length > 0) {
            this.minParticipantCount = this.searchResults[0].countValue;
          }
          this.loading = false;
        });



    }
  }

  ngOnDestroy() {
    console.log("unsubscribing ehr view");
    this.subscription.unsubscribe();
    this.subscription2.unsubscribe();
  }

  private searchDomain(query: string) {
    this.searchRequest = {query: query, domain: this.dbDomain.domainId, standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS}; // new SearchConceptsRequest();
    this.prevSearchText = query;
    //return this.api.getConceptsSearch(query, 'S', this.dbDomain.domainId);
    // This advanced gives error . Srushti todo uncomment and see what it is.
    console.log(this.searchRequest , "Search request ");
     return this.api.getAdvancedConceptSearch(this.searchRequest);
  }

  /*
  searchDomain() {
    if (!this.searchText || this.searchText.length === 0) {
      this.searchText = null;
      console.log('null search text');
    }

    this.api.getConceptsSearch(this.searchText, 'S', this.dbDomain.domainId).subscribe(results =>  {
      this.searchResults = results.items;
      // Set our min partipant count
      if (this.searchResults.length > 0 ) {
        this.minParticipantCount = this.searchResults[0].countValue;
      }
      this.loading = false;
    } );
  }*/

}
