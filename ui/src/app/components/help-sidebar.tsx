import {workspace} from '@angular-devkit/core/src/experimental';
import {Component, Input} from '@angular/core';
import {faEdit} from '@fortawesome/free-regular-svg-icons';
import {
  faBook, faCheckCircle, faEllipsisH,
  faEllipsisV, faExclamationTriangle,
  faFolderOpen,
  faInbox,
  faInfoCircle
} from '@fortawesome/free-solid-svg-icons';
import {faDna} from '@fortawesome/free-solid-svg-icons/faDna';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';
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
  reactStyles,
  ReactWrapperBase,
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
  compoundRuntimeOpStore,
  RuntimeStore,
  withStore
} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';

import {HelpTips} from 'app/components/help-tips';
import {Criteria, ParticipantCohortStatus, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {cohortsApi, dataSetApi} from '../services/swagger-fetch-clients';
import {Clickable, MenuItem, StyledAnchorTag} from './buttons';

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
    transition: 'width 0.5s ease-out',
    overflow: 'hidden',
    color: colors.primary,
    zIndex: -1,
  },
  notebookOverrides: {
    top: '0px',
    height: '100%'
  },
  sidebarContainerActive: {
    zIndex: 100,
  },
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    background: colorWithWhiteness(colors.primary, .87),
    transition: 'margin-right 0.5s ease-out',
    boxShadow: `-10px 0px 10px -8px ${colorWithWhiteness(colors.dark, .5)}`,
  },
  sidebarOpen: {
    marginRight: 0,
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
    transition: 'background 0.2s linear',
    verticalAlign: 'middle'
  },
  runtimeStatusIcon: {
    width: '.5rem',
    height: '.5rem'
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.25rem',
  },
  runtimeStatusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .1rem .1rem auto'
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  },
  navIcons: {
    position: 'absolute',
    right: '0',
    top: '0.75rem',
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
  menuButtonIcon: {
    width: 27,
    height: 27,
    opacity: 0.65,
    marginRight: 16
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
  },
  buttons: {
    paddingTop: '1rem',
    paddingLeft: '9rem',
    position: 'absolute'
  },
  backButton: {
    border: '0px',
    backgroundColor: 'none',
    color: colors.accent,
    marginRight: '1rem'
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

export const NOTEBOOK_HELP_CONTENT = 'notebookStorage';

const iconConfigs = {
  'criteria': {
    disabled: false,
    faIcon: faInbox,
    label: 'Selected Criteria',
    page: 'criteria',
    style: {fontSize: '21px'},
    tooltip: 'Selected Criteria',
  },
  'concept': {
    disabled: false,
    faIcon: faInbox,
    label: 'Selected Concepts',
    page: 'concept',
    style: {fontSize: '21px'},
    tooltip: 'Selected Concepts',
  },
  'help': {
    disabled: false,
    faIcon: faInfoCircle,
    label: 'Help Icon',
    page: null,
    style: {fontSize: '21px'},
    tooltip: 'Help Tips',
  },
  'notebooksHelp': {
    disabled: false,
    faIcon: faFolderOpen,
    label: 'Storage Icon',
    page: null,
    style: {fontSize: '21px'},
    tooltip: 'Workspace Storage',
  },
  'dataDictionary': {
    disabled: false,
    faIcon: faBook,
    label: 'Data Dictionary Icon',
    page: null,
    style: {color: colors.white, fontSize: '20px', marginTop: '5px'},
    tooltip: 'Data Dictionary',
  },
  'annotations': {
    disabled: false,
    faIcon: faEdit,
    label: 'Annotations Icon',
    page: 'reviewParticipantDetail',
    style: {fontSize: '20px', marginLeft: '3px'},
    tooltip: 'Annotations',
  },
  'runtime': {
    disabled: false,
    faIcon: null,
    label: 'Cloud Icon',
    page: null,
    style: {height: '22px', width: '22px'},
    tooltip: 'Compute Configuration'
  },
  'genomicExtractions': {
    disabled: false,
    faIcon: faDna,
    label: '', // TODO eric: where is this used?
    page: null,
    style: {height: '22px', width: '22px', marginTop: '0.25rem'},
    tooltip: 'Genomes Extraction History',
  }
};

const helpIconName = (helpContentKey: string) => {
  return helpContentKey === NOTEBOOK_HELP_CONTENT ? 'notebooksHelp' : 'help';
};

const icons = (
  helpContentKey: string,
  workspaceAccessLevel: WorkspaceAccessLevel
) => {
  const keys = [
    'criteria',
    'concept',
    helpIconName(helpContentKey),
    'dataDictionary',
    'annotations'
  ];

  if (WorkspacePermissionsUtil.canWrite(workspaceAccessLevel)) {
    keys.push('runtime');
  }

  keys.push('genomicExtractions');

  return keys.map(k => ({...iconConfigs[k], id: k}));
};

const analyticsLabels = {
  about: 'About Page',
  cohortBuilder: 'Cohort Builder',
  conceptSets: 'Concept Set',
  data: 'Data Landing Page',
  datasetBuilder: 'Dataset Builder',
  notebooks: 'Analysis Tab Landing Page',
  reviewParticipants: 'Review Participant List',
  reviewParticipantDetail: 'Review Individual',
};

interface Props {
  deleteFunction: Function;
  helpContentKey: string;
  profileState: any;
  setSidebarState: Function;
  shareFunction: Function;
  sidebarOpen: boolean;
  notebookStyles: boolean;
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  runtimeStore: RuntimeStore;
  compoundRuntimeOps: CompoundRuntimeOpStore;
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
  withUserProfile()
)(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        // TODO(RW-5607): Remember which icon was active.
        activeIcon: props.sidebarOpen ? helpIconName(props.helpContentKey) : undefined,
        filteredContent: undefined,
        participant: undefined,
        searchTerm: '',
        showCriteria: false,
        tooltipId: undefined
      };
    }

    async componentDidMount() {
      this.subscription = participantStore.subscribe(participant => this.setState({participant}));
      this.subscription.add(setSidebarActiveIconStore.subscribe(activeIcon => {
        if (activeIcon !== null) {
          this.setState({activeIcon});
          this.props.setSidebarState(!!activeIcon);
        }
      }));
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      // close the sidebar on each navigation excluding navigating between participants in cohort review
      if (!this.props.sidebarOpen && prevProps.sidebarOpen) {
        setTimeout(() => {
          // check if the sidebar has been opened again before resetting activeIcon
          if (!this.props.sidebarOpen) {
            this.setState({activeIcon: undefined});
          }
        }, 300);
      }
      if ((!this.props.criteria && !!prevProps.criteria ) || (!this.props.concept && !!prevProps.concept)) {
        this.props.setSidebarState(false);
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

    onIconClick(icon: any) {
      const {setSidebarState, sidebarOpen} = this.props;
      const {activeIcon} = this.state;
      const {id, label} = icon;
      const newSidebarOpen = !(id === activeIcon && sidebarOpen);
      if (newSidebarOpen) {
        this.setState({activeIcon: id});
        setSidebarState(true);
        this.analyticsEvent('OpenSidebar', `Sidebar - ${label}`);
      } else {
        setSidebarState(false);
      }
    }

    openContactWidget() {
      const {profileState: {profile: {contactEmail, familyName, givenName, username}}} = this.props;
      this.analyticsEvent('ContactUs');
      openZendeskWidget(givenName, familyName, username, contactEmail);
    }

    analyticsEvent(type: string, label?: string) {
      const {helpContentKey} = this.props;
      const analyticsLabel = analyticsLabels[helpContentKey];
      if (analyticsLabel) {
        const eventLabel = label ? `${label} - ${analyticsLabel}` : analyticsLabel;
        AnalyticsTracker.Sidebar[type](eventLabel);
      }
    }

    sidebarContainerStyles(activeIcon, notebookStyles) {
      const sidebarContainerStyle = {
        ...styles.sidebarContainer,
        width: `calc(${this.sidebarWidth}rem + 70px)` // +70px accounts for the width of the icon sidebar + box shadow
      };
      if (notebookStyles) {
        if (activeIcon) {
          return {...sidebarContainerStyle, ...styles.notebookOverrides, ...styles.sidebarContainerActive};
        } else {
          return {...sidebarContainerStyle, ...styles.notebookOverrides};
        }
      } else {
        if (activeIcon) {
          return {...sidebarContainerStyle, ...styles.sidebarContainerActive};
        } else {
          return sidebarContainerStyle;
        }
      }
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

    showIcon(icon) {
      const {concept, criteria, helpContentKey} = this.props;
      return !icon.page || icon.page === helpContentKey || (criteria && icon.page === 'criteria') || (concept && icon.page === 'concept');
    }

    displayFontAwesomeIcon(icon) {
      const {concept, criteria} = this.props;

      return <React.Fragment>
        {(criteria && icon.page === 'criteria' && criteria.length > 0) && <span data-test-id='criteria-count'
                                                         style={styles.criteriaCount}>
          {criteria.length}</span>}
        {(concept && icon.page === 'concept' && concept.length > 0) && <span data-test-id='concept-count'
                                                         style={styles.criteriaCount}>
          {concept.length}</span>}
            <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
          </React.Fragment> ;
    }

    displayRuntimeIcon(icon) {
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
        <FlexRow data-test-id='runtime-status-icon-container' style={styles.runtimeStatusIconContainer}>
          {(status === RuntimeStatus.Creating
          || status === RuntimeStatus.Starting
          || status === RuntimeStatus.Updating)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.runtimeStatusIcon,
              ...styles.rotate,
              color: colors.runtimeStatus.starting,
            }}/>
          }
          {status === RuntimeStatus.Stopped
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.stopped,
            }}/>
          }
          {status === RuntimeStatus.Running
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.running,
            }}/>
          }
          {(status === RuntimeStatus.Stopping
          || status === RuntimeStatus.Deleting)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.runtimeStatusIcon,
              ...styles.rotate,
              color: colors.runtimeStatus.stopping,
            }}/>
          }
          {status === RuntimeStatus.Error
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.error,
            }}/>
          }
        </FlexRow>
      </FlexRow>;
    }

    get sidebarStyle() {
      const sidebarStyle = {
        ...styles.sidebar,
        marginRight: `calc(-${this.sidebarWidth}rem - 40px)`,
        width: `${this.sidebarWidth}.5rem`,
      };
      return this.props.sidebarOpen ? {...sidebarStyle, ...styles.sidebarOpen} : sidebarStyle;
    }

    get sidebarWidth() {
      if (this.state.activeIcon && this.sidebarContent(this.state.activeIcon).bodyWidthRem) {
        return this.sidebarContent(this.state.activeIcon).bodyWidthRem;
      }
      return '14';
    }

    sidebarContent(activeIcon): {
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
                        contentKey={this.props.helpContentKey}/>,
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
              <RuntimePanel onClose={() => this.props.setSidebarState(false)}/>,
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
                        contentKey={this.props.helpContentKey}/>,
            showFooter: true
          };
        case 'annotations':
          return {
            headerPadding: '0.5rem 0.5rem 0 0.5rem',
            renderHeader: () =>
              <div style={{fontSize: 18, color: colors.primary}}>
                Participant {this.state.participant.participantId}
              </div>,
            renderBody: () => this.state.participant &&
                <SidebarContent/>,
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
                <SelectionList back={() => this.props.setSidebarState(false)} selections={[]}/>,
            showFooter: false
          };
        case 'genomicExtractions':
          return {
            headerPadding: '0.75rem',
            renderHeader: () =>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>
                Genomic Extractions
              </h3>,
            bodyWidthRem: '30',
            renderBody: () => {
              const tableData = [{
                datasetName: 'Name of a dataset',
                status: 0,
                statusJsx:  <FontAwesomeIcon icon={faSyncAlt} style={{color: colors.success, fontSize: '.7rem', marginLeft: '.4rem', display: 'block', animationName: 'spin', animationDuration: '5000ms', animationIterationCount: 'infinite', animationTimingFunction: 'linear'}}/>,
                dateStarted: 'Today at 7:12 am',
                cost: '',
                duration: '',
                menu:  <FontAwesomeIcon icon={faEllipsisV} style={{color: colors.accent, fontSize: '.7rem', marginLeft: 0, paddingRight: 0, display: 'block'}}/>,
              }, {
                datasetName: 'This_is_another_datasetssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss',
                status: 1,
                statusJsx: <FontAwesomeIcon icon={faExclamationTriangle} style={{color: colors.danger, fontSize: '.7rem', marginLeft: '.4rem', display: 'block'}}/>,
                dateStarted: 'Apr 4, 2021 at 8:15 am',
                cost: '$0.08',
                duration: '14 min',
                menu:  <FontAwesomeIcon icon={faEllipsisV} style={{color: colors.accent, fontSize: '.7rem', marginLeft: 0, paddingRight: 0, display: 'block'}}/>,
              }, {
                datasetName: 'Anotha one!',
                status: 2,
                statusJsx: <FontAwesomeIcon icon={faCheckCircle} style={{color: colors.success, fontSize: '.7rem', marginLeft: '.4rem', display: 'block'}}/>,
                dateStarted: 'Apr 1, 2021 at 8:15 am',
                cost: '$100.08',
                duration: '10h, 32min',
                menu:  <FontAwesomeIcon icon={faEllipsisV} style={{color: colors.accent, fontSize: '.7rem', marginLeft: 0, paddingRight: 0, display: 'block'}}/>,
              }];

              dataSetApi().getGenomicExtractionJobs(this.props.workspace.namespace, this.props.workspace.id)
                .then(jobs => {
                  console.log(jobs);
                });

              return <div id='extraction-data-table-container'>
                <DataTable value={tableData} autoLayout sortField='dateStarted' sortOrder={-1}>
                  <Column field='datasetName' header='Dataset Name' sortable style={{maxWidth: '8rem', textOverflow: 'ellipsis', overflow: 'hidden'}}/>
                  <Column field='statusJsx' header='Status' sortable sortField='status'/>
                  <Column field='dateStarted' header='Date Started' sortable/>
                  <Column field='cost' header='Cost' sortable/>
                  <Column field='duration' header='Duration' sortable/>
                  <Column field='menu' header=''/>
                </DataTable>
              </div>;
            },
            showFooter: false
      }
      }
    }

    render() {
      const {concept, criteria, helpContentKey, notebookStyles, workspace} = this.props;
      const {activeIcon, tooltipId} = this.state;
      const sidebarContent = this.sidebarContent(activeIcon);

      return <div id='help-sidebar'>
        <div style={notebookStyles ? {...styles.iconContainer, ...styles.notebookOverrides} : {...styles.iconContainer}}>
          {!(criteria || concept)  && this.renderWorkspaceMenu()}
          {icons(helpContentKey, workspace.accessLevel).map((icon, i) =>
          this.showIcon(icon) && <div key={i} style={{display: 'table'}}>
                <TooltipTrigger content={<div>{tooltipId === i && icon.tooltip}</div>} side='left'>
                  <div style={activeIcon === icon.id ? iconStyles.active : icon.disabled ? iconStyles.disabled : styles.icon}
                       onClick={() => {
                         if (icon.id !== 'dataDictionary' && !icon.disabled) {
                           this.onIconClick(icon);
                         }
                       }}
                       onMouseOver={() => this.setState({tooltipId: i})}
                       onMouseOut={() => this.setState({tooltipId: undefined})}>
                    {icon.id === 'dataDictionary'
                      ? <a href={supportUrls.dataDictionary} target='_blank'>
                          <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
                        </a>
                      : icon.id === 'runtime'
                        ? this.displayRuntimeIcon(icon)
                        : icon.faIcon === null
                          ? <img data-test-id={'help-sidebar-icon-' + icon.id} src={proIcons[icon.id]} style={icon.style} />
                          : this.displayFontAwesomeIcon(icon)
                    }
                  </div>
                </TooltipTrigger>
              </div>
            )
          }
        </div>
        <div style={this.sidebarContainerStyles(activeIcon, notebookStyles)}>
          <div style={this.sidebarStyle} data-test-id='sidebar-content'>

            {sidebarContent &&
              <div style={{height: '100%', overflow: 'auto'}}>
                <FlexColumn style={{height: '100%'}}>
                  {sidebarContent.renderHeader &&
                    <FlexRow style={{justifyContent: 'space-between', padding: sidebarContent.headerPadding}}>
                      {sidebarContent.renderHeader()}

                      <Clickable onClick={() => this.props.setSidebarState(false)}>
                          <img src={proIcons.times}
                               style={{height: '27px', width: '17px'}}
                               alt='Close'/>
                      </Clickable>
                    </FlexRow>
                  }

                  <div style={{flex: 1, padding: sidebarContent.bodyPadding || '0 0.5rem 5.5rem', height: '100%'}}>
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
                  </div>
                }
              </div>
            }
          </div>
        </div>
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
  @Input('helpContentKey') helpContentKey: Props['helpContentKey'];
  @Input('setSidebarState') setSidebarState: Props['setSidebarState'];
  @Input('shareFunction') shareFunction: Props['shareFunction'];
  @Input('sidebarOpen') sidebarOpen: Props['sidebarOpen'];
  @Input('notebookStyles') notebookStyles: Props['notebookStyles'];
  constructor() {
    super(HelpSidebar, ['deleteFunction', 'helpContentKey', 'setSidebarState', 'shareFunction', 'sidebarOpen', 'notebookStyles']);
  }
}
