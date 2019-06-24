import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';

import {Cohort} from 'generated/fetch';

const COHORT_TYPE = 'AoU_Discover';


@Component({
  selector: 'app-list-overview',
  templateUrl: './list-overview.component.html',
  styleUrls: [
    '../../styles/buttons.css',
    '../../styles/errors.css',
    './list-overview.component.css'
  ]
})
export class ListOverviewComponent implements OnInit {
  @Input() chartData: Array<any>;
  @Input() total: number;
  @Input() isRequesting: boolean;
  @Input() temporal: {flag, tempLength};
  @Input() error: boolean;

  cohortForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl()
  });

  saving = false;
  deleting = false;
  stackChart = false;
  showGenderChart = true;
  showComboChart = true;
  showConflictError = false;
  cohort: Cohort;

  ngOnInit(): void {
    if (currentCohortStore.getValue()) {
      this.cohort = currentCohortStore.getValue();
    }
  }

  get name() {
    return this.cohortForm.get('name');
  }

  get criteria() {
    return JSON.stringify(searchRequestStore.getValue());
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
    this.cohort.criteria = this.criteria;
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
    this.saving = true;
    const {ns, wsid} = urlParamsStore.getValue();
    const name = this.cohortForm.get('name').value;
    const description = this.cohortForm.get('description').value;
    const cohort = <Cohort>{name, description, criteria: this.criteria, type: COHORT_TYPE};
    cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
      navigate(['workspaces', ns, wsid, 'cohorts', c.id, 'actions']);
    }, (error) => {
      this.saving = false;
      if (error.status === 400) {
        this.showConflictError = true;
      }
    });
  }

  delete = () => {
    const {ns, wsid} = urlParamsStore.getValue();
    cohortsApi().deleteCohort(ns, wsid, this.cohort.id).then(() => {
      navigate(['workspaces', ns, wsid, 'cohorts']);
    }, (error) => {
      console.log(error);
    });
  }

  cancel = () => {
    this.deleting = false;
  }

  navigateTo(action: string) {
    const {ns, wsid} = urlParamsStore.getValue();
    let url = `/workspaces/${ns}/${wsid}/`;
    switch (action) {
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
