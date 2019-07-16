import {Component, Input} from '@angular/core';
import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {GenderChart} from 'app/cohort-search/gender-chart/gender-chart.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {
  currentCohortStore,
  currentWorkspaceStore,
  navigate,
  navigateByUrl,
  urlParamsStore
} from 'app/utils/navigation';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal';
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
    justifyContent: 'space-between',
  },
  saveButton: {
    height: '1.5rem',
    fontSize: '12px',
    padding: '0 0.75rem',
    borderRadius: '3px',
    float: 'right',
  },
  totalCount: {
    marginTop: 0,
    fontSize: '16px',
    fontWeight: 'bold'
  },
  totalError: {
    background: '#f7981c',
    color: '#ffffff',
    padding: '0.25rem 0.5rem',
    borderRadius: '5px',
    marginBottom: '0.5rem'
  },
  actionIcon: {
    float: 'right',
    margin: '0 1rem 0 0',
    minWidth: 0,
    padding: 0,
    color: '#262262',
    cursor: 'pointer'
  },
  disabled: {
    opacity: 0.4,
    cursor: 'not-allowed',
    pointerEvents: 'none'
  },
  cardContainer: {
    boxShadow: '0 0.125rem 0 0 #d7d7d7',
    border: '1px solid #d7d7d7',
    borderBottom: 'none',
    borderTop: 'none',
    borderRadius: '3px',
  },
  card: {
    background: '#ffffff',
    marginBottom: 0,
    marginTop: 0,
  },
  cardHeader: {
    color: '#302C71',
    fontSize: '13px',
    borderBottom: 'none',
    padding: '0.5rem 0.75rem',
  },
  error: {
    background: '#f7981c',
    color: '#ffffff',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  },
  invalid: {
    background: '#f5dbd9',
    color: '#565656',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  },
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
  saveModal: boolean;
  name: string;
  description: string;
  nameTouched: boolean;
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
        saveModal: false,
        name: undefined,
        description: undefined,
        nameTouched: false,
        saving: false,
        deleting: false,
        stackChart: false,
        showConflictError: false,
        saveError: false,
        cohort: undefined,
        apiCallCheck: 0,
      };
    }

    componentDidMount(): void {
      if (currentCohortStore.getValue()) {
        this.setState({cohort: currentCohortStore.getValue()});
      }
      this.getTotalCount();
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

    get criteria() {
      const mappedRequest = mapRequest(searchRequestStore.getValue());
      return JSON.stringify(mappedRequest);
    }

    saveCohort() {
      const {cohort} = this.state;
      cohort.criteria = this.criteria;
      this.setState({cohort, saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const cid = cohort.id;
      cohortsApi().updateCohort(ns, wsid, cid, cohort).then(() => {
        this.setState({saving: false});
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'actions']);
      }, (error) => {
        console.error(error);
        this.setState({saving: false, saveError: true});
      });
    }

    submit() {
      this.setState({saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const {name, description} = this.state;
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
      cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
        navigate(['workspaces', ns, wsid, 'cohorts', c.id, 'actions']);
      }, (error) => {
        if (error.status === 400) {
          this.setState({saving: false, showConflictError: true});
        } else {
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
      const {cohort, chartData, deleting, error, loading, saveModal, name, description, nameTouched,
        saving, saveError, stackChart, total} = this.state;
      const disableIcon = loading || !cohort ;
      const disableSave = cohort && cohort.criteria === this.criteria;
      const invalid = nameTouched && !name;
      const items = [
        {label: 'Save', command: () => this.saveCohort(), disabled: disableSave},
        {label: 'Save as', command: () => this.setState({saveModal: true})},
      ];
      return <React.Fragment>
        <div>
          <div style={styles.overviewHeader}>
            <div style={{width: '100%'}}>
              {!!cohort && <React.Fragment>
                <Menu appendTo={document.body}
                  model={items} popup={true} ref={el => this.dropdown = el} />
                <Button type='primary' style={styles.saveButton}
                  onClick={(event) => this.dropdown.toggle(event)}
                  disabled={loading || saving || this.hasErrors}>
                  SAVE COHORT <ClrIcon shape='caret down' />
                </Button>
              </React.Fragment>}
              {!cohort && <Button type='primary'
                onClick={() => this.setState({saveModal: true})}
                style={styles.saveButton}
                disabled={loading || this.hasErrors}>CREATE COHORT</Button>}
              <TooltipTrigger content={<div>Export to notebook</div>}>
                <Clickable style={{...styles.actionIcon, ...styles.disabled}}
                  onClick={() => this.navigateTo('notebook')} disabled>
                  <ClrIcon shape='export' className='is-solid' size={30} />
                </Clickable>
              </TooltipTrigger>
              <TooltipTrigger content={<div>Delete cohort</div>}>
                <Clickable style={{...styles.actionIcon, ...(disableIcon ? styles.disabled : {})}}
                  onClick={() => this.setState({deleting: true})}>
                  <ClrIcon shape='trash' className='is-solid' size={30} />
                </Clickable>
              </TooltipTrigger>
              <TooltipTrigger content={<div>Review participant level data</div>}>
                <Clickable style={{...styles.actionIcon, ...(disableIcon ? styles.disabled : {})}}
                  onClick={() => this.navigateTo('review')}>
                  <ClrIcon shape='copy' className='is-solid' size={30} />
                </Clickable>
              </TooltipTrigger>
            </div>
            <h2 style={styles.totalCount}>
              Total Count: &nbsp;
              {loading && !this.hasTemporalError && <Spinner size={18} />}
              {!loading && !this.hasErrors && total !== undefined &&
                <span>{total.toLocaleString()}</span>
              }
              {this.hasErrors && <span>
                -- <TooltipTrigger content={this.hasTemporalError ?
                    'Please complete criteria selections before saving temporal relationship.' :
                    `All criteria are suppressed. Un-suppress criteria to update the total count
                     based on the visible criteria.`}>
                  <ClrIcon style={{color: '#F57600'}} shape='warning-standard' size={18} />
                </TooltipTrigger>
              </span>}
            </h2>
          </div>
          {error && !this.hasErrors && <div style={styles.totalError}>
            <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
            Sorry, the request cannot be completed.
          </div>}
          {!this.hasErrors && !loading && !!chartData && total && <div style={styles.cardContainer}>
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
                  onClick={() => this.toggleChartMode()} />
              </div>
              <div style={{padding: '0.5rem 0.75rem'}}>
                {chartData.size &&
                  <ComboChart mode={stackChart ? 'stacked' : 'normalized'} data={chartData} />}
              </div>
            </div>
          </div>}
        </div>
        {saveModal && <Modal>
          <ModalTitle style={invalid ? {marginBottom: 0} : {}}>Save Cohort as</ModalTitle>
          <ModalBody style={{marginTop: '0.2rem'}}>
            {saveError && <div style={styles.error}>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
              Data cannot be saved. Please try again.
            </div>}
            {invalid && <div style={styles.invalid}>Cohort name is required</div>}
            <TextInput style={{marginBottom: '0.5rem'}} value={name} placeholder='COHORT NAME'
              onChange={(v) => this.setState({name: v, nameTouched: true})}
              disabled={saving} />
            <TextArea value={description} placeholder='DESCRIPTION' disabled={saving}
              onChange={(v) => this.setState({description: v})}/>
          </ModalBody>
          <ModalFooter>
            <Button style={{color: '#262262'}} type='link' onClick={() => this.setState({
              saveModal: false, name: undefined, description: undefined, saveError: false,
              nameTouched: false
            })} disabled={saving}>CANCEL</Button>
            <Button type='primary' disabled={!name || saving} onClick={() => this.submit()}>
              {saving && <Spinner style={{marginRight: '0.25rem'}} size={18} />}
               SAVE
            </Button>
          </ModalFooter>
        </Modal>}
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
