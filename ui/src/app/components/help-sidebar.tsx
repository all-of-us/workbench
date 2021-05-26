import {Component, Input} from '@angular/core';
import {faEdit} from '@fortawesome/free-regular-svg-icons';
import {
  faBook,
  faEllipsisV,
  faFolderOpen,
  faInbox,
  faInfoCircle, IconDefinition
} from '@fortawesome/free-solid-svg-icons';
import {faDna} from '@fortawesome/free-solid-svg-icons/faDna';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {CSSProperties} from 'react';
import * as React from 'react';
import {CSSTransition, TransitionGroup} from 'react-transition-group';
import {Subscription} from 'rxjs/Subscription';

import {faCircle} from '@fortawesome/free-solid-svg-icons/faCircle';
import {faSyncAlt} from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import {SelectionList} from 'app/cohort-search/selection-list/selection-list.component';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TooltipTrigger} from 'app/components/popups';
import {PopupTrigger} from 'app/components/popups';
import {RuntimePanel} from 'app/pages/analysis/runtime-panel';
import {SidebarContent} from 'app/pages/data/cohort-review/sidebar-content.component';
import {ConceptListPage} from 'app/pages/data/concept/concept-list';
import {participantStore} from 'app/services/review-state.service';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  DEFAULT,
  reactStyles,
  ReactWrapperBase, switchCase, withCdrVersions,
  withCurrentCohortCriteria,
  withCurrentConcept,
  withCurrentWorkspace,
  withUserProfile
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {
  currentCohortSearchContextStore,
  currentConceptStore,
  NavStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {withRuntimeStore} from 'app/utils/runtime-utils';
import {
  CompoundRuntimeOpStore,
  compoundRuntimeOpStore, GenomicExtractionStore, genomicExtractionStore,
  routeDataStore,
  RuntimeStore,
  serverConfigStore, updateGenomicExtractionStore,
  withStore
} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';

import {GenomicsExtractionTable} from 'app/components/genomics-extraction-table';
import {HelpTips} from 'app/components/help-tips';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import {getCdrVersion} from 'app/utils/cdr-versions';
import {
  CdrVersionTiersResponse,
  Criteria, GenomicExtractionJob,
  ParticipantCohortStatus,
  RuntimeStatus, TerraJobStatus,
  WorkspaceAccessLevel
} from 'generated/fetch';
import {Clickable, MenuItem, StyledAnchorTag} from './buttons';
import {Spinner} from './spinners';

const LOCAL_STORAGE_KEY_SIDEBAR_STATE = 'WORKSPACE_SIDEBAR_STATE';

const proIcons = {
  arrowLeft: '/assets/icons/arrow-left-regular.svg',
  runtime: '/assets/icons/thunderstorm-solid.svg',
  times: '/assets/icons/times-light.svg'
};

const styles = reactStyles({
  sidebarContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    overflow: 'hidden',
    color: colors.primary,
    zIndex: 100,
  },
  notebookOverrides: {
    top: '0px',
    height: '100%'
  },
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    background: colorWithWhiteness(colors.primary, .87),
    boxShadow: `-10px 0px 10px -8px ${colorWithWhiteness(colors.dark, .5)}`,
  },
  iconContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    minHeight: 'calc(100vh - 156px)',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 101
  },
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    verticalAlign: 'middle'
  },
  asyncOperationStatusIcon: {
    width: '.5rem',
    height: '.5rem',
    zIndex: 2,
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.25rem',
  },
  statusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .1rem .1rem auto'
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  },
  sectionTitle: {
    marginTop: '0.5rem',
    fontWeight: 600,
    color: colors.primary
  },
  contentItem: {
    marginTop: 0,
    color: colors.primary
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    padding: '1.25rem 0.75rem',
    background: colorWithWhiteness(colors.primary, .8),
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'none'
  },
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: colors.primary,
    fontWeight: 600,
    paddingLeft: 12,
    width: 160
  },
  criteriaCount: {
    position: 'absolute',
    height: '0.8rem',
    width: '0.8rem',
    top: '1rem',
    left: '0.55rem',
    textAlign: 'center',
    backgroundColor: colors.danger,
    borderRadius: '50%',
    display: 'inline-block',
    fontSize: '0.4rem'
  }
});

const iconStyles = {
  active: {
    ...styles.icon,
    background: colorWithWhiteness(colors.primary, 0.55)
  },
  disabled: {
    ...styles.icon,
    cursor: 'not-allowed'
  },
  menu: {
    ...styles.icon,
    margin: '0.5rem auto 1.5rem',
    height: 27,
    width: 27
  }
};

export const NOTEBOOK_PAGE_KEY = 'notebook';

interface IconConfig {
  id: string;
  disabled: boolean;
  faIcon: IconDefinition;
  label: string;
  showIcon: () => boolean;
  style: CSSProperties;
  tooltip: string;
}

const pageKeyToAnalyticsLabels = {
  about: 'About Page',
  cohortBuilder: 'Cohort Builder',
  conceptSets: 'Concept Set',
  searchConceptSets: 'Concept Set',
  conceptSetActions: 'Concept Set',
  data: 'Data Landing Page',
  datasetBuilder: 'Dataset Builder',
  notebooks: 'Analysis Tab Landing Page',
  reviewParticipants: 'Review Participant List',
  reviewParticipantDetail: 'Review Individual',
};

interface Props {
  deleteFunction: Function;
  pageKey: string;
  profileState: any;
  shareFunction: Function;
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  runtimeStore: RuntimeStore;
  compoundRuntimeOps: CompoundRuntimeOpStore;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  genomicExtraction: GenomicExtractionStore;
}

interface State {
  activeIcon: string;
  filteredContent: Array<any>;
  participant: ParticipantCohortStatus;
  searchTerm: string;
  showCriteria: boolean;
  tooltipId: number;
}

export const HelpSidebar = fp.flow(
  withCurrentCohortCriteria(),
  withCurrentConcept(),
  withCurrentWorkspace(),
  withRuntimeStore(),
  withStore(compoundRuntimeOpStore, 'compoundRuntimeOps'),
  withStore(genomicExtractionStore, 'genomicExtraction'),
  withUserProfile(),
  withCdrVersions()
)(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    private loadLastSavedKey: () => void;
    constructor(props: Props) {
      super(props);
      this.state = {
        activeIcon: null,
        filteredContent: undefined,
        participant: undefined,
        searchTerm: '',
        showCriteria: false,
        tooltipId: undefined
      };
    }

    iconConfig(iconKey): IconConfig {
      return {
        'criteria': {
          id: 'criteria',
          disabled: false,
          faIcon: faInbox,
          label: 'Selected Criteria',
          showIcon: () => this.props.pageKey === 'cohortBuilder' && !!this.props.criteria,
          style: {fontSize: '21px'},
          tooltip: 'Selected Criteria',
        },
        'concept': {
          id: 'concept',
          disabled: false,
          faIcon: faInbox,
          label: 'Selected Concepts',
          showIcon: () => this.props.pageKey === 'conceptSets',
          style: {fontSize: '21px'},
          tooltip: 'Selected Concepts',
        },
        'help': {
          id: 'help',
          disabled: false,
          faIcon: faInfoCircle,
          label: 'Help Icon',
          showIcon: () => true,
          style: {fontSize: '21px'},
          tooltip: 'Help Tips',
        },
        'notebooksHelp': {
          id: 'notebooksHelp',
          disabled: false,
          faIcon: faFolderOpen,
          label: 'Storage Icon',
          showIcon: () => true,
          style: {fontSize: '21px'},
          tooltip: 'Workspace Storage',
        },
        'dataDictionary': {
          id: 'dataDictionary',
          disabled: false,
          faIcon: faBook,
          label: 'Data Dictionary Icon',
          showIcon: () => true,
          style: {color: colors.white, fontSize: '20px', marginTop: '5px'},
          tooltip: 'Data Dictionary',
        },
        'annotations': {
          id: 'annotations',
          disabled: false,
          faIcon: faEdit,
          label: 'Annotations Icon',
          showIcon: () => this.props.pageKey === 'reviewParticipantDetail',
          style: {fontSize: '20px', marginLeft: '3px'},
          tooltip: 'Annotations',
        },
        'runtime': {
          id: 'runtime',
          disabled: false,
          faIcon: null,
          label: 'Cloud Icon',
          showIcon: () => true,
          style: {height: '22px', width: '22px'},
          tooltip: 'Compute Configuration'
        },
        'genomicExtractions': {
          id: 'genomicExtractions',
          disabled: false,
          faIcon: faDna,
          label: 'Genomic Extraction',
          showIcon: () => true,
          // position: absolute is so the status icon won't push the DNA icon to the left.
          style: {height: '22px', width: '22px', marginTop: '0.25rem', position: 'absolute'},
          tooltip: 'Genomic Extraction History',
        }
      }[iconKey];
    }

    icons(): IconConfig[] {
      const keys = [
        'criteria',
        'concept',
        'help',
        'notebooksHelp',
        'dataDictionary',
        'annotations'
      ].filter(key => this.iconConfig(key).showIcon());

      if (WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel)) {
        keys.push('runtime');
      }

      if (serverConfigStore.get().config.enableGenomicExtraction &&
          getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasWgsData) {
        keys.push('genomicExtractions');
      }

      return keys.map(k => this.iconConfig(k));
    }

    setActiveIcon(activeIcon: string) {
      this.setState({activeIcon});
      if (activeIcon) {
        localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
      } else {
        localStorage.removeItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE);
      }
    }

    async componentDidMount() {
      const lastSavedKey = localStorage.getItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE);

      // This is a little hacky but it's necessary because
      // 1. the pageKey is needed to know which icons are available on this page but it's not always available on mount
      // 2. router events (which usually close the sidebar panel) during initial page load will update the activeIcon
      //    and overwrite the value stored in localStorage key so we need to "save" it here
      // I'd like to clean this up but I think it'll have to wait until the router migration is complete.
      this.loadLastSavedKey = (() => {
        let loadedLastSavedKey = false;
        return () => {
          if (!loadedLastSavedKey && this.props.pageKey) {
            const iconConfig = this.icons().find(icon => icon.id === lastSavedKey);
            setSidebarActiveIconStore.next(iconConfig ? iconConfig.id : null);
            loadedLastSavedKey = true;
          }
        };
      })();

      this.loadLastSavedKey();
      this.subscription = participantStore.subscribe(participant => this.setState({participant}));
      this.subscription.add(setSidebarActiveIconStore.subscribe(activeIcon => {this.setActiveIcon(activeIcon); }));
      this.subscription.add(routeDataStore.subscribe((newRoute, oldRoute) => {
        if (!fp.isEmpty(oldRoute) && !fp.isEqual(newRoute, oldRoute)) {
          this.setActiveIcon(null);
        }
      }));

      if (serverConfigStore.get().config.enableGenomicExtraction &&
          getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasWgsData &&
          !genomicExtractionStore.get()[this.props.workspace.namespace]
      ) {
        const genomicExtractionList = await dataSetApi().getGenomicExtractionJobs(this.props.workspace.namespace, this.props.workspace.id);
        updateGenomicExtractionStore(this.props.workspace.namespace, genomicExtractionList.jobs);
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      this.loadLastSavedKey();
      if ((!this.props.criteria && !!prevProps.criteria ) || (!this.props.concept && !!prevProps.concept)) {
        this.setActiveIcon(null);
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

    onIconClick(icon: IconConfig) {
      const {activeIcon} = this.state;
      const {id: clickedActiveIconId, label} = icon;

      if (activeIcon === clickedActiveIconId) {
        this.setActiveIcon(null);
      } else {
        this.analyticsEvent('OpenSidebar', `Sidebar - ${label}`);
        this.setActiveIcon(clickedActiveIconId);
      }
    }

    openContactWidget() {
      const {profileState: {profile: {contactEmail, familyName, givenName, username}}} = this.props;
      this.analyticsEvent('ContactUs');
      openZendeskWidget(givenName, familyName, username, contactEmail);
    }

    analyticsEvent(type: string, label?: string) {
      const {pageKey} = this.props;
      const analyticsLabel = pageKeyToAnalyticsLabels[pageKey];
      if (analyticsLabel) {
        const eventLabel = label ? `${label} - ${analyticsLabel}` : analyticsLabel;
        AnalyticsTracker.Sidebar[type](eventLabel);
      }
    }

    sidebarContainerStyles(activeIcon) {
      return {
        ...styles.sidebarContainer,
        width: activeIcon ? `calc(${this.sidebarWidth}rem + 70px)` : 0, // +70px accounts for the width of the icon sidebar + box shadow
        ...(this.props.pageKey === NOTEBOOK_PAGE_KEY ? styles.notebookOverrides : {})
      };
    }

    renderWorkspaceMenu() {
      const {deleteFunction, shareFunction, workspace, workspace: {accessLevel, id, namespace}} = this.props;
      const isNotOwner = !workspace || accessLevel !== WorkspaceAccessLevel.OWNER;
      const tooltip = isNotOwner && 'Requires owner permission';
      return <PopupTrigger
        side='bottom'
        closeOnClick
        content={
          <React.Fragment>
            <div style={styles.dropdownHeader}>Workspace Actions</div>
            <MenuItem
              icon='copy'
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenDuplicatePage();
                NavStore.navigate(['/workspaces', namespace, id, 'duplicate']);
              }}>
              Duplicate
            </MenuItem>
            <MenuItem
              icon='pencil'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenEditPage();
                NavStore.navigate(['/workspaces', namespace, id, 'edit']);
              }}>
              Edit
            </MenuItem>
            <MenuItem
              icon='share'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenShareModal();
                shareFunction();
              }}>
              Share
            </MenuItem>
            <MenuItem
              icon='trash'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenDeleteModal();
                deleteFunction();
              }}>
              Delete
            </MenuItem>
          </React.Fragment>
        }>
        <div data-test-id='workspace-menu-button'>
          <TooltipTrigger content={<div>Menu</div>} side='left'>
            <div style={styles.icon} onClick={() => this.analyticsEvent('OpenSidebar', 'Sidebar - Menu Icon')}>
              <FontAwesomeIcon icon={faEllipsisV} style={{fontSize: '21px'}}/>
            </div>
          </TooltipTrigger>
        </div>
      </PopupTrigger>;
    }

    displayFontAwesomeIcon(icon: IconConfig) {
      const {concept, criteria} = this.props;

      return <React.Fragment>
        {(icon.id === 'criteria' && criteria && criteria.length > 0) && <span data-test-id='criteria-count'
                                                         style={styles.criteriaCount}>
          {criteria.length}</span>}
        {(icon.id === 'concept' && concept && concept.length > 0) && <span data-test-id='concept-count'
                                                         style={styles.criteriaCount}>
          {concept.length}</span>}
            <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
          </React.Fragment> ;
    }

    displayRuntimeIcon(icon: IconConfig) {
      const {runtimeStore, compoundRuntimeOps, workspace} = this.props;
      let status = runtimeStore && runtimeStore.runtime && runtimeStore.runtime.status;
      if ((!status || status === RuntimeStatus.Deleted) &&
          workspace.namespace in compoundRuntimeOps) {
        // If a compound operation is still pending, and we're transitioning
        // through the "Deleted" phase of the runtime, we want to keep showing
        // an activity spinner. Avoids an awkward UX during a delete/create cycle.
        // There also be some lag during the runtime creation flow between when
        // the compound operation starts, and the runtime is set in the store; for
        // this reason use Creating rather than Deleting here.
        status = RuntimeStatus.Creating;
      }

      // We always want to show the thunderstorm icon.
      // For most runtime statuses (Deleting and Unknown currently excepted), we will show a small
      // overlay icon in the bottom right of the tab showing the runtime status.
      return <FlexRow style={{height: '100%', alignItems: 'center', justifyContent: 'space-around'}}>
        <img data-test-id={'help-sidebar-icon-' + icon.id} src={proIcons[icon.id]} style={{...icon.style, position: 'absolute'}} />
        <FlexRow data-test-id='runtime-status-icon-container' style={styles.statusIconContainer}>
          {(status === RuntimeStatus.Creating
          || status === RuntimeStatus.Starting
          || status === RuntimeStatus.Updating)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.asyncOperationStatusIcon,
              ...styles.rotate,
              color: colors.asyncOperationStatus.starting,
            }}/>
          }
          {status === RuntimeStatus.Stopped
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.asyncOperationStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.asyncOperationStatus.stopped,
            }}/>
          }
          {status === RuntimeStatus.Running
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.asyncOperationStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.asyncOperationStatus.running,
            }}/>
          }
          {(status === RuntimeStatus.Stopping
          || status === RuntimeStatus.Deleting)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.asyncOperationStatusIcon,
              ...styles.rotate,
              color: colors.asyncOperationStatus.stopping,
            }}/>
          }
          {status === RuntimeStatus.Error
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.asyncOperationStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.asyncOperationStatus.error,
            }}/>
          }
        </FlexRow>
      </FlexRow>;
    }

    withinPastTwentyFourHours(epoch) {
      const completionTimeMoment = moment(epoch);
      const twentyFourHoursAgo = moment().subtract(1, 'days');
      return completionTimeMoment.isAfter(twentyFourHoursAgo);
    }

    displayExtractionIcon(icon: IconConfig) {
      const extractionsByWorkspace = this.props.genomicExtraction;
      const extractionJobs = extractionsByWorkspace[this.props.workspace.namespace];
      const jobsByStatus = fp.groupBy('status', extractionJobs);
      let status;
      // If any jobs are currently active, show the 'sync' icon corresponding to their status.
      if (jobsByStatus[TerraJobStatus.RUNNING]) {
        status = TerraJobStatus.RUNNING;
      } else if (jobsByStatus[TerraJobStatus.ABORTING]) {
        status = TerraJobStatus.ABORTING;
      } else if (
          jobsByStatus[TerraJobStatus.SUCCEEDED]
          || jobsByStatus[TerraJobStatus.FAILED]
          || jobsByStatus[TerraJobStatus.ABORTED]
      ) {
        // Otherwise, show the status of the most recent completed job, if it was completed within the past 24h.
        const completedJobs = fp.flatten([
            jobsByStatus[TerraJobStatus.SUCCEEDED] || [],
            jobsByStatus[TerraJobStatus.FAILED] || [],
            jobsByStatus[TerraJobStatus.ABORTED] || []
        ]);
        const mostRecentCompletedJob = fp.flow(
          fp.filter((job: GenomicExtractionJob) => this.withinPastTwentyFourHours(job.completionTime)),
          // This could be phrased as fp.sortBy('completionTime') but it confuses the compile time type checker
          fp.sortBy(job => job.completionTime),
          fp.reverse,
          fp.head
        )(completedJobs);
        if (mostRecentCompletedJob) {
          status = mostRecentCompletedJob.status;
        }
      }

      // We always want to show the DNA icon.
      // When there are running or recently completed  jobs, we will show a small overlay icon in
      // the bottom right of the tab showing the job status.
      return <FlexRow style={{height: '100%', alignItems: 'center', justifyContent: 'space-around'}}>
        {this.displayFontAwesomeIcon(icon)}
        <FlexRow data-test-id='extraction-status-icon-container' style={styles.statusIconContainer}>
          {
            switchCase(status,
              [TerraJobStatus.RUNNING, () => <FontAwesomeIcon icon={faSyncAlt} style={{
                ...styles.asyncOperationStatusIcon,
                ...styles.rotate,
                color: colors.asyncOperationStatus.starting,
              }}/>],
              [TerraJobStatus.ABORTING, () => <FontAwesomeIcon icon={faSyncAlt} style={{
                ...styles.asyncOperationStatusIcon,
                ...styles.rotate,
                color: colors.asyncOperationStatus.stopping,
              }}/>],
              [TerraJobStatus.FAILED, () => <FontAwesomeIcon icon={faCircle} style={{
                ...styles.asyncOperationStatusIcon,
                color: colors.asyncOperationStatus.error,
              }}/>],
              [TerraJobStatus.SUCCEEDED, () => <FontAwesomeIcon icon={faCircle} style={{
                ...styles.asyncOperationStatusIcon,
                color: colors.asyncOperationStatus.succeeded,
              }}/>],
              [TerraJobStatus.ABORTED, () => <FontAwesomeIcon icon={faCircle} style={{
                ...styles.asyncOperationStatusIcon,
                color: colors.asyncOperationStatus.stopped,
              }}/>]
            )
          }
        </FlexRow>
      </FlexRow>;
    }

    get sidebarStyle() {
      return {
        ...styles.sidebar,
        width: `${this.sidebarWidth}.5rem`,
      };
    }

    get sidebarWidth() {
      return fp.getOr('14', 'bodyWidthRem', this.sidebarContent(this.state.activeIcon));
    }

    sidebarContent(activeIcon): {
      overflow?: string;
      headerPadding?: string;
      renderHeader?: () => JSX.Element;
      bodyWidthRem?: string;
      bodyPadding?: string;
      renderBody: () => JSX.Element;
      showFooter: boolean;
    } {
      switch (activeIcon) {
        case 'help':
          return {
            headerPadding: '0.5rem',
            renderHeader: () =>
              <h3 style={{...styles.sectionTitle, marginTop: 0, lineHeight: 1.75}}>
                Help Tips
              </h3>,
            renderBody: () =>
              <HelpTips allowSearch={true}
                        onSearch={() => this.analyticsEvent('Search')}
                        pageKey={this.props.pageKey}/>,
            showFooter: true
          };
        case 'runtime':
          return {
            headerPadding: '0.75rem',
            renderHeader: () =>
              <div>
                <h3 style={{...styles.sectionTitle, marginTop: 0, lineHeight: 1.75}}>Cloud analysis environment</h3>
                <div style={{padding: '0.5rem 1rem'}}>
                  Your analysis environment consists of an application and compute resources.
                  Your cloud environment is unique to this workspace and not shared with other users.
                </div>
              </div>,
            bodyWidthRem: '30',
            bodyPadding: '0 1.25rem',
            renderBody: () =>
             <RuntimePanel onClose={() => this.setActiveIcon(null)}/>,
            showFooter: false
          };
        case 'notebooksHelp':
          return {
            headerPadding: '0.5rem',
            renderHeader: () =>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>
                Workspace storage
              </h3>,
            renderBody: () =>
              <HelpTips allowSearch={false}
                        pageKey='notebook'/>,
            showFooter: true
          };
        case 'annotations':
          return {
            headerPadding: '0.5rem 0.5rem 0 0.5rem',
            renderHeader: () => this.state.participant &&
              <div style={{fontSize: 18, color: colors.primary}}>
                {'Participant ' + this.state.participant.participantId}
              </div>
            ,
            renderBody: () => this.state.participant ?
               <SidebarContent/> : <Spinner style={{display: 'block', margin: '3rem auto'}}/>,
            showFooter: true
          };
        case 'concept':
          return {
            headerPadding: '0.75rem',
            renderHeader: () =>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>
                Selected Concepts
              </h3>,
            bodyWidthRem: '20',
            bodyPadding: '0.75rem 0.75rem 0',
            renderBody: () => !!currentConceptStore.getValue() &&
                <ConceptListPage/>,
            showFooter: false
          };
        case 'criteria':
          return {
            bodyWidthRem: '20',
            bodyPadding: '0.75rem 0.75rem 0',
            renderBody: () => !!currentCohortSearchContextStore.getValue() &&
                <SelectionList back={() => this.setActiveIcon(null)} selections={[]}/>,
            showFooter: false
          };
        case 'genomicExtractions':
          return {
            overflow: 'visible',
            headerPadding: '0.75rem',
            renderHeader: () =>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>
                Genomic Extractions
              </h3>,
            bodyWidthRem: '30',
            renderBody: () => <GenomicsExtractionTable />,
            showFooter: false
          };
      }
    }

    render() {
      const {activeIcon} = this.state;
      const sidebarContent = this.sidebarContent(activeIcon);
      const shouldRenderWorkspaceMenu = !this.iconConfig('concept').showIcon() && !this.iconConfig('criteria').showIcon();

      return <div id='help-sidebar'>
        <div style={{...styles.iconContainer, ...(this.props.pageKey === NOTEBOOK_PAGE_KEY ? styles.notebookOverrides : {})}}>
          {shouldRenderWorkspaceMenu && this.renderWorkspaceMenu()}
          {this.icons().map((icon, i) =>
              <div key={i} style={{display: 'table'}}>
                <TooltipTrigger content={<div>{icon.tooltip}</div>} side='left'>
                  <div style={activeIcon === icon.id ? iconStyles.active : icon.disabled ? iconStyles.disabled : styles.icon}
                       onClick={() => {
                         if (icon.id !== 'dataDictionary' && !icon.disabled) {
                           this.onIconClick(icon);
                         }
                       }}>
                    {
                      switchCase(icon.id,
                        ['dataDictionary',
                          () => <a href={supportUrls.dataDictionary} target='_blank'>
                              <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
                            </a>
                        ],
                        ['runtime', () => this.displayRuntimeIcon(icon)],
                        ['genomicExtractions', () => this.displayExtractionIcon(icon)],
                        [DEFAULT, () => icon.faIcon === null
                              ? <img data-test-id={'help-sidebar-icon-' + icon.id} src={proIcons[icon.id]} style={icon.style} />
                              : this.displayFontAwesomeIcon(icon)
                        ]
                      )
                    }
                  </div>
                </TooltipTrigger>
              </div>
            )
          }
        </div>

        <TransitionGroup>
          <CSSTransition
            key={activeIcon}
            classNames='sidebar'
            addEndListener={(node, done) => {
              node.addEventListener('transitionend', (e) => {
                if (node.isEqualNode(e.target)) {
                  done(e);
                }
              }, false);
            }}>
            <div style={this.sidebarContainerStyles(activeIcon)}>
              <div style={this.sidebarStyle} data-test-id='sidebar-content'>
                {activeIcon && sidebarContent &&
                <div style={{height: '100%', overflow: sidebarContent.overflow || 'auto'}}>
                  <FlexColumn style={{height: '100%'}}>
                    {sidebarContent.renderHeader &&
                    <FlexRow style={{justifyContent: 'space-between', padding: sidebarContent.headerPadding}}>
                      {sidebarContent.renderHeader()}

                      <Clickable style={{marginLeft: 'auto'}} onClick={() => this.setActiveIcon(null)}>
                        <img src={proIcons.times}
                             style={{height: '27px', width: '17px'}}
                             alt='Close'/>
                      </Clickable>
                    </FlexRow>}

                    <div className='slim-scroll-bar' style={{flex: 1, padding: sidebarContent.bodyPadding || '0 0.5rem 5.5rem'}}>
                      {sidebarContent.renderBody()}
                    </div>
                  </FlexColumn>

                  {sidebarContent.showFooter &&
                  <div style={{...styles.footer}}>
                      <h3 style={{...styles.sectionTitle, marginTop: 0}}>Not finding what you're looking for?</h3>
                      <p style={styles.contentItem}>
                          Visit our <StyledAnchorTag href={supportUrls.helpCenter}
                                                     target='_blank' onClick={() => this.analyticsEvent('UserSupport')}> User Support Hub
                      </StyledAnchorTag> page or <span style={styles.link} onClick={() => this.openContactWidget()}> contact us</span>.
                      </p>
                  </div>}
                </div>}
              </div>
            </div>
          </CSSTransition>
        </TransitionGroup>
      </div>;
    }
  }
);

@Component({
  selector: 'app-help-sidebar',
  template: '<div #root></div>',
})
export class HelpSidebarComponent extends ReactWrapperBase {
  @Input('deleteFunction') deleteFunction: Props['deleteFunction'];
  @Input('pageKey') pageKey: Props['pageKey'];
  @Input('shareFunction') shareFunction: Props['shareFunction'];

  constructor() {
    super(HelpSidebar, ['deleteFunction', 'pageKey', 'shareFunction']);
  }
}
