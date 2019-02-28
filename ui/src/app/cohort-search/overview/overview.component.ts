import {select} from '@angular-redux/store';
import {Component, Input} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions, searchRequestError} from 'app/cohort-search/redux';
import {navigate, urlParamsStore} from 'app/utils/navigation';

import {Cohort, CohortsService} from 'generated';

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
export class OverviewComponent {
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
  stackChart = false;
  showGenderChart = true;
  showComboChart = true;
  showConflictError = false;

  constructor(
    private actions: CohortSearchActions,
    private cohortApi: CohortsService,
  ) {}

  get name() {
    return this.cohortForm.get('name');
  }

  modalChange(value) {
    if (!value) {
      this.cohortForm.reset();
      this.showConflictError = false;
    }
  }

  submit() {
    const {ns, wsid} = urlParamsStore.getValue();

    const name = this.cohortForm.get('name').value;
    const description = this.cohortForm.get('description').value;
    const criteria = JSON.stringify(this.actions.mapAll());
    const cohort = <Cohort>{name, description, criteria, type: COHORT_TYPE};
    this.cohortApi.createCohort(ns, wsid, cohort).subscribe((_) => {
      navigate(['workspaces', ns, wsid, 'cohorts']);
    }, (error) => {
      if (error.status === 400) {
        this.showConflictError = true;
      }
    });
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
