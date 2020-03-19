import {Component, Input} from '@angular/core';
import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {GenderChart} from 'app/cohort-search/gender-chart/gender-chart.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {ageTypeToText, genderOrSexTypeToText, mapRequest} from 'app/cohort-search/utils';
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
import {isAbortError} from 'app/utils/errors';
import {currentWorkspaceStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {AgeType, Cohort, GenderOrSexType, ResourceType, TemporalTime} from 'generated/fetch';
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
    padding: '0 0.4rem'
  },
  cardHeader: {
    color: colors.primary,
    fontSize: '12px',
    fontWeight: 600,
    borderBottom: 'none',
    padding: '0.5rem 0',
  },
  chartSpinner: {
    marginLeft: 'calc(50% - 50px)',
    marginTop: 'calc(50% - 75px)',
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
  menuButton: {
    border: `1px solid ${colorWithWhiteness(colors.black, .8)}`,
    borderRadius: '0.125rem',
    color: colors.primary,
    cursor: 'pointer',
    fontSize: '11px',
    fontWeight: 100,
    height: '1.25rem',
    lineHeight: '0.75rem',
    marginRight: '0.25rem',
    padding: '0.25rem',
    textAlign: 'left',
    verticalAlign: 'middle',
    width: '35%',
  },
  refreshButton: {
    background: 'none',
    border: `1px solid ${colors.accent}`,
    color: colors.accent,
    cursor: 'pointer'
  }
});

interface Props {
  cohort: Cohort;
  cohortChanged: boolean;
  searchRequest: any;
  updateCount: any;
  updating: Function;
}

interface State {
  ageType: AgeType;
  apiCallCheck: number;
  apiError: boolean;
  chartData: any;
  currentGraphOptions: {
    ageType: AgeType,
    genderOrSexType: GenderOrSexType
  };
  deleting: boolean;
  description: string;
  existingCohorts: Array<string>;
  genderOrSexType: GenderOrSexType;
  initializing: boolean;
  loading: boolean;
  name: string;
  nameTouched: boolean;
  refreshing: boolean;
  saveError: boolean;
  saveModalOpen: boolean;
  saving: boolean;
  stackChart: boolean;
  total: number;
}

export const ListOverview = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private aborter = new AbortController();
    private ageMenu: any;
    private genderOrSexMenu: any;
    private saveMenu: any;
    constructor(props: Props) {
      super(props);
      this.state = {
        ageType: AgeType.AGE,
        apiCallCheck: 0,
        apiError: false,
        chartData: undefined,
        currentGraphOptions: {ageType: AgeType.AGE, genderOrSexType: GenderOrSexType.GENDER},
        deleting: false,
        description: undefined,
        existingCohorts: [],
        genderOrSexType: GenderOrSexType.GENDER,
        initializing: true,
        loading: false,
        name: undefined,
        nameTouched: false,
        refreshing: false,
        saveError: false,
        saveModalOpen: false,
        saving: false,
        stackChart: true,
        total: undefined,
      };
    }

    componentDidMount(): void {
      this.getTotalCount();
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (!this.state.initializing && this.props.updateCount > prevProps.updateCount && !this.definitionErrors) {
        this.getTotalCount();
      }
    }

    componentWillUnmount(): void {
      this.aborter.abort();
    }

    getTotalCount() {
      const {loading, refreshing} = this.state;
      if (loading || refreshing) {
        this.aborter.abort();
        this.aborter = new AbortController();
      }
      const {searchRequest} = this.props;
      if (searchRequest.includes.length > 0) {
        this.setState({loading: true, apiError: false});
        this.callApi()
          .then(response => {
            this.setState({
              chartData: response.items,
              initializing: false,
              loading: false,
              total: response.items.reduce((sum, data) => sum + data.count, 0),
            });
          })
          .catch(error => {
            if (!isAbortError(error)) {
              console.error(error);
              this.setState({apiError: true, loading: false});
            }
          });
      } else {
        this.setState({chartData: [], total: 0, initializing: false});
      }
    }

    refreshGraphs() {
      this.setState({refreshing: true});
      this.callApi()
        .then(response => {
          const {ageType, genderOrSexType} = this.state;
          this.setState({chartData: response.items, currentGraphOptions: {ageType, genderOrSexType}});
        })
        .catch(error => {
          if (!isAbortError(error)) {
            console.error(error);
          }
        })
        .finally(() => this.setState({refreshing: false}));
    }

    callApi() {
      const {searchRequest} = this.props;
      const {ageType, genderOrSexType} = this.state;
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const request = mapRequest(searchRequest);
      return cohortBuilderApi()
        .getDemoChartInfo(+cdrVersionId, genderOrSexType.toString(), ageType.toString(), request, {signal: this.aborter.signal});
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
      const activeGroups = searchRequest.includes.filter(grp => grp.temporal && grp.status === 'active');
      return activeGroups.some(grp => {
        const activeItems = grp.items.reduce((acc, it) => {
          if (it.status === 'active') {
            acc[it.temporalGroup]++;
          }
          return acc;
        }, [0, 0]);
        const inputError = grp.time !== TemporalTime.DURINGSAMEENCOUNTERAS && (isNaN(parseInt(grp.timeValue, 10)) || grp.timeValue < 0);
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
      const {cohort, updating} = this.props;
      cohort.criteria = this.criteria;
      this.setState({saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const cid = cohort.id;
      cohortsApi().updateCohort(ns, wsid, cid, cohort).then(() => {
        this.setState({saving: false});
        updating(true);
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'actions']);
      }, (error) => {
        console.error(error);
        this.setState({saving: false, saveError: true});
      });
    }

    submit() {
      triggerEvent('Click icon', 'Click', 'Icon - Save As - Cohort Builder');
      this.setState({saving: true, saveError: false});
      const {ns, wsid} = urlParamsStore.getValue();
      const {name, description} = this.state;
      const {updating} = this.props;
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
      cohortsApi().createCohort(ns, wsid, cohort).then((c) => {
        updating(true);
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', c.id, 'actions']);
      }, (error) => {
        console.error(error);
        this.setState({saving: false, saveError: true});
      });
    }

    delete = () => {
      triggerEvent('Click icon', 'Click', 'Icon - Delete - Cohort Builder');
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort, updating} = this.props;
      cohortsApi().deleteCohort(ns, wsid, cohort.id).then(() => {
        updating();
        navigate(['workspaces', ns, wsid, 'data']);
      }, (error) => {
        console.log(error);
      });
    }

    cancelDelete = () => {
      this.setState({deleting: false});
    }

    cancelSave = () => {
      this.setState({saveModalOpen: false, name: undefined, description: undefined, saveError: false, nameTouched: false});
    }

    navigateTo(action: string) {
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort} = this.props;
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
      const {cohort} = this.props;
      const {ageType, apiError, chartData, currentGraphOptions, deleting, description, existingCohorts, genderOrSexType, loading,
        name, nameTouched, refreshing, saveModalOpen, saveError, saving, stackChart, total} = this.state;
      const disableIcon = loading || !cohort ;
      const disableSave = loading || saving || this.definitionErrors || !total;
      const disableRefresh = ageType === currentGraphOptions.ageType && genderOrSexType === currentGraphOptions.genderOrSexType;
      const invalid = nameTouched && (!name || !name.trim());
      const nameConflict = !!name && existingCohorts.includes(name.trim());
      const saveDisabled = invalid || !name || nameConflict || saving;
      const showTotal = total !== undefined && total !== null;
      const saveItems = [
        {label: 'Save', command: () => this.saveCohort(), disabled: !this.props.cohortChanged},
        {label: 'Save as', command: () => this.openSaveModal()},
      ];
      const genderOrSexItems = [
        {
          label: genderOrSexTypeToText(GenderOrSexType.GENDER),
          command: () => this.setState({genderOrSexType: GenderOrSexType.GENDER})
        },
        {
          label: genderOrSexTypeToText(GenderOrSexType.SEXATBIRTH),
          command: () => this.setState({genderOrSexType: GenderOrSexType.SEXATBIRTH})
        },
      ];
      const ageItems = [
        {label: ageTypeToText(AgeType.AGE), command: () => this.setState({ageType: AgeType.AGE})},
        {label: ageTypeToText(AgeType.AGEATCONSENT), command: () => this.setState({ageType: AgeType.AGEATCONSENT})},
        {label: ageTypeToText(AgeType.AGEATCDR), command: () => this.setState({ageType: AgeType.AGEATCDR})},
      ];
      return <React.Fragment>
        <div>
          <div style={styles.overviewHeader}>
            <div style={{width: '100%'}}>
              {!!cohort.id ? <React.Fragment>
                <Menu appendTo={document.body} model={saveItems} popup={true} ref={el => this.saveMenu = el} />
                <Button type='primary' style={styles.saveButton} onClick={(event) => this.saveMenu.toggle(event)} disabled={disableSave}>
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
                <div style={styles.cardHeader}>Results by</div>
                <div style={refreshing ? styles.disabled : {}}>
                  <Menu appendTo={document.body} model={genderOrSexItems} popup={true} ref={el => this.genderOrSexMenu = el} />
                  <button style={styles.menuButton} onClick={(event) => this.genderOrSexMenu.toggle(event)}>
                    {genderOrSexTypeToText(genderOrSexType)} <ClrIcon style={{float: 'right'}} shape='caret down' size={12}/>
                  </button>
                  <Menu appendTo={document.body} model={ageItems} popup={true} ref={el => this.ageMenu = el} />
                  <button style={styles.menuButton} onClick={(event) => this.ageMenu.toggle(event)}>
                    {ageTypeToText(ageType)} <ClrIcon style={{float: 'right'}} shape='caret down' size={12}/>
                  </button>
                  <button style={disableRefresh ? {...styles.refreshButton, ...styles.disabled} : styles.refreshButton}
                    onClick={() => this.refreshGraphs()}>REFRESH</button>
                </div>
                {refreshing ?
                  <div style={{height: '15rem'}}>
                    <Spinner style={styles.chartSpinner} size={75}/>
                  </div> :
                  <React.Fragment>
                    <div style={styles.cardHeader}>
                      {genderOrSexTypeToText(currentGraphOptions.genderOrSexType)}
                    </div>
                    <div style={{padding: '0.5rem 0.75rem'}}
                      onMouseEnter={() => triggerEvent('Graphs', 'Hover', 'Graphs - Gender - Cohort Builder')}>
                      {!!chartData.length && <GenderChart data={chartData} />}
                    </div>
                    <div style={styles.cardHeader}>
                      {genderOrSexTypeToText(currentGraphOptions.genderOrSexType)}, {ageTypeToText(currentGraphOptions.ageType)}, and Race
                      <ClrIcon shape='sort-by'
                        className={stackChart ? 'is-info' : ''}
                        onClick={() => this.toggleChartMode()} />
                    </div>
                    <div style={{padding: '0.5rem 0.75rem'}} onMouseEnter={() => triggerEvent(
                      'Graphs',
                      'Hover',
                      'Graphs - Gender Age Race - Cohort Builder'
                    )}>
                      {!!chartData.length && <ComboChart mode={stackChart ? 'stacked' : 'normalized'} data={chartData} />}
                    </div>
                  </React.Fragment>
                }
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
            <Button type='primary' disabled={saveDisabled} onClick={() => this.submit()}>
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
  @Input('cohort') cohort: Props['cohort'];
  @Input('cohortChanged') cohortChanged: Props['cohortChanged'];
  @Input('searchRequest') searchRequest: Props['searchRequest'];
  @Input('updateCount') updateCount: Props['updateCount'];
  @Input('updating') updating: Props['updating'];

  constructor() {
    super(ListOverview, ['cohort', 'cohortChanged', 'searchRequest', 'updateCount', 'updating']);
  }
}
