import * as React from 'react';
import { CSSTransition, TransitionGroup } from 'react-transition-group';
import * as fp from 'lodash/fp';
import { faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { CdrVersionTiersResponse } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { CloseButton, StyledExternalLink } from 'app/components/buttons';
import { ConfirmWorkspaceDeleteModal } from 'app/components/confirm-workspace-delete-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  HelpSidebarIcons,
  IconConfig,
  SidebarIconId,
} from 'app/components/help-sidebar-icons';
import { HelpTips } from 'app/components/help-tips';
import { withErrorModal } from 'app/components/modals';
import { PopupTrigger, TooltipTrigger } from 'app/components/popups';
import { WorkspaceActionsMenu } from 'app/pages/workspace/workspace-actions-menu';
import { WorkspaceShare } from 'app/pages/workspace/workspace-share';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { NavigationProps, sidebarActiveIconStore } from 'app/utils/navigation';
import { routeDataStore } from 'app/utils/stores';
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
    padding: '1.875rem 1.125rem',
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
  betaBadge: {
    border: `3px solid`,
    borderRadius: '11px',
    padding: '0 0.5rem',
    backgroundColor: colors.white,
    lineHeight: '1rem',
  },
  cromwellBetaBadge: {
    borderColor: '#F0B1D0',
  },
  rstudioBetaBadge: {
    borderColor: '#72abdc',
  },
  sasBetaBadge: {
    borderColor: '#008ACD',
  },
});

export const LEONARDO_APP_PAGE_KEY = 'leonardo_app';

const pageKeyToAnalyticsLabels = {
  about: 'About Page',
  data: 'Data Landing Page',
  notebooks: 'Analysis Tab Landing Page',
};

interface Props extends NavigationProps {
  pageKey: string;
  profileState: any;
  shareFunction: Function;
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
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
  searchTerm: string;
  tooltipId: number;
  currentModal: CurrentModal;
}

export const HelpSidebar = fp.flow(
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
        searchTerm: '',
        tooltipId: undefined,
        currentModal: CurrentModal.None,
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
          this.props.workspace.terraName
        );
        this.props.navigate(['workspaces']);
      }
    );

    setActiveIcon(activeIcon: SidebarIconId) {
      sidebarActiveIconStore.next(activeIcon);
    }

    async componentDidMount() {
      const initialActiveIcon = localStorage.getItem(
        LOCAL_STORAGE_KEY_SIDEBAR_STATE
      ) as SidebarIconId;
      // This is being set here instead of the constructor to show the opening animation of the side panel and
      // indicate to the user that it's something they can close.
      this.setActiveIcon(initialActiveIcon);
      this.subscriptions.push(
        sidebarActiveIconStore.subscribe((activeIcon) => {
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
        width: `${Number(this.sidebarWidth) + 0.75}rem`,
      };
    }

    get sidebarWidth() {
      const { activeIcon } = this.state;
      return fp.getOr('21', 'bodyWidthRem', this.sidebarContent(activeIcon));
    }

    sidebarContent(activeIcon: SidebarIconId): {
      overflow?: string;
      headerPadding?: string;
      renderHeader?: () => JSX.Element;
      bodyWidthRem?: string;
      bodyPadding?: string;
      renderBody: () => JSX.Element;
      showFooter: boolean;
    } {
      const { pageKey } = this.props;

      switch (activeIcon) {
        case 'help':
          return {
            headerPadding: '0.75rem',
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
      }
    }

    render() {
      const { activeIcon } = this.state;
      const {
        workspace,
        workspace: { namespace, terraName },
        pageKey,
      } = this.props;
      const sidebarContent = this.sidebarContent(activeIcon);

      const closeButton = (
        <CloseButton
          style={{ marginLeft: 'auto' }}
          onClose={() => this.setActiveIcon(null)}
        />
      );

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
                        terraName,
                        'duplicate',
                      ]);
                    }}
                    onEdit={() => {
                      AnalyticsTracker.Workspaces.OpenEditPage();
                      this.props.navigate([
                        'workspaces',
                        namespace,
                        terraName,
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
                      this.analyticsEvent('OpenSidebar', 'Sidebar - Menu Icon')
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

            <HelpSidebarIcons
              {...{ ...this.props, activeIcon }}
              onIconClick={(icon) => this.onIconClick(icon)}
            />
          </div>

          <TransitionGroup>
            <CSSTransition<undefined>
              key={activeIcon}
              classNames='sidebar'
              addEndListener={(node, done) => {
                node.addEventListener('transitionend', done, false);
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
                            {closeButton}
                          </FlexRow>
                        )}

                        <div
                          className='slim-scroll-bar'
                          style={{
                            flex: 1,
                            padding:
                              sidebarContent.bodyPadding || '0 0.75rem 8.25em',
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
                <ConfirmWorkspaceDeleteModal
                  closeFunction={() =>
                    this.setState({ currentModal: CurrentModal.None })
                  }
                  receiveDelete={() => this.deleteWorkspace()}
                  workspaceName={this.props.workspace.name}
                />
              ),
            ]
          )}
        </div>
      );
    }
  }
);
