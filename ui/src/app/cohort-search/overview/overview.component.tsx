import {Component, Input} from '@angular/core';
import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {GenderChart} from 'app/cohort-search/gender-chart/gender-chart.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentCohortStore, currentWorkspaceStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import {Cohort, TemporalTime} from 'generated/fetch';
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
    color: colors.white,
    padding: '0.25rem 0.5rem',
    borderRadius: '5px',
    marginBottom: '0.5rem'
  },
  actionIcon: {
    float: 'right',
    margin: '0 1rem 0 0',
    minWidth: 0,
    padding: 0,
    color: colors.primary,
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
    background: colors.white,
    marginBottom: 0,
    marginTop: 0,
  },
  cardHeader: {
    color: colors.primary,
    fontSize: '13px',
    borderBottom: 'none',
    padding: '0.5rem 0.75rem',
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  },
  invalid: {
    background: colorWithWhiteness(colors.danger, .7),
    color: colorWithWhiteness(colors.dark, .1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginBottom: '0.25rem',
    padding: '3px 5px'
  },
});

interface Props {
  searchRequest: any;
  updateCount: number;
  updateSaving: Function;
}

interface State {
  apiCallCheck: number;
  apiError: boolean;
  chartData: any;
  cohort: Cohort;
  deleting: boolean;
  description: string;
  existingCohorts: Array<string>;
  loading: boolean;
  name: string;
  nameTouched: boolean;
  saveError: boolean;
  saveModalOpen: boolean;
  saving: boolean;
  stackChart: boolean;
  total: number;
}

export const ListOverview = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    dropdown: any;
    constructor(props: Props) {
      super(props);
      this.state = {
        apiCallCheck: 0,
        apiError: false,
        chartData: undefined,
        cohort: undefined,
        deleting: false,
        description: undefined,
        existingCohorts: [],
        loading: true,
        name: undefined,
        nameTouched: false,
        saveError: false,
        saveModalOpen: false,
        saving: false,
        stackChart: true,
        total: undefined,
      };
    }

    componentDidMount(): void {
      if (currentCohortStore.getValue()) {
        this.setState({cohort: currentCohortStore.getValue()});
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (this.props.updateCount > prevProps.updateCount && !this.definitionErrors) {
        this.setState({loading: true, apiError: false});
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
        if (request.includes.length > 0) {
          cohortBuilderApi().getDemoChartInfo(+cdrVersionId, request).then(response => {
            if (localCheck === this.state.apiCallCheck) {
              this.setState({
                chartData: response.items,
                total: response.items.reduce((sum, data) => sum + data.count, 0),
                loading: false
              });
            }
          });
        } else {
          this.setState({chartData: [], total: 0, loading: false});
        }
      } catch (error) {
        console.error(error);
        this.setState({apiError: true, loading: false});
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

    get definitionErrors() {
      return this.hasTemporalError || !this.hasActiveItems;
    }

    get criteria() {
      const mappedRequest = mapRequest(searchRequestStore.getValue());
      return JSON.stringify(mappedRequest);
    }

    saveCohort() {
      triggerEvent('Click icon', 'Click', 'Icon - Save - Cohort Builder');
      this.props.updateSaving(true);
      const {cohort} = this.state;
      cohort.criteria = this.criteria;
      this.setState({saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const cid = cohort.id;
      cohortsApi().updateCohort(ns, wsid, cid, cohort).then(() => {
        this.setState({saving: false});
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'actions']);
      }, (error) => {
        console.error(error);
        this.setState({saving: false, saveError: true});
      });
    }

    submit() {
      triggerEvent('Click icon', 'Click', 'Icon - Save As - Cohort Builder');
      this.props.updateSaving(true);
      this.setState({saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const {name, description} = this.state;
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
      cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', c.id, 'actions']);
      }, (error) => {
        console.error(error);
        this.setState({saving: false, saveError: true});
      });
    }

    delete = () => {
      triggerEvent('Click icon', 'Click', 'Icon - Delete - Cohort Builder');
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort} = this.state;
      cohortsApi().deleteCohort(ns, wsid, cohort.id).then(() => {
        navigate(['workspaces', ns, wsid, 'data', 'cohorts']);
      }, (error) => {
        console.log(error);
      });
    }

    cancelDelete = () => {
      this.setState({deleting: false});
    }

    cancelSave = () => {
      this.props.updateSaving(false);
      this.setState({saveModalOpen: false, name: undefined, description: undefined,
        saveError: false, nameTouched: false});
    }

    navigateTo(action: string) {
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort} = this.state;
      let url = `/workspaces/${ns}/${wsid}/`;
      switch (action) {
        case 'notebook':
          triggerEvent('Click icon', 'Click', 'Icon - Export - Cohort Builder');
          url += 'notebooks';
          break;
        case 'review':
          triggerEvent('Click icon', 'Click', 'Icon - Review - Cohort Builder');
          url += `data/cohorts/${cohort.id}/review`;
          break;
      }
      navigateByUrl(url);
    }

    toggleChartMode() {
      triggerEvent('Graphs', 'Click', 'Graphs - Flip - Gender Age Race - Cohort Builder');
      const {stackChart} = this.state;
      this.setState({stackChart: !stackChart});
    }

    openSaveModal() {
      this.setState({saveModalOpen: true});
      const {ns, wsid} = urlParamsStore.getValue();
      cohortsApi().getCohortsInWorkspace(ns, wsid).then(response => {
        this.setState({existingCohorts: response.items.map(cohort => cohort.name)});
      }, (error) => console.error(error));
    }

    render() {
      const {apiError, cohort, chartData, deleting, description, existingCohorts, loading, name, nameTouched, saveModalOpen, saveError,
        saving, stackChart, total} = this.state;
      const disableIcon = loading || !cohort ;
      const disableSave = loading || saving || this.definitionErrors || !total;
      const invalid = nameTouched && !name;
      const nameConflict = existingCohorts.includes(name);
      const showTotal = total !== undefined && total !== null;
      const items = [
        {label: 'Save', command: () => this.saveCohort(),
          disabled: cohort && cohort.criteria === this.criteria},
        {label: 'Save as', command: () => this.openSaveModal()},
      ];
      return <React.Fragment>
        <div>
          <div style={styles.overviewHeader}>
            <div style={{width: '100%'}}>
              {!!cohort ? <React.Fragment>
                <Menu appendTo={document.body} model={items} popup={true} ref={el => this.dropdown = el} />
                <Button type='primary' style={styles.saveButton} onClick={(event) => this.dropdown.toggle(event)} disabled={disableSave}>
                  Save Cohort <ClrIcon shape='caret down' />
                </Button>
              </React.Fragment>
              : <Button type='primary'
                onClick={() => this.openSaveModal()}
                style={styles.saveButton}
                disabled={disableSave}>Create Cohort</Button>}
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
              {this.definitionErrors ? <span>
                -- <TooltipTrigger content={this.hasTemporalError ?
                'Please complete criteria selections before saving temporal relationship.' :
                `All criteria are suppressed. Un-suppress criteria to update the total count
                     based on the visible criteria.`}>
                  <ClrIcon style={{color: '#F57600'}} shape='warning-standard' size={18} />
                </TooltipTrigger>
              </span>
              : loading ? <Spinner size={18} /> : <span>{showTotal && total.toLocaleString()}</span>}
            </h2>
          </div>
          {apiError && !this.definitionErrors && <div style={styles.totalError}>
            <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
            Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
          </div>}
          {!!total && !this.definitionErrors && !loading && !!chartData &&
            <div style={styles.cardContainer}>
              <div style={styles.card}>
                <div style={styles.cardHeader}>
                  Results by Gender Identity
                </div>
                <div style={{padding: '0.5rem 0.75rem'}} onMouseEnter={() => triggerEvent(
                  'Graphs',
                  'Hover',
                  'Graphs - Gender - Cohort Builder'
                )}>
                  {!!chartData.length && <GenderChart data={chartData} />}
                </div>
              </div>
              <div style={styles.card}>
                <div style={styles.cardHeader}>
                  Results By Gender Identity, Age Range, and Race
                  <ClrIcon shape='sort-by'
                    className={stackChart ? 'is-info' : ''}
                    onClick={() => this.toggleChartMode()} />
                </div>
                <div style={{padding: '0.5rem 0.75rem'}} onMouseEnter={() => triggerEvent(
                  'Graphs',
                  'Hover',
                  'Graphs - Gender Age Race - Cohort Builder'
                )}>
                  {!!chartData.length &&
                    <ComboChart mode={stackChart ? 'stacked' : 'normalized'} data={chartData} />}
                </div>
              </div>
            </div>
          }
        </div>
        {saveModalOpen && <Modal>
          <ModalTitle style={invalid ? {marginBottom: 0} : {}}>Save Cohort as</ModalTitle>
          <ModalBody style={{marginTop: '0.2rem'}}>
            {saveError && <div style={styles.error}>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
              Data cannot be saved. Please try again.
            </div>}
            {invalid && <div style={styles.invalid}>Cohort name is required</div>}
            {nameConflict && <div style={styles.invalid}>A cohort with this name already exists. Please choose a different name.</div>}
            <TextInput style={{marginBottom: '0.5rem'}} value={name} placeholder='COHORT NAME'
              onChange={(v) => this.setState({name: v, nameTouched: true})} disabled={saving} />
            <TextArea value={description} placeholder='DESCRIPTION' disabled={saving}
              onChange={(v) => this.setState({description: v})}/>
          </ModalBody>
          <ModalFooter>
            <Button style={{color: colors.primary}} type='link' onClick={() => this.cancelSave()}
              disabled={saving}>Cancel</Button>
            <Button type='primary' disabled={!name || nameConflict || saving} onClick={() => this.submit()}>
              {saving && <Spinner style={{marginRight: '0.25rem'}} size={18} />}
               Save
            </Button>
          </ModalFooter>
        </Modal>}
        {deleting && <ConfirmDeleteModal closeFunction={this.cancelDelete}
          resourceType={ResourceType.COHORT}
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
export class OverviewComponent extends ReactWrapperBase {
  @Input('searchRequest') searchRequest: Props['searchRequest'];
  @Input('updateCount') updateCount: Props['updateCount'];
  @Input('updateSaving') updateSaving: Props['updateSaving'];

  constructor() {
    super(ListOverview, ['searchRequest', 'updateCount', 'updateSaving']);
  }
}
