import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest} from 'app/cohort-search/utils';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {
  currentCohortStore,
  currentWorkspaceStore,
  navigate,
  navigateByUrl,
  urlParamsStore
} from 'app/utils/navigation';

import {Cohort, TemporalTime} from 'generated/fetch';
import {fromJS} from 'immutable';
import * as React from 'react';

const COHORT_TYPE = 'AoU_Discover';

interface Props {
  searchRequest: any;
  update: number;
}

interface State {
  error: boolean;
  loading: boolean;
  total: number;
  chartData: any;
  saving: boolean;
  deleting: boolean;
  stackChart: boolean;
  showConflictError: boolean;
  saveError: boolean;
  cohort: Cohort;
  apiCallCheck: number;
}

export const ListOverview = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        error: false,
        loading: false,
        total: undefined,
        chartData: undefined,
        saving: false,
        deleting: false,
        stackChart: false,
        showConflictError: false,
        saveError: false,
        cohort: undefined,
        apiCallCheck: 0,
      };
    }
    cohortForm = new FormGroup({
      name: new FormControl('', [Validators.required]),
      description: new FormControl()
    });

    componentDidMount(): void {
      if (currentCohortStore.getValue()) {
        this.setState({cohort: currentCohortStore.getValue()});
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (this.props.update > prevProps.update && !this.hasErrors) {
        this.setState({loading: true, error: false});
        this.getTotalCount();
      }
    }

    ngOnChanges(changes: SimpleChanges): void {
      const notFirstUpdate = changes.update && !changes.update.firstChange;
      if (notFirstUpdate && !this.hasErrors) {
        this.loading = true;
        this.error = false;
        this.getTotalCount();
      }
    }

    getTotalCount() {
      try {
        this.apiCallCheck++;
        const localCheck = this.apiCallCheck;
        const {cdrVersionId} = currentWorkspaceStore.getValue();
        const request = mapRequest(this.searchRequest);
        cohortBuilderApi().getDemoChartInfo(+cdrVersionId, request).then(response => {
          if (localCheck === this.apiCallCheck) {
            // TODO remove immutable conversion and modify charts to use vanilla javascript
            this.chartData = fromJS(response.items);
            this.total = response.items.reduce((sum, data) => sum + data.count, 0);
            this.loading = false;
          }
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
        const inputError = grp.time !== TemporalTime.DURINGSAMEENCOUNTERAS &&
          (grp.timeValue === null || grp.timeValue < 0);
        return activeItems.includes(0) || inputError;
      });
    }

    get hasErrors() {
      return this.hasTemporalError || !this.hasActiveItems;
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
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
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
  }
);

@Component({
  selector: 'app-list-overview',
  template: '<div #root></div>',
})
export class ListOverviewComponent extends ReactWrapperBase {
  @Input('searchRequest') searchRequest: Props['searchRequest'];
  @Input('update') update: Props['update'];

  constructor() {
    super(ListOverview, ['searchReaquest', 'update']);
  }
}