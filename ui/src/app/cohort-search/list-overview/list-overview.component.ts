import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest} from 'app/cohort-search/utils';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {
  currentCohortStore,
  currentWorkspaceStore,
  navigate,
  navigateByUrl,
  urlParamsStore
} from 'app/utils/navigation';

import {Cohort} from 'generated/fetch';
import {fromJS} from 'immutable';

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
export class ListOverviewComponent implements OnChanges, OnInit {
  @Input() searchRequest: any;
  @Input() update: number;

  cohortForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl()
  });

  error: boolean;
  loading: boolean;
  total: number;
  chartData: any;
  saving = false;
  deleting = false;
  stackChart = false;
  showGenderChart = true;
  showComboChart = true;
  showConflictError = false;
  saveError = false;
  temporalError = false;
  cohort: Cohort;

  ngOnInit(): void {
    if (currentCohortStore.getValue()) {
      this.cohort = currentCohortStore.getValue();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.update && this.hasActiveItems && !this.hasTemporalError) {
      this.loading = true;
      this.error = false;
      this.getTotalCount();
    }
  }

  getTotalCount() {
    try {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const request = mapRequest(this.searchRequest);
      cohortBuilderApi().getDemoChartInfo(+cdrVersionId, request).then(response => {
        // TODO remove immutable conversion and modify charts to use vanilla javascript
        this.chartData = fromJS(response.items);
        this.total = response.items.reduce((sum, data) => sum + data.count, 0);
        this.loading = false;
      }, (err) => {
        console.error(err);
        this.error = true;
        this.loading = false;
      });
    } catch (error) {
      console.error(error);
      this.error = true;
      this.loading = false;
    }
  }

  get hasActiveItems() {
    return ['includes', 'excludes'].some(role => {
      const activeGroups = this.searchRequest[role].filter(grp => grp.status === 'active');
      return activeGroups.some(grp => {
        const activeItems = grp.items.filter(it => it.status === 'active');
        return activeItems.length > 0;
      });
    });
  }

  get hasTemporalError() {
    const activeGroups = this.searchRequest.includes
      .filter(grp => grp.temporal && grp.status === 'active');
    return activeGroups.some(grp => {
      const activeItems = grp.items.reduce((acc, it) => {
        if (it.status === 'active') {
          acc[it.temporalGroup]++;
        }
        return acc;
      }, [0, 0]);
      return activeItems.includes(0);
    });
  }

  get name() {
    return this.cohortForm.get('name');
  }

  get criteria() {
    const mappedRequest = mapRequest(searchRequestStore.getValue());
    return JSON.stringify(mappedRequest);
  }

  get unchanged() {
    return this.criteria === this.cohort.criteria;
  }

  modalChange(value) {
    if (!value) {
      this.cohortForm.reset();
      this.showConflictError = false;
      this.saveError = false;
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
      if (error.status === 500) {
        this.saveError = true;
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
