import {select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions, searchRequestError} from 'app/cohort-search/redux';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';

import {Cohort} from 'generated/fetch';

const COHORT_TYPE = 'AoU_Discover';


@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: [
    '../../styles/buttons.css',
    '../../styles/errors.css',
    './overview.component.css'
  ]
})
export class OverviewComponent implements OnInit {
  @Input() chartData$: Observable<List<any>>;
  @Input() total$: Observable<number>;
  @Input() isRequesting$: Observable<boolean>;
  @Input() temporal: {flag, tempLength};
  @select(searchRequestError) error$: Observable<boolean>;

  cohortForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl()
  });

  error: boolean;
  saving = false;
  stackChart = false;
  showGenderChart = true;
  showComboChart = true;
  showConflictError = false;
  cohort: Cohort;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit(): void {
    if (currentCohortStore.getValue()) {
      this.cohort = currentCohortStore.getValue();
    }
  }

  get name() {
    return this.cohortForm.get('name');
  }

  get criteria() {
    return JSON.stringify(this.actions.mapAll());
  }

  get unchanged() {
    return this.criteria === this.cohort.criteria;
  }

  modalChange(value) {
    if (!value) {
      this.cohortForm.reset();
      this.showConflictError = false;
    }
  }

  saveCohort() {
    this.saving = true;
    const {ns, wsid} = urlParamsStore.getValue();
    this.cohort.criteria = JSON.stringify(this.actions.mapAll());
    const cid = this.cohort.id;
    cohortsApi().updateCohort(ns, wsid, cid, this.cohort).then(() => {
      this.saving = false;
      navigate(['workspaces', ns, wsid, 'cohorts', cid, 'actions']);
    }, (error) => {
      if (error.status === 400) {
        console.log(error);
        this.saving = false;
      }
    });
  }

  submit() {
    const {ns, wsid} = urlParamsStore.getValue();
    const name = this.cohortForm.get('name').value;
    const description = this.cohortForm.get('description').value;
    const cohort = <Cohort>{name, description, criteria: this.criteria, type: COHORT_TYPE};
    cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
      const queryParams = {cid: c.id};
      navigate(['workspaces', ns, wsid, 'cohorts', 'actions'], {queryParams});
    }, (error) => {
      if (error.status === 400) {
        this.showConflictError = true;
      }
    });
  }

  navigateTo(action: string) {
    const {ns, wsid} = urlParamsStore.getValue();
    let url = `/workspaces/${ns}/${wsid}/`;
    switch (action) {
      case 'cohort':
        url += 'cohorts';
        break;
      case 'notebook':
        url += 'notebooks';
        break;
      case 'review':
        url += `cohorts/${this.cohort.id}/review`;
        break;
    }
    navigateByUrl(url);
  }

  toggleChartMode() {
    this.stackChart = !this.stackChart;
  }

  toggleShowGender() {
    this.showGenderChart = !this.showGenderChart;
  }

  toggleShowCombo() {
    this.showComboChart = !this.showComboChart;
  }
}
