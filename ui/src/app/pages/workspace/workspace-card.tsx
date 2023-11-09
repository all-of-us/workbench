import * as React from 'react';
import * as fp from 'lodash/fp';
import { faLock } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace, WorkspaceAccessLevel } from 'generated/fetch';

import { SnowmanButton, StyledRouterLink } from 'app/components/buttons';
import { WorkspaceCardBase } from 'app/components/card';
import { ConfirmWorkspaceDeleteModal } from 'app/components/confirm-workspace-delete-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon, ControlledTierBadge } from 'app/components/icons';
import { withErrorModal } from 'app/components/modals';
import { PopupTrigger, TooltipTrigger } from 'app/components/popups';
import { AouTitle } from 'app/components/text-wrappers';
import { WorkspaceMenu } from 'app/pages/workspace/workspace-menu';
import { WorkspaceShare } from 'app/pages/workspace/workspace-share';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  AccessTierShortNames,
  displayNameForTier,
} from 'app/utils/access-tiers';
import { AnalyticsTracker, triggerEvent } from 'app/utils/analytics';
import { displayDate } from 'app/utils/dates';
import { currentWorkspaceStore, NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';

import { WorkspaceActionsMenu } from './workspace-actions-menu';

const EVENT_CATEGORY = 'Workspace list';

const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative',
  },
  workspaceMenuWrapper: {
    paddingTop: '.75rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, 0.6),
    flex: '0 0 1.5rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  workspaceName: {
    color: colors.accent,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
  },
  workspaceNameDisabled: {
    color: colors.disabled,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
    pointerEvents: 'none',
    cursor: 'not-allowed',
  },
  permissionBox: {
    color: colors.white,
    height: '1.5rem',
    width: '4.5rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.3rem',
    padding: 0,
  },
  lockWorkspace: {
    color: colors.warning,
    marginBottom: '0.15rem',
    width: '21px',
    height: '21px',
    viewBox: '0 0 25 27',
  },
});

interface WorkspaceCardState {
  confirmDeleting: boolean;
  showShareModal: boolean;
}

interface WorkspaceCardProps extends NavigationProps {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  // A reload function that can be called by this component to request a refresh
  // of the workspace contained in this card.
  reload(): Promise<void>;
  // non-CT users cannot click or see on CT workspaces.
  tierAccessDisabled?: boolean;
}

export const WorkspaceCard = fp.flow(withNavigation)(
  class extends React.Component<WorkspaceCardProps, WorkspaceCardState> {
    constructor(props) {
      super(props);
      this.state = {
        confirmDeleting: false,
        showShareModal: false,
      };
    }

    deleteWorkspace = withErrorModal(
      {
        title: 'Error Deleting Workspace',
        message: `Could not delete workspace '${this.props.workspace.id}'.`,
        showBugReportLink: true,
        onDismiss: () => {
          this.setState({ confirmDeleting: false });
        },
      },
      async () => {
        AnalyticsTracker.Workspaces.Delete();
        await workspacesApi().deleteWorkspace(
          this.props.workspace.namespace,
          this.props.workspace.id
        );
        await this.props.reload();
      }
    );

    async handleShareDialogClose() {
      // Share workspace publishes to current workspace,
      // but here we aren't in the context of a workspace
      // so we need to clear it.
      currentWorkspaceStore.next(undefined);
      this.setState({ showShareModal: false });
      this.reloadData();
    }

    // Reloads data by calling the callback from the owning component. This
    // currently causes the workspace-list to reload the entire list of recentWorkspaces.
    async reloadData() {
      await this.props.reload();
    }

    trackWorkspaceNavigation() {
      const {
        workspace: { name, published },
      } = this.props;
      published
        ? AnalyticsTracker.Workspaces.NavigateToFeatured(name)
        : triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
    }

    render() {
      const {
        workspace,
        workspace: { accessTierShortName, adminLocked, namespace, id },
        accessLevel,
        tierAccessDisabled,
        navigate,
      } = this.props;
      const { confirmDeleting, showShareModal } = this.state;
      return (
        <React.Fragment>
          <WorkspaceCardBase>
            <FlexRow style={{ height: '100%' }}>
              <WorkspaceMenu
                tierAccessDisabled={tierAccessDisabled}
                workspace={workspace}
                accessLevel={accessLevel}
              />
              <FlexColumn
                style={{
                  ...styles.workspaceCard,
                  padding: '.75rem',
                }}
                data-test-id='workspace-card'
              >
                <FlexColumn style={{ marginBottom: 'auto' }}>
                  <FlexRow style={{ alignItems: 'flex-start' }}>
                    <StyledRouterLink
                      style={
                        tierAccessDisabled
                          ? styles.workspaceNameDisabled
                          : styles.workspaceName
                      }
                      analyticsFn={() => this.trackWorkspaceNavigation()}
                      data-test-id={'workspace-card-link'}
                      propagateDataTestId
                      path={`/workspaces/${namespace}/${id}/data`}
                    >
                      <TooltipTrigger
                        content={
                          tierAccessDisabled && (
                            <div>
                              This workspace is a{' '}
                              {displayNameForTier(accessTierShortName)}{' '}
                              workspace. You do not have access. Please complete
                              the data access requirements to gain access.
                            </div>
                          )
                        }
                      >
                        <div
                          style={
                            tierAccessDisabled
                              ? styles.workspaceNameDisabled
                              : styles.workspaceName
                          }
                          data-test-id='workspace-card-name'
                        >
                          {workspace.name}
                        </div>
                      </TooltipTrigger>
                    </StyledRouterLink>
                  </FlexRow>
                  {workspace.researchPurpose.reviewRequested === true &&
                    workspace.researchPurpose.approved === false && (
                      <div style={{ color: colors.danger }}>
                        <ClrIcon
                          shape='exclamation-triangle'
                          className='is-solid'
                          style={{ fill: colors.danger }}
                        />
                        Rejected
                      </div>
                    )}
                </FlexColumn>
                <FlexRow style={{ justifyContent: 'space-between' }}>
                  <FlexColumn>
                    <div
                      style={{
                        ...styles.permissionBox,
                        backgroundColor:
                          colors.workspacePermissionsHighlights[accessLevel],
                      }}
                      data-test-id='workspace-access-level'
                    >
                      {accessLevel}
                    </div>
                    <div style={{ fontSize: 12 }}>
                      Last Changed: {displayDate(workspace.lastModifiedTime)}
                    </div>
                  </FlexColumn>
                  <FlexColumn
                    style={{ justifyContent: 'flex-end', marginLeft: '1.2rem' }}
                  >
                    <FlexRow style={{ alignContent: 'space-between' }}>
                      {adminLocked && (
                        <FlexColumn
                          data-test-id='workspace-lock'
                          style={{ justifyContent: 'flex-end' }}
                        >
                          <TooltipTrigger content='Workspace compliance action is required'>
                            <FontAwesomeIcon
                              icon={faLock}
                              style={styles.lockWorkspace}
                            />
                          </TooltipTrigger>
                        </FlexColumn>
                      )}
                      {accessTierShortName ===
                        AccessTierShortNames.Controlled && (
                        <ControlledTierBadge />
                      )}
                    </FlexRow>
                  </FlexColumn>
                </FlexRow>
              </FlexColumn>
            </FlexRow>
          </WorkspaceCardBase>
          {confirmDeleting && (
            <ConfirmWorkspaceDeleteModal
              data-test-id='confirm-delete-modal'
              closeFunction={() => {
                this.setState({ confirmDeleting: false });
              }}
              receiveDelete={() => {
                AnalyticsTracker.Workspaces.Delete();
                this.deleteWorkspace();
              }}
              workspaceNamespace={workspace.namespace}
              workspaceName={workspace.name}
            />
          )}
          {showShareModal && (
            <WorkspaceShare
              data-test-id='workspace-share-modal'
              workspace={{ ...workspace, accessLevel }}
              onClose={() => this.handleShareDialogClose()}
            />
          )}
        </React.Fragment>
      );
    }
  }
);
