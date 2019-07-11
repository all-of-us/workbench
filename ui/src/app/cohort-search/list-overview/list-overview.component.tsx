import {Component, Input} from '@angular/core';
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

    getTotalCount() {
      try {
        const {searchRequest} = this.props;
        const localCheck = this.state.apiCallCheck + 1;
        this.setState({apiCallCheck: localCheck});
        const {cdrVersionId} = currentWorkspaceStore.getValue();
        const request = mapRequest(searchRequest);
        cohortBuilderApi().getDemoChartInfo(+cdrVersionId, request).then(response => {
          if (localCheck === this.state.apiCallCheck) {
            // TODO remove immutable conversion and modify charts to use vanilla javascript
            this.setState({
              chartData: fromJS(response.items),
              total: response.items.reduce((sum, data) => sum + data.count, 0),
              loading: false
            });
          }
        });
      } catch (error) {
        console.error(error);
        this.setState({error: true, loading: false});
      }
    }

    get hasActiveItems() {
      const {searchRequest} = this.props;
      return ['includes', 'excludes'].some(role => {
        const activeGroups = searchRequest[role].filter(grp => grp.status === 'active');
        return activeGroups.some(grp => {
          const activeItems = grp.items.filter(it => it.status === 'active');
          return activeItems.length > 0;
        });
      });
    }

    get hasTemporalError() {
      const {searchRequest} = this.props;
      const activeGroups = searchRequest.includes
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
      const {cohort} = this.state;
      return this.criteria === cohort.criteria;
    }

    modalChange(value) {
      if (!value) {
        this.cohortForm.reset();
        this.setState({showConflictError: false, saveError: false});
      }
    }

    saveCohort() {
      const {cohort} = this.state;
      cohort.criteria = this.criteria;
      this.setState({cohort, saving: true});
      const {ns, wsid} = urlParamsStore.getValue();
      const cid = cohort.id;
      cohortsApi().updateCohort(ns, wsid, cid, cohort).then(() => {
        this.setState({saving: false});
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'actions']);
      }, (error) => {
        if (error.status === 500) {
          console.log(error);
          this.setState({saveError: true});
        }
      });
    }

    submit() {
      this.setState({saving: true});
      const {ns, wsid} = urlParamsStore.getValue();
      const name = this.cohortForm.get('name').value;
      const description = this.cohortForm.get('description').value;
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
      cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
        navigate(['workspaces', ns, wsid, 'cohorts', c.id, 'actions']);
      }, (error) => {
        if (error.status === 400) {
          this.setState({saving: false, showConflictError: true});
        }
        if (error.status === 500) {
          this.setState({saving: false, saveError: true});
        }
      });
    }

    delete = () => {
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort} = this.state;
      cohortsApi().deleteCohort(ns, wsid, cohort.id).then(() => {
        navigate(['workspaces', ns, wsid, 'cohorts']);
      }, (error) => {
        console.log(error);
      });
    }

    cancel = () => {
      this.setState({deleting: false});
    }

    navigateTo(action: string) {
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort} = this.state;
      let url = `/workspaces/${ns}/${wsid}/`;
      switch (action) {
        case 'notebook':
          url += 'notebooks';
          break;
        case 'review':
          url += `cohorts/${cohort.id}/review`;
          break;
      }
      navigateByUrl(url);
    }

    toggleChartMode() {
      const {stackChart} = this.state;
      this.setState({stackChart: !stackChart});
    }
    
    render() {
      const {cohort} = this.state;
    }
      return <React.Fragment>
        <div>
          <div className='overview-header'>
            <div className='actions-container'>
              <clr-dropdown *ngIf='cohort' style='float: right'>
              <button
                type='button'
                className='btn btn-primary'
              [disabled]='loading || saving || hasErrors'
          [clrLoading]='saving'
          clrDropdownTrigger>
          <span style='margin-right: 5px'>Save Cohort</span>
          <clr-icon shape='caret down'></clr-icon>
        </button>
        <clr-dropdown-menu>
          <button
            type='button'
            className='dropdown-item'
            [disabled]='unchanged'
            (click)='saveCohort()'
              clrDropdownItem>Save</button>
            <button
              type='button'
              className='dropdown-item'
            (click)='saveModal.open()'
            clrDropdownItem>Save as</button>
        </clr-dropdown-menu>
      </clr-dropdown>
      <button
        *ngIf='!cohort'
    type='button'
    className='btn btn-primary'
    style='float: right; margin: 0;'
    [disabled]='loading || hasErrors'
        (click)='saveModal.open()'>Create Cohort</button>
      <button
        className='btn btn-link'
        (click)='navigateTo('notebook')'
        disabled
        [popper]=''Export to notebook''
        [popperTrigger]=''hover''>
        <clr-icon shape='export' className='is-solid' size='30'></clr-icon>
      </button>
      <button
        className='btn btn-link'
        (click)='deleting = true'
        [disabled]='loading || !cohort'
        [popper]=''Delete cohort''
        [popperTrigger]=''hover''>
        <clr-icon shape='trash' className='is-solid' size='30'></clr-icon>
      </button>
      <button
        className='btn btn-link'
        (click)='navigateTo('review')'
        [disabled]='loading || !cohort'
        [popper]=''Review participant level data''
        [popperTrigger]=''hover''>
        <clr-icon shape='copy' className='is-solid disabled' size='30'></clr-icon>
      </button>
    </div>
    <h2 className='count-header' id='total-count'>
      Total Count:
      <span *ngIf='loading && !hasTemporalError' className='spinner spinner-sm'>
        Loading...
      </span>
      <span className='count-header' *ngIf='!loading && !hasErrors'>
        {{total | number}}
      </span>
      <span *ngIf='hasErrors'>
        --
        <clr-icon
          [popper]='errorPopper'
          [popperTrigger]=''hover''
          shape='warning-standard'
          size='18'></clr-icon>
      </span>
    </h2>
  </div>
  <div *ngIf='error && !hasErrors' className='total-error'>
    <clr-icon className='is-solid' shape='exclamation-triangle' size='22'></clr-icon>
    Sorry, the request cannot be completed.
  </div>
  <div *ngIf='!hasErrors && !loading && chartData && total'>
    <div className='card bg-faded'>
      <div className='card-header header-text'>
        Results by Gender
      </div>
      <div className='card-block'>
        <app-gender-chart *ngIf='chartData.size'
          [data]='chartData'>
        </app-gender-chart>
      </div>
    </div>
    <div className='card bg-faded'>
      <div className='card-header header-text'>
        Results By Gender, Age Range, and Race
        <clr-icon shape='sort-by'
          [className.is-info]='stackChart'
          (click)='toggleChartMode()'>
        </clr-icon>
      </div>
      <div className='card-block'>
        <app-combo-chart *ngIf='chartData.size'
          [data]='chartData'
          [mode]='stackChart ? 'stacked' : 'normalized''>
        </app-combo-chart>
      </div>
    </div>
  </div>
</div>
<popper-content #errorPopper>
  <p   *ngIf='error && !hasTemporalError' [style.margin]=''0''>
    A problem occurred and we were not able to retrieve the requested data
    </p>
    {this.hasTemporalError && this.hasActiveItems && <p style={{margin: 0}}>
        Please complete criteria selections before saving temporal relationship.
      </p>}
    <p *ngIf='!hasActiveItems' [style.margin]=''0''>
        All criteria are suppressed. Un-suppress criteria to update the total count based on the visible criteria.
    </p>
    </popper-content>
    <clr-modal #saveModal
    [clrModalSize]=''md''
    (clrModalOpenChange)='modalChange($event)'>
    <div className='modal-title'>
    <h2>Save New Cohort</h2>
    </div>
    <div className='modal-body'>
    <form [formGroup]='cohortForm' (ngSubmit)='submit()'>
    <label
    for='name-control'
    [className.show]='(name.dirty || name.touched) && cohortForm.invalid'>
    <em>*A Cohort Name is Required</em>
    </label>
    <div className='name-area'>
    <input
    name='name'
    id='name-control'
    className='control'
    placeholder='Cohort Name'
    maxlength='80'
    formControlName='name'
    type='text'
    [className.invalid]='(name.dirty || name.touched) && cohortForm.invalid'
    (input)='showConflictError = false'
    [className.alreadyTaken]='showConflictError'
    autofocus>
    <clr-icon shape='warning-standard' className='warning-standard is-solid' *ngIf='showConflictError'></clr-icon>
    <div *ngIf='showConflictError' className='error'>
    {{name.value}} already exists. Please choose another name.
    </div>
    </div>
    <textarea
    name='description'
    id='description-control'
    className='control'
    placeholder='Description'
    rows='60'
    formControlName='description'>
    </textarea>
    </form>
    <div *ngIf='saveError' className='total-error'>
    <clr-icon className='is-solid' shape='exclamation-triangle' size='22'></clr-icon>
    Data cannot be saved. Please try again.
    </div>
    </div>
    <div className='modal-footer'>
    <div>
    <button
    type='button'
    className='btn btn-link'
    (click)='saveModal.close()'>
    Cancel
    </button>
    <button
    type='button'
    className='btn btn-primary'
    (click)='submit()'
    [clrLoading]='saving'
    [disabled]='!cohortForm.valid || saveError'>
    Save Cohort
    </button>
    </div>
    </div>
    </clr-modal>
    <app-confirm-delete-modal
    *ngIf='deleting'
    [resourceName]='cohort.name'
    [resourceType]=''cohort''
    [receiveDelete]='delete'
    [closeFunction]='cancel'>
    </app-confirm-delete-modal>
      </React.Fragment>;
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
