import {Component, Input} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {GenderChart} from 'app/cohort-search/gender-chart/gender-chart.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal';

import {
  currentCohortStore,
  currentWorkspaceStore,
  navigate,
  navigateByUrl,
  urlParamsStore
} from 'app/utils/navigation';
import {Cohort, TemporalTime} from 'generated/fetch';
import {fromJS} from 'immutable';
import {Menu} from 'primereact/menu';
import * as React from 'react';

const COHORT_TYPE = 'AoU_Discover';

const styles = reactStyles({
  overviewHeader: {
    display: 'flex',
    flexFlow: 'row wrap',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  totalError: {
    background: '#f7981c',
    color: '#ffffff',
    padding: '0.25rem 0.5rem',
    borderRadius: '5px',
    marginBottom: '0.5rem'
  },
  card: {
    background: '#ffffff',
    padding: '0.25rem 0.5rem',
    borderBottom: 'none',
    borderTop: 'none',
    borderRadius: '3px',
    marginBottom: 0,
    marginTop: 0,
  },
  cardHeader: {
    color: '#302C71',
    fontSize: '13px',
    borderBottom: 'none',
    padding: '0.5rem 0.75rem',
  }
});

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
    dropdown: any;
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
      this.getTotalCount();
    }

    // componentDidUpdate(prevProps: Readonly<Props>): void {
    //   if (this.props.update > prevProps.update && !this.hasErrors) {
    //     // TODO fix below code so it doesn't constantly call setState and crash the browser
    //     // this.setState({loading: true, error: false});
    //     // this.getTotalCount();
    //   }
    // }

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
      const {cohort, chartData, deleting, error, loading, saving, stackChart, total} = this.state;
      const disableSave = cohort && cohort.criteria === this.criteria;
      const items = [
        {label: 'Save', command: () => this.saveCohort(), disabled: disableSave},
        {label: 'Save as', command: () => {/* open modal */}},
      ];
      return <React.Fragment>
        <div>
          <div style={styles.overviewHeader}>
            <div style={{width: '100%'}}>
              {!!cohort && <React.Fragment>
                <Menu model={items} popup={true} ref={el => this.dropdown = el} />
                <Button type='primary' onClick={(event) => this.dropdown.toggle(event)}
                  disabled={loading || saving || this.hasErrors}>
                  SAVE COHORT <ClrIcon shape='caret down' />
                </Button>
              </React.Fragment>}
              {!cohort && <Button type='primary'
                onClick={() => {/* open modal */}}
                style={{float: 'right', margin: 0}}
                disabled={loading || this.hasErrors}>CREATE COHORT</Button>}
              <Clickable onClick={() => this.navigateTo('notebook')} disabled>
                <ClrIcon shape='export' className='is-solid' size={30} title='Export to notebook' />
              </Clickable>
              <Clickable onClick={() => this.setState({deleting: true})}
                 disabled={loading || !cohort}>
                <ClrIcon shape='trash' className='is-solid' size={30} title='Delete cohort' />
              </Clickable>
              <Clickable onClick={() => this.navigateTo('review')} disabled={loading || !cohort}>
                <ClrIcon shape='copy' className='is-solid' size={30}
                  title='Review participant level data' />
              </Clickable>
            </div>
            <h2 style={{fontSize: '16px', fontWeight: 'bold'}}>
              Total Count:
              {loading && !this.hasTemporalError && <Spinner size={16} />}
              {!loading && !this.hasErrors && total !== undefined &&
                <span>{total.toLocaleString()}</span>
              }
              {this.hasErrors && <span>
                -- <ClrIcon style={{color: '#F57600'}} shape='warning-standard' size={18} />
              </span>}
            </h2>
          </div>
          {error && !this.hasErrors && <div style={styles.totalError}>
            <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
            Sorry, the request cannot be completed.
          </div>}
          {!this.hasErrors && !loading && !!chartData && total && <div>
            <div style={styles.card}>
              <div style={styles.cardHeader}>
                Results by Gender
              </div>
              <div style={{padding: '0.5rem 0.75rem'}}>
                {chartData.size && <GenderChart data={chartData} />}
              </div>
            </div>
            <div style={styles.card}>
              <div style={styles.cardHeader}>
                Results By Gender, Age Range, and Race
                <ClrIcon shape='sort-by'
                  className={stackChart ? 'is-info' : ''}
                  onClick={this.toggleChartMode()} />
              </div>
              <div style={{padding: '0.5rem 0.75rem'}}>
                {chartData.size &&
                <ComboChart mode={stackChart ? 'stacked' : 'normalized'} data={chartData} />}
              </div>
            </div>
          </div>}
        </div>
        {deleting && <ConfirmDeleteModal closeFunction={this.cancel}
          resourceType='cohort'
          receiveDelete={this.delete}
          resourceName={cohort.name} />}
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
    super(ListOverview, ['searchRequest', 'update']);
  }
}
