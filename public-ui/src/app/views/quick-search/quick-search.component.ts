import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';

import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import { Observable } from 'rxjs/Rx';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';


@Component({
  selector: 'app-quick-search',
  templateUrl: './quick-search.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit, OnDestroy {
  pageImage = '/assets/db-images/man-standing.png';
  title = 'Quick Guided Search';
  subTitle = 'Enter a keyword or data standards code (eg ICD, SNOMED) in the search bar to search across Electronic Health Record (EHR) data and program surveys.';
  searchResults = [];
  domainResults = [];
  surveyResults = [];
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  totalParticipants;
  domains = [];
  loading = true;
  dataType = null;
  EHR_DATATYPE = 'ehr';
  SURVEY_DATATYPE = 'surveys';

  private subscriptions: ISubscription[] = [];

  constructor(private api: DataBrowserService,
              private route: ActivatedRoute,
              private router: Router) {
    this.route.params.subscribe(params => {
          this.dataType = params.dataType;
    });
  }

  ngOnInit() {

    // Set title based on datatype
    if (this.dataType === this.EHR_DATATYPE) {
      this.title = 'Electronic Health Data';
      this.subTitle = 'Enter a keyword or data standards code (eg ICD, SNOMED) in the search bar to search across Electronic Health Record (EHR) data.';
    }
    if (this.dataType === this.SURVEY_DATATYPE) {
      this.title = 'Participant Survey Data';
      this.subTitle = 'Enter a keyword to search survey data. Or click on a survey below to view full content.';
    }
    // Get search result from localStorage
    this.prevSearchText = localStorage.getItem('searchText');
    if (!this.prevSearchText) {
      this.prevSearchText = '';
    }
    this.searchText.setValue(this.prevSearchText);

    this.subscriptions.push(
      this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue)
    );

    // Do initial search if we have search text
    if (this.prevSearchText) {
      this.subscriptions.push(
        this.searchDomains(this.prevSearchText).subscribe((data: DbDomainListResponse) => {
          return this.searchCallback(data);
        }));
    }
    // Get domain totals only once so if they erase search we can load them
    this.subscriptions.push(
      this.api.getDomainTotals().subscribe((data: DbDomainListResponse) => {
        this.domains = data.items;
        // Only set results to the totals if we don't have a searchText
        if (!this.prevSearchText) {
          this.searchCallback(data);
        }
        this.loading = false;
      })
    );

    // Search when text value changes
    this.subscriptions.push(
      this.searchText.valueChanges
        .debounceTime(300)
        .distinctUntilChanged()
        .switchMap((query) => this.searchDomains(query))
        .subscribe((data: DbDomainListResponse) => {
          this.searchCallback(data);
        })
    );

    // Set to loading as long as they are typing
    this.subscriptions.push(this.searchText.valueChanges.subscribe(
      (query) => this.loading = true ));
  }
  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  public showDataType(showType) {
    return !this.loading && (!this.dataType || this.dataType === showType);
  }

  private searchCallback(results: DbDomainListResponse) {
    this.searchResults = results.items;
    this.domainResults = results.items.filter(d => d.dbType === 'domain_filter');
    this.surveyResults = results.items.filter(s => s.dbType === 'survey');
    this.loading = false;
  }
  public searchDomains(query: string) {
    this.prevSearchText = query;
    localStorage.setItem('searchText', query);
    // If query empty reset to already reatrieved domain totals
    if (query.length === 0) {
      const resultsObservable = new Observable((observer) => {
        const domains: DbDomainListResponse = {items: this.domains};
        observer.next(domains);
        observer.complete();
      });
      return resultsObservable;
    }

    return this.api.getDomainSearchResults(query);
  }

  public viewResults(r) {
    localStorage.setItem('dbDomain', JSON.stringify(r));
    localStorage.setItem('searchText', this.prevSearchText);

    if (r.dbType === 'survey') {
      this.router.navigateByUrl('/survey/' + r.domainId.toLowerCase());
    } else {
      this.router.navigateByUrl('/ehr/' + r.domainId.toLowerCase());
    }
  }

}
