import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import { Observable } from "rxjs/Rx";
import {DbDomainListResponse} from "../../../publicGenerated/model/dbDomainListResponse";


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
  private subscriptions: ISubscription;
  private subscription2: ISubscription;
  private subscription3: ISubscription;


  constructor(private api: DataBrowserService,
              private router: Router) {
  }

  ngOnInit() {

    this.subscriptions = this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);

    // Initialize results to all totals
    this.subscription2 = this.api.getDomainTotals().subscribe((data: DbDomainListResponse) => {
      this.loading = true;
      this.domains = data.items;
      this.searchResults = this.domains;
      this.domainResults = this.domains.filter(d => d.dbType === 'domain_filter');
      this.surveyResults = this.domains.filter(d => d.dbType === 'survey');
      this.loading = false;
    });


    this.subscription3 = this.searchText.valueChanges
      .debounceTime(200)
      .distinctUntilChanged()
      .switchMap((query) => this.searchDomains(query))
      .subscribe((data: DbDomainListResponse) => {
        this.searchResults = data.items;
        this.domainResults = data.items.filter(d => d.dbType === 'domain_filter');
        this.surveyResults = data.items.filter(s => s.dbType === 'survey');
        this.loading = false;
      });
  }
  ngOnDestroy() {
    console.log('unsubscribing ');
    this.subscriptions.unsubscribe();
    this.subscription2.unsubscribe();
    this.subscription3.unsubscribe();
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
    console.log("Searching domains ", query)
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
