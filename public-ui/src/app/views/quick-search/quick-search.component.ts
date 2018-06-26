import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import { Observable } from 'rxjs/Rx';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
import {DbDomain} from "../../../publicGenerated/model/dbDomain";


@Component({
  selector: 'app-quick-search',
  templateUrl: './quick-search.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit, OnDestroy {
  pageImage = '/assets/db-images/man-standing.png';
  title = 'Quick Guided Search';
  subTitle = 'Enter keywords to search EHR data and survey modules. Or click on an EHR domain or survey.';
  searchResults = [];
  domainResults = [];
  surveyResults = [];
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  totalParticipants;
  domains = [];
  loading = true;
  private subscriptions: ISubscription[] = [];

  constructor(private api: DataBrowserService,
              private router: Router) {
      this.route.params.subscribe(params => {
        this.dataType = params.dataType;
      });
    }
  }

  ngOnInit() {

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
    // Get domain totalas only once so if they erase search we can load them
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
  }
  ngOnDestroy() {
    for (const s of this.subscriptions){
      s.unsubscribe();
    }
  }

  private searchCallback(results:DbDomainListResponse) {
    this.searchResults = results.items;
    this.domainResults = results.items.filter(d => d.dbType === 'domain_filter');
    this.surveyResults = results.items.filter(s => s.dbType === 'survey');
    this.loading = false;
  }
  public searchDomains(query: string) {
    // If query empty reset to already reatrieved domain totals
    if (query.length === 0) {
      const resultsObservable = new Observable((observer) => {
        const domains: DbDomainListResponse = {items: this.domains};
        observer.next(domains);
        observer.complete();
      });
      return resultsObservable;
    }
    this.prevSearchText = query;
    localStorage.setItem('searchText', query);
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
