import * as fp from 'lodash/fp';
import {Menu} from 'primereact/menu';
import * as React from 'react';

import {GenderChart} from 'app/cohort-search/gender-chart/gender-chart.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {ageTypeToText, genderOrSexTypeToText, mapRequest} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ComboChart} from 'app/components/combo-chart.component';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {CreateModal} from 'app/components/create-modal';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {isAbortError} from 'app/utils/errors';
import {currentWorkspaceStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  AgeType,
  CdrVersionTiersResponse,
  Cohort,
  GenderOrSexType,
  ResourceType,
  TemporalTime,
  WorkspaceAccessLevel
} from 'generated/fetch';

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
  menuButton: {
    background: colors.white,
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
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
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
  genderOrSexType: GenderOrSexType;
  initializing: boolean;
  loading: boolean;
  refreshing: boolean;
  saveModalOpen: boolean;
  saving: boolean;
  stackChart: boolean;
  total: number;
}

export const ListOverview = fp.flow(withCurrentWorkspace(), withCdrVersions()) (
  class extends React.Component<Props, State> {
    private aborter = new AbortController();
    private ageMenu: any;
    private genderOrSexMenu: any;
    private saveMenu: any;
    constructor(props: Props) {
      super(props);
      this.state = {
        ageType: AgeType.AGEATCDR,
        apiCallCheck: 0,
        apiError: false,
        chartData: undefined,
        currentGraphOptions: {ageType: AgeType.AGEATCDR, genderOrSexType: GenderOrSexType.GENDER},
        deleting: false,
        genderOrSexType: GenderOrSexType.GENDER,
        initializing: true,
        loading: false,
        refreshing: false,
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
      const {searchRequest} = this.props;
      if (searchRequest.includes.length > 0) {
        const {loading, refreshing} = this.state;
        if (loading || refreshing) {
          this.aborter.abort();
          this.aborter = new AbortController();
        }
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
      const {id, namespace} = currentWorkspaceStore.getValue();
      const request = mapRequest(searchRequest);
      return cohortBuilderApi()
        .findDemoChartInfo(namespace, id, genderOrSexType.toString(), ageType.toString(), request, {signal: this.aborter.signal});
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
      this.setState({saving: true});
      const {ns, wsid} = urlParamsStore.getValue();
      const cid = cohort.id;
      cohortsApi().updateCohort(ns, wsid, cid, cohort)
        .then(() => {
          this.setState({saving: false});
          updating(true);
          navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'actions']);
        })
        .catch(error => {
          console.error(error);
          this.setState({saving: false});
        });
    }

    createCohort(name, description) {
      triggerEvent('Click icon', 'Click', 'Icon - Save As - Cohort Builder');
      this.setState({saving: true});
      const {ns, wsid} = urlParamsStore.getValue();
      const {updating} = this.props;
      const cohort = {name, description, criteria: this.criteria, type: COHORT_TYPE};
      return cohortsApi().createCohort(ns, wsid, cohort)
        .then((c) => {
          updating(true);
          navigate(['workspaces', ns, wsid, 'data', 'cohorts', c.id, 'actions']);
        })
        .finally(() => this.setState({saving: false}));
    }

    delete = () => {
      triggerEvent('Click icon', 'Click', 'Icon - Delete - Cohort Builder');
      const {ns, wsid} = urlParamsStore.getValue();
      const {cohort, updating} = this.props;
      cohortsApi().deleteCohort(ns, wsid, cohort.id)
        .then(() => {
          updating();
          navigate(['workspaces', ns, wsid, 'data']);
        })
        .catch(error => console.error(error));
    }

    cancelDelete = () => {
      this.setState({deleting: false});
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
    }

    async getCohortNames() {
      const {ns, wsid} = urlParamsStore.getValue();
      const response = await cohortsApi().getCohortsInWorkspace(ns, wsid);
      return response.items.map(cohort => cohort.name);
    }

    get ageItems() {
      return [
        {label: ageTypeToText(AgeType.AGE), command: () => this.setState({ageType: AgeType.AGE})},
        {label: ageTypeToText(AgeType.AGEATCONSENT), command: () => this.setState({ageType: AgeType.AGEATCONSENT})},
        {label: ageTypeToText(AgeType.AGEATCDR), command: () => this.setState({ageType: AgeType.AGEATCDR})},
      ];
    }

    get genderOrSexItems() {
      return [{
        label: genderOrSexTypeToText(GenderOrSexType.GENDER),
        command: () => this.setState({genderOrSexType: GenderOrSexType.GENDER})
      }, {
        label: genderOrSexTypeToText(GenderOrSexType.SEXATBIRTH),
        command: () => this.setState({genderOrSexType: GenderOrSexType.SEXATBIRTH})
      }];
    }

    get saveItems() {
      return [
        {label: 'Save', command: () => this.saveCohort(), disabled: !this.props.cohortChanged},
        {label: 'Save as', command: () => this.openSaveModal()},
      ];
    }

    get disableActionIcons() {
      return this.state.loading || !this.props.cohort.id;
    }

    get disableDeleteIcon() {
      return this.disableActionIcons || this.props.workspace.accessLevel === WorkspaceAccessLevel.READER;
    }

    get disableSaveButton() {
      const {loading, saving, total} = this.state;
      return loading || saving || this.definitionErrors || !total;
    }

    get disableRefreshButton() {
      const {ageType, currentGraphOptions, genderOrSexType} = this.state;
      return ageType === currentGraphOptions.ageType && genderOrSexType === currentGraphOptions.genderOrSexType;
    }

    get showTotalCount() {
      return ![null, undefined].includes(this.state.total);
    }

    render() {
      const {cohort} = this.props;
      const {ageType, apiError, chartData, currentGraphOptions, deleting, genderOrSexType, loading, refreshing,
        saveModalOpen, stackChart, total} = this.state;
      return <React.Fragment>
        <div>
          <div style={styles.overviewHeader}>
            <div style={{width: '100%'}}>
              {!!cohort.id ? <React.Fragment>
                <Menu style={{width: 'fit-content', minWidth: '7.25rem'}}
                  appendTo={document.body}
                  model={this.saveItems}
                  popup={true}
                  ref={el => this.saveMenu = el}/>
                <Button type='primary'
                  style={styles.saveButton}
                  onClick={(event) => this.saveMenu.toggle(event)}
                  disabled={this.disableSaveButton}>
                  Save Cohort <ClrIcon shape='caret down'/>
                </Button>
              </React.Fragment>
              : <Button type='primary'
                onClick={() => this.openSaveModal()}
                style={styles.saveButton}
                disabled={this.disableSaveButton}>Create Cohort</Button>}
              <TooltipTrigger content={<div>Export to notebook</div>}>
                <Clickable style={{...styles.actionIcon, ...styles.disabled}}
                  onClick={() => this.navigateTo('notebook')} disabled>
                  <ClrIcon shape='export' className='is-solid' size={30} />
                </Clickable>
              </TooltipTrigger>
              <TooltipTrigger content={<div>Delete cohort</div>}>
                <Clickable style={this.disableDeleteIcon ? {...styles.actionIcon, ...styles.disabled} : styles.actionIcon}
                  onClick={() => this.setState({deleting: true})}>
                  <ClrIcon shape='trash' className='is-solid' size={30} />
                </Clickable>
              </TooltipTrigger>
              <TooltipTrigger content={<div>Review participant level data</div>}>
                <Clickable style={this.disableActionIcons ? {...styles.actionIcon, ...styles.disabled} : styles.actionIcon}
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
              : loading ? <Spinner size={18} /> : <span>{this.showTotalCount && total.toLocaleString()}</span>}
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
                  <Menu appendTo={document.body}
                    model={this.genderOrSexItems}
                    popup={true} ref={el => this.genderOrSexMenu = el}/>
                  <button style={styles.menuButton} onClick={(event) => this.genderOrSexMenu.toggle(event)}>
                    {genderOrSexTypeToText(genderOrSexType)} <ClrIcon style={{float: 'right'}} shape='caret down' size={12}/>
                  </button>
                  <Menu appendTo={document.body}
                    model={this.ageItems} popup={true}
                    ref={el => this.ageMenu = el}/>
                  <button style={styles.menuButton} onClick={(event) => this.ageMenu.toggle(event)}>
                    {ageTypeToText(ageType)} <ClrIcon style={{float: 'right'}} shape='caret down' size={12}/>
                  </button>
                  <button style={this.disableRefreshButton ? {...styles.refreshButton, ...styles.disabled} : styles.refreshButton}
                    onClick={() => this.refreshGraphs()}>
                    REFRESH
                  </button>
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

        {saveModalOpen && <CreateModal entityName='Cohort'
                                       title='Save Cohort as'
                                       getExistingNames={() => this.getCohortNames()}
                                       save={(name, desc) => this.createCohort(name, desc)}
                                       close={() => this.setState({saveModalOpen: false})}/>}

        {deleting && <ConfirmDeleteModal closeFunction={this.cancelDelete}
          resourceType={ResourceType.COHORT}
          receiveDelete={this.delete}
          resourceName={cohort.name} />}
      </React.Fragment>;
    }
  }
);
