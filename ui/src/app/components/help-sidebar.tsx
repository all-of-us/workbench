import * as React from 'react';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import * as fp from 'lodash/fp';
import { faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  CdrVersionTiersResponse,
  Criteria,
  GenomicExtractionJob,
  ParticipantCohortStatus,
  ResourceType,
  RuntimeError,
  RuntimeStatus,
} from 'generated/fetch';

import { SelectionList } from 'app/cohort-search/selection-list/selection-list.component';
import { AppsPanel } from 'app/components/apps-panel';
import { CloseButton, StyledExternalLink } from 'app/components/buttons';
import { ConfirmDeleteModal } from 'app/components/confirm-delete-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { GenomicsExtractionTable } from 'app/components/genomics-extraction-table';
import {
  HelpSidebarIcons,
  IconConfig,
  showConceptIcon,
  showCriteriaIcon,
  SidebarIconId,
} from 'app/components/help-sidebar-icons';
import { HelpTips } from 'app/components/help-tips';
import { withErrorModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { PopupTrigger } from 'app/components/popups';
import { RuntimeConfigurationPanel } from 'app/components/runtime-configuration-panel';
import { RuntimeErrorModal } from 'app/components/runtime-error-modal';
import { Spinner } from 'app/components/spinners';
import { SidebarContent } from 'app/pages/data/cohort-review/sidebar-content.component';
import { ConceptListPage } from 'app/pages/data/concept/concept-list';
import { WorkspaceActionsMenu } from 'app/pages/workspace/workspace-actions-menu';
import { WorkspaceShare } from 'app/pages/workspace/workspace-share';
import { participantStore } from 'app/services/review-state.service';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  switchCase,
  withCdrVersions,
  withCurrentCohortCriteria,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  currentConceptStore,
  NavigationProps,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { PanelContent } from 'app/utils/runtime-utils';
import {
  routeDataStore,
  runtimeStore,
  withGenomicExtractionJobs,
} from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { openZendeskWidget, supportUrls } from 'app/utils/zendesk';

export const LOCAL_STORAGE_KEY_SIDEBAR_STATE = 'WORKSPACE_SIDEBAR_STATE';

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
    height: '100%',
  },
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    background: colorWithWhiteness(colors.primary, 0.87),
    boxShadow: `-10px 0px 10px -8px ${colorWithWhiteness(colors.dark, 0.5)}`,
  },
  iconContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    minHeight: 'calc(100vh - 156px)',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 101,
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
    verticalAlign: 'middle',
  },
  sectionTitle: {
    marginTop: 0,
    fontWeight: 600,
    color: colors.primary,
  },
  contentItem: {
    marginTop: 0,
    color: colors.primary,
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    padding: '1.25rem 0.75rem',
    background: colorWithWhiteness(colors.primary, 0.8),
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'none',
  },
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: colors.primary,
    fontWeight: 600,
    paddingLeft: 12,
    width: 160,
  },
});

export const LEONARDO_APP_PAGE_KEY = 'leonardo_app';

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

interface Props extends NavigationProps {
  pageKey: string;
  profileState: any;
  shareFunction: Function;
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  genomicExtractionJobs: GenomicExtractionJob[];
  cohortContext: any;
}

enum CurrentModal {
  None,
  Share,
  Delete,
  HasRuntimeError,
}

interface State {
  activeIcon: SidebarIconId;
  filteredContent: Array<any>;
  participant: ParticipantCohortStatus;
  searchTerm: string;
  showCriteria: boolean;
  tooltipId: number;
  currentModal: CurrentModal;
  runtimeErrors: Array<RuntimeError>;
  runtimeConfPanelInitialState: PanelContent | null;
}

export const HelpSidebar = fp.flow(
  withCurrentCohortCriteria(),
  withCurrentCohortSearchContext(),
  withCurrentConcept(),
  withGenomicExtractionJobs,
  withCurrentWorkspace(),
  withUserProfile(),
  withCdrVersions(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        activeIcon: null,
        filteredContent: undefined,
        participant: undefined,
        searchTerm: '',
        showCriteria: false,
        tooltipId: undefined,
        currentModal: CurrentModal.None,
        runtimeErrors: null,
        runtimeConfPanelInitialState: null,
      };
    }

    subscriptions = [];

    deleteWorkspace = withErrorModal(
      {
        title: 'Error Deleting Workspace',
        message: `Could not delete workspace '${this.props.workspace.name}'.`,
        showBugReportLink: true,
        onDismiss: () => {
          this.setState({ currentModal: CurrentModal.None });
        },
      },
      async () => {
        AnalyticsTracker.Workspaces.Delete();
        await workspacesApi().deleteWorkspace(
          this.props.workspace.namespace,
          this.props.workspace.id
        );
        this.props.navigate(['workspaces']);
      }
    );

    setActiveIcon(activeIcon: SidebarIconId) {
      setSidebarActiveIconStore.next(activeIcon);
      // let the Runtime Config Panel use its own logic
      this.setState({ runtimeConfPanelInitialState: null });
    }

    openRuntimeConfigWithState(runtimeConfPanelInitialState: PanelContent) {
      setSidebarActiveIconStore.next('runtimeConfig');
      this.setState({ runtimeConfPanelInitialState });
    }

    async componentDidMount() {
      // This is being set here instead of the constructor to show the opening animation of the side panel and
      // indicate to the user that it's something they can close.
      this.setActiveIcon(
        localStorage.getItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE) as SidebarIconId
      );
      this.subscriptions.push(
        participantStore.subscribe((participant) =>
          this.setState({ participant })
        )
      );
      this.subscriptions.push(
        setSidebarActiveIconStore.subscribe((activeIcon) => {
          this.setState({ activeIcon });
          if (activeIcon) {
            localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
          } else {
            localStorage.removeItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE);
          }
        })
      );
      this.subscriptions.push(
        routeDataStore.subscribe((newRoute, oldRoute) => {
          if (!fp.isEmpty(oldRoute) && !fp.isEqual(newRoute, oldRoute)) {
            this.setActiveIcon(null);
          }
        })
      );
      this.subscriptions.push(
        runtimeStore.subscribe((newRuntime, oldRuntime) => {
          // If the runtime status has changed from something that is not Error to Error,
          // and we have error messages, show the error modal.
          if (
            newRuntime.runtimeLoaded &&
            newRuntime.workspaceNamespace === this.props.workspace.namespace &&
            newRuntime.runtime.status === RuntimeStatus.Error &&
            (!oldRuntime.runtime ||
              oldRuntime.runtime.status !== RuntimeStatus.Error) &&
            newRuntime.runtime.errors
          ) {
            this.setState({
              currentModal: CurrentModal.HasRuntimeError,
              runtimeErrors: newRuntime.runtime.errors,
            });
          }
        })
      );
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (
        (!this.props.criteria && !!prevProps.criteria) ||
        (!this.props.concept && !!prevProps.concept)
      ) {
        this.setActiveIcon(null);
      }
    }

    componentWillUnmount(): void {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
    }

    onIconClick(icon: IconConfig) {
      const { activeIcon } = this.state;
      const { id: clickedActiveIconId, label } = icon;

      if (activeIcon === clickedActiveIconId) {
        this.setActiveIcon(null);
      } else {
        this.analyticsEvent('OpenSidebar', `Sidebar - ${label}`);
        this.setActiveIcon(clickedActiveIconId);
      }
    }

    openContactWidget() {
      const {
        profileState: {
          profile: { contactEmail, familyName, givenName, username },
        },
      } = this.props;
      this.analyticsEvent('ContactUs');
      openZendeskWidget(givenName, familyName, username, contactEmail);
    }

    analyticsEvent(type: string, label?: string) {
      const { pageKey } = this.props;
      const analyticsLabel = pageKeyToAnalyticsLabels[pageKey];
      if (analyticsLabel) {
        const eventLabel = label
          ? `${label} - ${analyticsLabel}`
          : analyticsLabel;
        AnalyticsTracker.Sidebar[type](eventLabel);
      }
    }

    sidebarContainerStyles(activeIcon) {
      return {
        ...styles.sidebarContainer,
        width: activeIcon ? `calc(${this.sidebarWidth}rem + 70px)` : 0, // +70px accounts for the width of the icon sidebar + box shadow
        ...(this.props.pageKey === LEONARDO_APP_PAGE_KEY
          ? styles.notebookOverrides
          : {}),
      };
    }

    get sidebarStyle() {
      return {
        ...styles.sidebar,
        width: `${this.sidebarWidth}.5rem`,
      };
    }

    get sidebarWidth() {
      const { activeIcon } = this.state;
      return fp.getOr('14', 'bodyWidthRem', this.sidebarContent(activeIcon));
    }

    sidebarContent(
      activeIcon: SidebarIconId,
      runtimeConfPanelInitialState?: PanelContent
    ): {
      overflow?: string;
      headerPadding?: string;
      renderHeader?: () => JSX.Element;
      bodyWidthRem?: string;
      bodyPadding?: string;
      renderBody: () => JSX.Element;
      showFooter: boolean;
    } {
      const { pageKey, workspace, cohortContext } = this.props;

      switch (activeIcon) {
        case 'help':
          return {
            headerPadding: '0.5rem',
            renderHeader: () => (
              <h3
                style={{
                  ...styles.sectionTitle,
                  lineHeight: 1.75,
                }}
              >
                Help Tips
              </h3>
            ),
            renderBody: () => (
              <HelpTips
                {...{ pageKey }}
                allowSearch={true}
                onSearch={() => this.analyticsEvent('Search')}
              />
            ),
            showFooter: true,
          };
        case 'runtimeConfig':
          return {
            headerPadding: '0.75rem',
            renderHeader: () => (
              <div>
                <h3
                  style={{
                    ...styles.sectionTitle,
                    lineHeight: 1.75,
                  }}
                >
                  Cloud analysis environment
                </h3>
              </div>
            ),
            bodyWidthRem: '30',
            bodyPadding: '0 1.25rem',
            renderBody: () => (
              <RuntimeConfigurationPanel
                onClose={() => this.setActiveIcon(null)}
                initialPanelContent={runtimeConfPanelInitialState}
              />
            ),
            showFooter: false,
          };
        case 'apps':
          return {
            bodyWidthRem: '19',
            renderBody: () => (
              <AppsPanel
                {...{ workspace }}
                onClose={() => this.setActiveIcon(null)}
                onClickRuntimeConf={() =>
                  this.openRuntimeConfigWithState(PanelContent.Customize)
                }
                onClickDeleteRuntime={() =>
                  this.openRuntimeConfigWithState(PanelContent.DeleteRuntime)
                }
              />
            ),
            showFooter: false,
          };
        case 'notebooksHelp':
          return {
            headerPadding: '0.5rem',
            renderHeader: () => (
              <h3 style={styles.sectionTitle}>Workspace storage</h3>
            ),
            renderBody: () => (
              <HelpTips allowSearch={false} pageKey='notebook' />
            ),
            showFooter: true,
          };
        case 'annotations':
          return {
            headerPadding: '0.5rem 0.5rem 0 0.5rem',
            renderHeader: () =>
              this.state.participant && (
                <div style={{ fontSize: 18, color: colors.primary }}>
                  {'Participant ' + this.state.participant.participantId}
                </div>
              ),
            renderBody: () =>
              this.state.participant ? (
                <SidebarContent participant={this.state.participant} />
              ) : (
                <Spinner style={{ display: 'block', margin: '3rem auto' }} />
              ),
            showFooter: true,
          };
        case 'concept':
          return {
            headerPadding: '0.75rem',
            renderHeader: () => (
              <h3 style={styles.sectionTitle}>Selected Concepts</h3>
            ),
            bodyWidthRem: '20',
            bodyPadding: '0.75rem 0.75rem 0',
            renderBody: () =>
              !!currentConceptStore.getValue() && <ConceptListPage />,
            showFooter: false,
          };
        case 'criteria':
          return {
            bodyWidthRem: '20',
            bodyPadding: '0.75rem 0.75rem 0',
            renderBody: () =>
              !!cohortContext && (
                <SelectionList
                  back={() => this.setActiveIcon(null)}
                  selections={[]}
                />
              ),
            showFooter: false,
          };
        case 'genomicExtractions':
          return {
            overflow: 'visible',
            headerPadding: '0.75rem',
            renderHeader: () => (
              <h3 style={styles.sectionTitle}>Genomic Extractions</h3>
            ),
            bodyWidthRem: '30',
            renderBody: () => <GenomicsExtractionTable />,
            showFooter: false,
          };
      }
    }

    render() {
      const { activeIcon, runtimeErrors, runtimeConfPanelInitialState } =
        this.state;
      const {
        workspace,
        workspace: { namespace, id },
        pageKey,
        criteria,
      } = this.props;
      const sidebarContent = this.sidebarContent(
        activeIcon,
        runtimeConfPanelInitialState
      );
      const shouldRenderWorkspaceMenu =
        !showConceptIcon(pageKey) && !showCriteriaIcon(pageKey, criteria);

      return (
        <div id='help-sidebar'>
          <div
            style={{
              ...styles.iconContainer,
              ...(pageKey === LEONARDO_APP_PAGE_KEY
                ? styles.notebookOverrides
                : {}),
            }}
          >
            {shouldRenderWorkspaceMenu && (
              <PopupTrigger
                side='bottom'
                closeOnClick
                content={
                  <React.Fragment>
                    <div style={styles.dropdownHeader}>Workspace Actions</div>
                    <WorkspaceActionsMenu
                      workspaceData={workspace}
                      onDuplicate={() => {
                        AnalyticsTracker.Workspaces.OpenDuplicatePage();
                        this.props.navigate([
                          'workspaces',
                          namespace,
                          id,
                          'duplicate',
                        ]);
                      }}
                      onEdit={() => {
                        AnalyticsTracker.Workspaces.OpenEditPage();
                        this.props.navigate([
                          'workspaces',
                          namespace,
                          id,
                          'edit',
                        ]);
                      }}
                      onShare={() => {
                        AnalyticsTracker.Workspaces.OpenShareModal();
                        this.setState({ currentModal: CurrentModal.Share });
                      }}
                      onDelete={() => {
                        AnalyticsTracker.Workspaces.OpenDeleteModal();
                        this.setState({ currentModal: CurrentModal.Delete });
                      }}
                    />
                  </React.Fragment>
                }
              >
                <div
                  aria-label='Open Actions Menu'
                  data-test-id='workspace-menu-button'
                >
                  <TooltipTrigger content={<div>Menu</div>} side='left'>
                    <div
                      style={styles.icon}
                      onClick={() =>
                        this.analyticsEvent(
                          'OpenSidebar',
                          'Sidebar - Menu Icon'
                        )
                      }
                    >
                      <FontAwesomeIcon
                        icon={faEllipsisV}
                        style={{ fontSize: '21px' }}
                      />
                    </div>
                  </TooltipTrigger>
                </div>
              </PopupTrigger>
            )}
            <HelpSidebarIcons
              {...{ ...this.props, activeIcon }}
              onIconClick={(icon) => this.onIconClick(icon)}
            />
          </div>

          <TransitionGroup>
            <CSSTransition
              key={activeIcon}
              classNames='sidebar'
              addEndListener={(node, done) => {
                node.addEventListener(
                  'transitionend',
                  (e) => {
                    if (node.isEqualNode(e.target)) {
                      done(e);
                    }
                  },
                  false
                );
              }}
            >
              <div style={this.sidebarContainerStyles(activeIcon)}>
                <div style={this.sidebarStyle} data-test-id='sidebar-content'>
                  {activeIcon && sidebarContent && (
                    <div
                      style={{
                        height: '100%',
                        overflow: sidebarContent.overflow || 'auto',
                      }}
                    >
                      <FlexColumn style={{ height: '100%' }}>
                        {sidebarContent.renderHeader && (
                          <FlexRow
                            style={{
                              justifyContent: 'space-between',
                              padding: sidebarContent.headerPadding,
                            }}
                          >
                            {sidebarContent.renderHeader()}
                            <CloseButton
                              style={{ marginLeft: 'auto' }}
                              onClose={() => this.setActiveIcon(null)}
                            />
                          </FlexRow>
                        )}

                        <div
                          className='slim-scroll-bar'
                          style={{
                            flex: 1,
                            padding:
                              sidebarContent.bodyPadding || '0 0.5rem 5.5rem',
                          }}
                        >
                          {sidebarContent.renderBody()}
                        </div>
                      </FlexColumn>

                      {sidebarContent.showFooter && (
                        <div style={{ ...styles.footer }}>
                          <h3 style={styles.sectionTitle}>
                            Not finding what you're looking for?
                          </h3>
                          <p style={styles.contentItem}>
                            Visit our{' '}
                            <StyledExternalLink
                              href={supportUrls.helpCenter}
                              target='_blank'
                              onClick={() => this.analyticsEvent('UserSupport')}
                            >
                              {' '}
                              User Support Hub
                            </StyledExternalLink>{' '}
                            page or{' '}
                            <span
                              style={styles.link}
                              onClick={() => this.openContactWidget()}
                            >
                              {' '}
                              contact us
                            </span>
                            .
                          </p>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </CSSTransition>
          </TransitionGroup>

          {switchCase(
            this.state.currentModal,
            [
              CurrentModal.Share,
              () => (
                <WorkspaceShare
                  workspace={this.props.workspace}
                  onClose={() =>
                    this.setState({ currentModal: CurrentModal.None })
                  }
                />
              ),
            ],
            [
              CurrentModal.Delete,
              () => (
                <ConfirmDeleteModal
                  closeFunction={() =>
                    this.setState({ currentModal: CurrentModal.None })
                  }
                  resourceType={ResourceType.WORKSPACE}
                  receiveDelete={() => this.deleteWorkspace()}
                  resourceName={this.props.workspace.name}
                />
              ),
            ],
            [
              CurrentModal.HasRuntimeError,
              () => (
                <RuntimeErrorModal
                  closeFunction={() =>
                    this.setState({ currentModal: CurrentModal.None })
                  }
                  openRuntimePanel={() => this.setActiveIcon('runtimeConfig')}
                  errors={runtimeErrors}
                />
              ),
            ]
          )}
        </div>
      );
    }
  }
);
