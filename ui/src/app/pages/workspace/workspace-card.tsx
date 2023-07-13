import * as React from 'react';
import * as fp from 'lodash/fp';
import { faLockAlt } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace, WorkspaceAccessLevel } from 'generated/fetch';

import {
  Button,
  SnowmanButton,
  StyledRouterLink,
} from 'app/components/buttons';
import { WorkspaceCardBase } from 'app/components/card';
import { ConfirmWorkspaceDeleteModal } from 'app/components/confirm-workspace-delete-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon, ControlledTierBadge } from 'app/components/icons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
  withErrorModal,
} from 'app/components/modals';
import { PopupTrigger, TooltipTrigger } from 'app/components/popups';
import { AouTitle } from 'app/components/text-wrappers';
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
import { serverConfigStore } from 'app/utils/stores';
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
  showResearchPurposeReviewModal: boolean;
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
        showResearchPurposeReviewModal: false,
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

    handleReviewResearchPurpose() {
      const { workspace } = this.props;
      this.props.navigate([
        'workspaces',
        workspace.namespace,
        workspace.id,
        'about',
      ]);
    }

    requiresReviewPrompt() {
      return (
        serverConfigStore.get().config.enableResearchReviewPrompt &&
        this.props.workspace.researchPurpose.needsReviewPrompt
      );
    }

    trackWorkspaceNavigation() {
      const {
        workspace: { name, published },
      } = this.props;
      published
        ? AnalyticsTracker.Workspaces.NavigateToFeatured(name)
        : triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
    }

    onClick(e) {
      if (this.requiresReviewPrompt()) {
        this.setState({ showResearchPurposeReviewModal: true });
        e.preventDefault();
      }
    }

    render() {
      const {
        workspace,
        workspace: { accessTierShortName, adminLocked, namespace, id },
        accessLevel,
        tierAccessDisabled,
        navigate,
      } = this.props;
      const {
        confirmDeleting,
        showShareModal,
        showResearchPurposeReviewModal,
      } = this.state;
      return (
        <React.Fragment>
          <WorkspaceCardBase>
            <FlexRow style={{ height: '100%' }}>
              <FlexColumn style={styles.workspaceMenuWrapper}>
                {!tierAccessDisabled && (
                  <PopupTrigger
                    side='bottom'
                    closeOnClick
                    content={
                      <WorkspaceActionsMenu
                        workspaceData={{ ...workspace, accessLevel }}
                        onDuplicate={() => {
                          // Using workspace.published here to identify Featured Workspaces. At some point, we will need a separate property for
                          // this on the workspace object once users are able to publish their own workspaces
                          workspace.published
                            ? AnalyticsTracker.Workspaces.DuplicateFeatured(
                                workspace.name
                              )
                            : AnalyticsTracker.Workspaces.OpenDuplicatePage(
                                'Card'
                              );
                          navigate(['workspaces', namespace, id, 'duplicate']);
                        }}
                        onEdit={() => {
                          AnalyticsTracker.Workspaces.OpenEditPage('Card');
                          navigate(['workspaces', namespace, id, 'edit']);
                        }}
                        onDelete={() => {
                          AnalyticsTracker.Workspaces.OpenDeleteModal('Card');
                          triggerEvent(
                            EVENT_CATEGORY,
                            'delete',
                            'Card menu - click delete'
                          );
                          this.setState({ confirmDeleting: true });
                        }}
                        onShare={() => {
                          AnalyticsTracker.Workspaces.OpenShareModal('Card');
                          triggerEvent(
                            EVENT_CATEGORY,
                            'share',
                            'Card menu - click share'
                          );
                          this.setState({ showShareModal: true });
                        }}
                      />
                    }
                  >
                    <SnowmanButton style={{ marginLeft: 0 }} />
                  </PopupTrigger>
                )}
              </FlexColumn>
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
                      onClick={(e) => this.onClick(e)}
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
                  {this.requiresReviewPrompt() && (
                    <div style={{ color: colors.warning }}>
                      <ClrIcon
                        shape={'warning-standard'}
                        class={'is-solid'}
                        size={20}
                        style={{
                          color: colors.warning,
                          flex: '0 0 auto',
                        }}
                      />
                      Needs Review
                    </div>
                  )}
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
                              icon={faLockAlt}
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
          {showResearchPurposeReviewModal && (
            <Modal data-test-id='workspace-review-modal'>
              <ModalTitle>
                Please review Research Purpose for Workspace '{workspace.name}'
              </ModalTitle>
              <ModalBody style={{ display: 'flex', flexDirection: 'column' }}>
                <div>
                  Now that you have had some time to explore the Researcher
                  Workbench for your project, please review your workspace
                  description to make sure it is accurate. As a reminder,
                  project descriptions are publicly cataloged in the{' '}
                  <AouTitle />
                  's{' '}
                  <a
                    href='https://www.researchallofus.org/research-projects-directory/'
                    target='_blank'
                  >
                    Research Project Directory
                  </a>{' '}
                  for participants and public to review.
                </div>
              </ModalBody>
              <ModalFooter>
                <Button
                  type='primary'
                  style={{ marginLeft: '1.5rem', marginRight: '1.5rem' }}
                  onClick={() => this.handleReviewResearchPurpose()}
                >
                  REVIEW NOW
                </Button>
                <Button
                  type='secondary'
                  onClick={() =>
                    this.setState({ showResearchPurposeReviewModal: false })
                  }
                >
                  REVIEW LATER
                </Button>
              </ModalFooter>
            </Modal>
          )}
        </React.Fragment>
      );
    }
  }
);
