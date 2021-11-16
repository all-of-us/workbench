import * as fp from 'lodash/fp';
import * as React from 'react';

import {ResourceType, Workspace, WorkspaceAccessLevel} from 'generated/fetch';

import {Button, Clickable, MenuItem, SnowmanButton} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon, ControlledTierBadge} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle, withErrorModal} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {AouTitle} from 'app/components/text-wrappers';
import {WorkspaceShare} from 'app/pages/workspace/workspace-share';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {displayDate, reactStyles} from 'app/utils';
import {AccessTierShortNames, displayNameForTier} from 'app/utils/access-tiers';
import {AnalyticsTracker, triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore, NavigationProps, useNavigation} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {withNavigation} from 'app/utils/with-navigation-hoc';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {faCheck} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faLockAlt} from '@fortawesome/pro-solid-svg-icons';

const EVENT_CATEGORY = 'Workspace list';

const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative'
  },
  workspaceMenuWrapper: {
    paddingTop: '.5rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, .6),
    flex: '0 0 1rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  workspaceName: {
    color: colors.accent,
    marginBottom: '0.5rem',
    fontSize: 18,
    wordBreak: 'break-all'
  },
  workspaceNameDisabled: {
    color: colors.disabled,
    marginBottom: '0.5rem',
    fontSize: 18,
    wordBreak: 'break-all'
  },
  permissionBox: {
    color: colors.white,
    height: '1rem',
    width: '3rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.2rem',
    padding: 0
  },
  lockWorkspace: {
    color: colors.warning,
    marginBottom: '0.2rem'
  }
});

interface WorkspaceCardMenuProps {
  disabled: boolean;
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  onShare: Function;
  onDelete: Function;
}

const WorkspaceCardMenu: React.FunctionComponent<WorkspaceCardMenuProps> = ({
  workspace,
  accessLevel,
  onShare,
  onDelete
}) => {
  const [navigate, ] = useNavigation();

  const wsPathPrefix = 'workspaces/' + workspace.namespace + '/' + workspace.id;

  return <PopupTrigger
    side='bottom'
    closeOnClick
    content={
      <React.Fragment>
        <MenuItem icon='copy'
                  onClick={() => {
                    // Using workspace.published here to identify Featured Workspaces. At some point, we will need a separate property for
                    // this on the workspace object once users are able to publish their own workspaces
                    workspace.published ?
                      AnalyticsTracker.Workspaces.DuplicateFeatured(workspace.name) :
                      AnalyticsTracker.Workspaces.OpenDuplicatePage('Card');
                    navigate([wsPathPrefix, 'duplicate']);
                  }}>
          Duplicate
        </MenuItem>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={WorkspacePermissionsUtil.isOwner(accessLevel)}>
          <MenuItem icon='pencil'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenEditPage('Card');
                      navigate([wsPathPrefix, 'edit']); }
                    }
                    disabled={!WorkspacePermissionsUtil.isOwner(accessLevel)}>
            Edit
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div data-test-id='workspace-share-disabled-tooltip'>Requires Owner Permission</div>}
                        disabled={WorkspacePermissionsUtil.isOwner(accessLevel)}>
          <MenuItem icon='pencil'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenShareModal('Card');
                      onShare();
                    }}
                    disabled={!WorkspacePermissionsUtil.isOwner(accessLevel)}>
            Share
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={WorkspacePermissionsUtil.isOwner(accessLevel)}>
          <MenuItem icon='trash'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenDeleteModal('Card');
                      onDelete();
                    }}
                    disabled={!WorkspacePermissionsUtil.isOwner(accessLevel)}>
            Delete
          </MenuItem>
        </TooltipTrigger>
      </React.Fragment>
    }
  >
    <SnowmanButton style={{marginLeft: 0}} data-test-id='workspace-card-menu'/>
  </PopupTrigger>;
};

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

    deleteWorkspace = withErrorModal({
      title: 'Error Deleting Workspace',
      message: `Could not delete workspace '${this.props.workspace.id}'.`,
      showBugReportLink: true,
      onDismiss: () => {
        this.setState({confirmDeleting: false});
      }
    }, async() => {
      AnalyticsTracker.Workspaces.Delete();
      await workspacesApi().deleteWorkspace(this.props.workspace.namespace, this.props.workspace.id);
      await this.props.reload();
    });

    async handleShareDialogClose() {
      // Share workspace publishes to current workspace,
      // but here we aren't in the context of a workspace
      // so we need to clear it.
      currentWorkspaceStore.next(undefined);
      this.setState({showShareModal: false});
      this.reloadData();
    }

    // Reloads data by calling the callback from the owning component. This
    // currently causes the workspace-list to reload the entire list of recentWorkspaces.
    async reloadData() {
      await this.props.reload();
    }

    handleReviewResearchPurpose() {
      const {workspace} = this.props;
      this.props.navigate(['workspaces', workspace.namespace, workspace.id, 'about']);

    }

    onClick() {
      const {workspace} = this.props;
      if (serverConfigStore.get().config.enableResearchReviewPrompt && workspace.researchPurpose.needsReviewPrompt) {
        this.setState({showResearchPurposeReviewModal: true});
      } else {
        workspace.published ?
          AnalyticsTracker.Workspaces.NavigateToFeatured(workspace.name) :
          triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
        this.props.navigate(['workspaces', workspace.namespace, workspace.id, 'data']);
      }
    }

    render() {
      const {
        workspace,
        workspace: {accessTierShortName, adminLocked},
        accessLevel,
        tierAccessDisabled
      } = this.props;
      const {confirmDeleting, showShareModal, showResearchPurposeReviewModal} = this.state;
      return <React.Fragment>
        <WorkspaceCardBase>
          <FlexRow style={{height: '100%'}}>
            <FlexColumn style={styles.workspaceMenuWrapper}>
              {!tierAccessDisabled && <WorkspaceCardMenu
                workspace={workspace}
                accessLevel={accessLevel}
                onDelete={() => {
                  triggerEvent(
                    EVENT_CATEGORY, 'delete', 'Card menu - click delete');
                  this.setState({confirmDeleting: true});
                }}
                onShare={() => {
                  triggerEvent(EVENT_CATEGORY, 'share', 'Card menu - click share');
                  this.setState({showShareModal: true});
                }}
                disabled={false}
              />}
            </FlexColumn>
            <FlexColumn
              style={{
                ...styles.workspaceCard,
                padding: '.5rem',
              }}
              data-test-id='workspace-card'
            >
              <FlexColumn style={{marginBottom: 'auto'}}>
                <FlexRow style={{ alignItems: 'flex-start' }}>
                  <Clickable style={{cursor: tierAccessDisabled ? 'not-allowed' : 'pointer', ...styles}}
                             onClick={() => this.onClick()}
                             disabled={tierAccessDisabled}>
                    <TooltipTrigger content={tierAccessDisabled && <div>
                      This workspace is a {displayNameForTier(accessTierShortName)} workspace. You do not have access.
                      Please complete the data access requirements to gain access.
                    </div>
                    }>
                    <div style={tierAccessDisabled ? styles.workspaceNameDisabled : styles.workspaceName}
                         data-test-id='workspace-card-name'>
                      {workspace.name}
                    </div>
                    </TooltipTrigger>
                  </Clickable>
                </FlexRow>
                {
                  workspace.researchPurpose.reviewRequested === true &&
                  workspace.researchPurpose.approved === false &&
                  <div style={{color: colors.danger}}>
                      <ClrIcon shape='exclamation-triangle' className='is-solid'
                               style={{fill: colors.danger}}/>
                      Rejected
                  </div>
                }
              </FlexColumn>
              <FlexRow style={{justifyContent: 'space-between'}}>
                <FlexColumn>
                  <div
                    style={{
                      ...styles.permissionBox,
                      backgroundColor: colors.workspacePermissionsHighlights[accessLevel]
                    }}
                    data-test-id='workspace-access-level'
                  >
                    {accessLevel}
                  </div>
                  <div style={{fontSize: 12}}>
                    Last Changed: {displayDate(workspace.lastModifiedTime)}
                  </div>
                </FlexColumn>
                <FlexColumn style={{justifyContent: 'flex-end'}}>
                  {accessTierShortName === AccessTierShortNames.Controlled && <ControlledTierBadge/>}
                </FlexColumn>
                {adminLocked && <FlexColumn data-test-id='workspace-lock' style={{justifyContent: 'flex-end'}}>
                  <TooltipTrigger content='Workspace compliance action is required'>
                    <FontAwesomeIcon icon={faLockAlt} style={styles.lockWorkspace}/>
                  </TooltipTrigger>
                </FlexColumn>}
              </FlexRow>
            </FlexColumn>
          </FlexRow>
        </WorkspaceCardBase>
        {confirmDeleting &&
        <ConfirmDeleteModal data-test-id='confirm-delete-modal'
                            resourceType={ResourceType.WORKSPACE}
                            resourceName={workspace.name}
                            receiveDelete={() => {
                              AnalyticsTracker.Workspaces.Delete();
                              this.deleteWorkspace();
                            }}
                            closeFunction={() => {this.setState({confirmDeleting: false}); }}/>}
        {showShareModal && <WorkspaceShare data-test-id='workspace-share-modal'
                                           workspace={{...workspace, accessLevel}}
                                           onClose={() => this.handleShareDialogClose()} />}
        {showResearchPurposeReviewModal && <Modal data-test-id='workspace-review-modal'>
            <ModalTitle>Please review Research Purpose for Workspace '{workspace.name}'</ModalTitle>
            <ModalBody style={{display: 'flex', flexDirection: 'column'}}>
                <div>
                    Now that you have had some time to explore the Researcher Workbench for your project,
                    please review your workspace description to make sure it is accurate. As a reminder,
                    project descriptions are publicly cataloged in the <AouTitle/>'s <a
                    href='https://www.researchallofus.org/research-projects-directory/' target='_blank'>
                    Research Project Directory</a> for participants and public to review.
                </div>
            </ModalBody>
            <ModalFooter>
                <Button type='primary' style={{marginLeft: '1rem', marginRight: '1rem'}}
                        onClick={() => this.handleReviewResearchPurpose()}>REVIEW NOW</Button>
                <Button type='secondary'
                        onClick={() => this.setState({showResearchPurposeReviewModal: false})}>REVIEW LATER</Button>
            </ModalFooter>
        </Modal>}
      </React.Fragment>;

    }
  }
);

