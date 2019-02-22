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
import {ConceptGroup} from '../../utils/conceptGroup';
import {DbConfigService} from '../../utils/db-config.service';


@Component({
    selector: 'app-quick-search',
    templateUrl: './quick-search.component.html',
    styleUrls: ['../../styles/template.css', '../../styles/cards.css',
        './quick-search.component.css']
})
export class QuickSearchComponent implements OnInit, OnDestroy {
    dbDesc = `The Data Browser provides interactive views of the publically available
     All of Us (AoU) Research Program participant data. Currently, participant provided
      information, including surveys
    and physical measurements taken at the time of participant enrollment
    (“program physical measurements”), as well as electronic health record (EHR) data are
     available. The AoU Research Program data resource will grow to include
      more data types over time.`;
    title = 'Search Across Data Types';
    subTitle = 'Conduct a search across all All of Us Research Program data types, ' +
      'including surveys, ' +
      'physical measurements taken at the time of participant enrollment ' +
      ' (“program physical measurements”), ' +
      'and electronic health record (EHR) data. Search using common keywords and/or ' +
      'billing or data standards codes (ie SNOMED, CPT, ICD). ';
    searchResults = [];
    domainResults = [];
    surveyResults = [];
    totalResults: DomainInfosAndSurveyModulesResponse;
    searchText: FormControl = new FormControl();
    prevSearchText = '';
    totalParticipants;
    loading = true;
    dataType = null;
    EHR_DATATYPE = 'ehr';
    SURVEY_DATATYPE = 'surveys';
    PROGRAM_PHYSICAL_MEASUREMENTS = 'program_physical_measurements';
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
    pmConceptGroups: ConceptGroup[];

    private subscriptions: ISubscription[] = [];

    constructor(private api: DataBrowserService,
                private route: ActivatedRoute,
                private router: Router,
                public dbc: DbConfigService) {
      this.route.params.subscribe(params => {
        this.dataType = params.dataType;
      });
    }

    ngOnInit() {
      this.dbc.getPmGroups().subscribe(results => {
        this.pmConceptGroups = results;
      });
        // Set title based on datatype
      if (this.dataType === this.EHR_DATATYPE) {
        this.title = 'Electronic Health Data';
        this.subTitle = 'Enter a keyword or data standards code (eg ICD, SNOMED)' +
            ' in the search bar ' +
            'to search across Electronic Health Record (EHR) data.';
      }
      if (this.dataType === this.SURVEY_DATATYPE) {
        this.title = 'Browse Participant Surveys';
        this.subTitle = 'Participants are asked to complete ' +
            'The Basics survey at the time of enrollment,' +
            ' and may choose to complete additional surveys as they become available. ' +
            'Use this tool to browse all survey questions as well as all ' +
            'response options for each question. ' +
            'The Data Browser provides a total count, ' +
            'grouped by age at occurrence and gender, for each response option.';
      }
      if (this.dataType === this.PROGRAM_PHYSICAL_MEASUREMENTS) {
        this.title = 'Program Physical Measurements';
        this.subTitle = 'Participants have the option to provide a standard set of ' +
          'physical measurements as part\n' +
          'of the enrollment process  (“program physical measurements”).\n' +
          'Use this tool to browse distributions of measurement ' +
          'values and counts,\n' +
          'grouped by age at occurrence and gender, for each ' +
          'program physical measurement.';
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
                this.totalResults = data;
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
            domainInfos: this.totalResults.domainInfos,
            surveyModules: this.totalResults.surveyModules
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

  public matchPhysicalMeasurements(searchString: string) {
    if (!this.pmConceptGroups) {
      return 0;
    }
    if (!searchString) {
      return this.pmConceptGroups.length;
    }
    return this.pmConceptGroups.filter(conceptgroup =>
      conceptgroup.groupName.toLowerCase().includes(searchString.toLowerCase())).length;
  }
}
