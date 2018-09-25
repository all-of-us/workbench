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
import {DomainInfosAndSurveyModulesResponse} from '../../../publicGenerated/model/domainInfosAndSurveyModulesResponse';


@Component({
  selector: 'app-quick-search',
  templateUrl: './quick-search.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit, OnDestroy {
  pageImage = '/assets/db-images/man-standing.png';
  title = 'Quick Guided Search';
  subTitle = 'Enter keywords to search EHR data and survey modules. ' +
    'Or click on an EHR domain or survey.';
  domainResults = [];
  surveyResults = [];
  searchText: FormControl = new FormControl();
  prevSearchText = '';
  totalParticipants;
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
    }
    if (this.dataType === this.SURVEY_DATATYPE) {
      this.title = 'Participant Survey Data';
    }
    // Get search result from localStorage
    this.prevSearchText = localStorage.getItem('searchText');
    if (!this.prevSearchText) {
      this.prevSearchText = '';
    }
    this.searchText.setValue(this.prevSearchText);

    this.subscriptions.push(
      this.api.getParticipantCount().subscribe(
          result => this.totalParticipants = result.countValue)
    );

    // Do initial search if we have search text
    if (this.prevSearchText) {
      this.subscriptions.push(
        this.searchDomains(this.prevSearchText).subscribe(
            (data: DomainInfosAndSurveyModulesResponse) => {
          return this.searchCallback(data);
        }));
    }
    // Get domain totals only once so if they erase search we can load them
    this.subscriptions.push(
      this.api.getDomainTotals().subscribe(
          (data: DomainInfosAndSurveyModulesResponse) => {
        this.domainResults = data.domainInfos;
        this.surveyResults = data.surveyModules;
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
        .subscribe((data: DomainInfosAndSurveyModulesResponse) => {
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

  private searchCallback(results: DomainInfosAndSurveyModulesResponse) {
    this.domainResults = results.domainInfos;
    this.surveyResults = results.surveyModules;
    this.loading = false;
  }
  public searchDomains(query: string) {
    this.prevSearchText = query;
    localStorage.setItem('searchText', query);
    // If query empty reset to already retrieved domain totals
    if (query.length === 0) {
      const resultsObservable = new Observable((observer) => {
        const domains: DomainInfosAndSurveyModulesResponse = {
            domainInfos: this.domainResults,
            surveyModules: this.surveyResults
        };
        observer.next(domains);
        observer.complete();
      });
      return resultsObservable;
    }

    return this.api.getDomainSearchResults(query);
  }

  public viewSurvey(r) {
    localStorage.setItem('surveyModule', JSON.stringify(r));
    localStorage.setItem('searchText', this.prevSearchText);
    this.router.navigateByUrl('/survey/' + r.conceptId);
  }

  public viewEhrDomain(r) {
    localStorage.setItem('ehrDomain', JSON.stringify(r));
    localStorage.setItem('searchText', this.prevSearchText);
    this.router.navigateByUrl('/ehr/' + r.domain.toLowerCase());
  }

}
