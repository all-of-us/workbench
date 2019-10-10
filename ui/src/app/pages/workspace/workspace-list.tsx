import {Component} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {currentWorkspaceStore, navigate} from 'app/utils/navigation';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

import {AlertDanger} from 'app/components/alert';
import {BugReportModal} from 'app/components/bug-report';
import {
  Button,
  CardButton,
  Clickable,
  Link,
  MenuItem
} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ListPageHeader} from 'app/components/headers';
import {ClrIcon, SnowmanIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceShare} from 'app/pages/workspace/workspace-share';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  displayDate,
  reactStyles,
  ReactWrapperBase,
  withUserProfile
} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {
  ErrorResponse,
  Profile, UserRole,
} from 'generated/fetch';
import * as React from 'react';
import RSelect from 'react-select';

const EVENT_CATEGORY = 'Workspace list';

const styles = reactStyles({
  fadeBox: {
    margin: '1rem auto 0 auto', width: '97.5%', padding: '0 1rem'
  },
  cardArea: {
    display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'
  },
  addCard: {
    margin: '0 1rem 1rem 0', fontWeight: 600, color: 'rgb(33, 111, 180)'
  },
  workspaceName: {
    color: colors.accent, marginBottom: '0.5rem', fontWeight: 600,
    fontSize: 18, wordBreak: 'break-all', cursor: 'pointer',
  },
  workspaceDescription: {
    textOverflow: 'ellipsis', overflow: 'hidden', height: '2rem', display: '-webkit-box',
    WebkitLineClamp: 2, WebkitBoxOrient: 'vertical'
  },
  workspaceCard: {
    display: 'flex', flexDirection: 'column', justifyContent: 'space-between', height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative'
  },
  workspaceCardFooter: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'
  },
  permissionBox: {
    color: colors.white, height: '1rem', width: '3rem', fontSize: 10, textAlign: 'center',
    borderRadius: '0.2rem', padding: 0
  }
});


const WorkspaceCardMenu: React.FunctionComponent<{
  disabled: boolean, wp: WorkspacePermissions, onShare: Function, onDelete: Function
}> = ({disabled, wp, onShare, onDelete}) => {
  const wsPathPrefix = 'workspaces/' + wp.workspace.namespace + '/' + wp.workspace.id;

  return <PopupTrigger
      side='bottom'
      closeOnClick
      content={ <React.Fragment>
        <MenuItem icon='copy'
                  onClick={() => {navigate([wsPathPrefix, 'duplicate']); }}>
          Duplicate
        </MenuItem>
        <TooltipTrigger content={<div>Requires Write Permission</div>}
                        disabled={wp.canWrite}>
          <MenuItem icon='pencil'
                    onClick={() => {navigate([wsPathPrefix, 'edit']); }}
                    disabled={!wp.canWrite}>
            Edit
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={wp.isOwner}>
          <MenuItem icon='pencil' onClick={onShare} disabled={!wp.isOwner}>
            Share
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={wp.isOwner}>
          <MenuItem icon='trash' onClick={onDelete} disabled={!wp.isOwner}>
            Delete
          </MenuItem>
        </TooltipTrigger>
      </React.Fragment>}
  >
    <Clickable disabled={disabled} data-test-id='workspace-card-menu'>
      <SnowmanIcon disabled={disabled}
      />
    </Clickable>
  </PopupTrigger>;
};

interface WorkspaceCardState {
  bugReportError: string;
  bugReportOpen: boolean;
  confirmDeleting: boolean;
  // Whether this card is busy loading data specific to the workspace.
  loadingData: boolean;
  sharing: boolean;
  // The list of user roles associated with this workspace. Lazily populated
  // only when the workspace share dialog is opened.
  userRoles?: UserRole[];
  workspaceDeletionError: boolean;
}

interface WorkspaceCardProps {
  userEmail: string;
  wp: WorkspacePermissions;
  // A reload function that can be called by this component to reqeust a refresh
  // of the workspace contained in this card.
  reload(): Promise<void>;
}

export class WorkspaceCard extends React.Component<WorkspaceCardProps, WorkspaceCardState> {
  constructor(props) {
    super(props);
    this.state = {
      bugReportError: '',
      bugReportOpen: false,
      confirmDeleting: false,
      loadingData: false,
      sharing: false,
      userRoles: null,
      workspaceDeletionError: false
    };
  }

  async deleteWorkspace() {
    const {wp} = this.props;
    this.setState({
      confirmDeleting: false,
      loadingData: true});
    try {
      await workspacesApi().deleteWorkspace(wp.workspace.namespace, wp.workspace.id);
      this.setState({loadingData: false});
      await this.props.reload();
    } catch (e) {
      this.setState({workspaceDeletionError: true, loadingData: false});
    }
  }

  // The function called when the "share" action is called on a workspace card
  // within the workspaces list.
  async handleShareAction() {
    this.setState({
      loadingData: true
    });
    const userRolesResponse = await workspacesApi().getFirecloudWorkspaceUserRoles(
      this.props.wp.workspace.namespace,
      this.props.wp.workspace.id);
    // Trigger the sharing dialog to be shown.
    this.setState({
      loadingData: false,
      sharing: true,
      userRoles: userRolesResponse.items});
  }

  async handleShareDialogClose() {
    // Share workspace publishes to current workspace,
    // but here we aren't in the context of a workspace
    // so we need to clear it.
    currentWorkspaceStore.next(undefined);
    this.setState({
      sharing: false,
      userRoles: null});
    this.reloadData();
  }

  submitWorkspaceDeletionError(): void {
    this.setState({
      bugReportOpen: true,
      bugReportError: 'Could not delete workspace.',
      workspaceDeletionError: false});
  }

  // Reloads data by calling the callback from the owning component. This
  // currently causes the workspace-list to reload the entire list of workspaces.
  async reloadData() {
    await this.props.reload();
  }

  render() {
    const {userEmail, wp} = this.props;
    const {bugReportError, bugReportOpen, confirmDeleting, loadingData,
      sharing, userRoles, workspaceDeletionError} = this.state;

    return <React.Fragment>
      <WorkspaceCardBase>
        <div style={styles.workspaceCard} data-test-id='workspace-card'>
          {loadingData && <SpinnerOverlay/>}
          <FlexColumn>
            <FlexRow style={{alignItems: 'flex-start'}}>
              <WorkspaceCardMenu wp={wp}
                                 onDelete={() => {
                                   triggerEvent(
                                     EVENT_CATEGORY, 'delete', 'Card menu - click delete');
                                   this.setState({confirmDeleting: true});
                                 }}
                                 onShare={() => {
                                   triggerEvent(EVENT_CATEGORY, 'share', 'Card menu - click share');
                                   this.handleShareAction();
                                 }}
                                 disabled={false}/>
              <Clickable>
                <div style={styles.workspaceName}
                     data-test-id='workspace-card-name'
                     onClick={() => {
                       triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
                       navigate(['workspaces', wp.workspace.namespace, wp.workspace.id, 'data']);
                     }}>
                  {wp.workspace.name}</div>
              </Clickable>
            </FlexRow>
            {wp.isPending && <div style={{color: colors.warning}}>
              <ClrIcon shape='exclamation-triangle' className='is-solid'
                       style={{fill: colors.warning}}/>
              Pending Approval
            </div>}
            {wp.isRejected && <div style={{color: colors.danger}}>
              <ClrIcon shape='exclamation-triangle' className='is-solid'
                       style={{fill: colors.danger}}/>
              Rejected
            </div> }
          </FlexColumn>
          <div style={styles.workspaceCardFooter}>
            <div style={{fontSize: 12, lineHeight: '17px'}}>Last Changed: <br/>
              {displayDate(wp.workspace.lastModifiedTime)}</div>
            <div style={{
              ...styles.permissionBox,
              backgroundColor: colors.workspacePermissionsHighlights[wp.accessLevel]}}
                 data-test-id='workspace-access-level'>{wp.accessLevel}</div>
          </div>
        </div>
      </WorkspaceCardBase>
      {workspaceDeletionError && <Modal>
        <ModalTitle>Error: Could not delete workspace '{wp.workspace.name}'</ModalTitle>
        <ModalBody style={{display: 'flex', flexDirection: 'row'}}>
          Please{' '}
          <Link onClick={() => this.submitWorkspaceDeletionError()}>submit a bug report.</Link>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.setState({workspaceDeletionError: false})}>Close</Button>
        </ModalFooter>
      </Modal>}
      {confirmDeleting &&
        <ConfirmDeleteModal data-test-id='confirm-delete-modal'
                            resourceType='workspace'
                            resourceName={wp.workspace.name}
                            receiveDelete={() => {this.deleteWorkspace(); }}
                            closeFunction={() => {this.setState({confirmDeleting: false}); }}/>}
      {sharing && <WorkspaceShare data-test-id='workspace-share-modal'
                                  workspace={wp.workspace}
                                  accessLevel={wp.accessLevel}
                                  userEmail={userEmail}
                                  sharing={sharing}
                                  userRoles={userRoles}
                                  onClose={() => this.handleShareDialogClose()} />}
      {bugReportOpen && <BugReportModal bugReportDescription={bugReportError}
                                        onClose={() => this.setState({bugReportOpen: false})}/>}
    </React.Fragment>;

  }
}


export const WorkspaceList = withUserProfile()
(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { workspacesLoading: boolean,
    workspaceList: WorkspacePermissions[],
    errorText: string,
    firstSignIn: Date,
  }> {
  private timer: NodeJS.Timer;

  constructor(props) {
    super(props);
    this.state = {
      workspacesLoading: true,
      workspaceList: [],
      errorText: '',
      firstSignIn: undefined
    };
  }

  componentDidMount() {
    this.reloadWorkspaces(null);
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
  }

  async reloadWorkspaces(filter) {
    filter = filter ? filter : (() => true);
    this.setState({workspacesLoading: true});
    try {
      const workspacesReceived = (await workspacesApi().getWorkspaces())
        .items.filter(response => filter(response.accessLevel));
      workspacesReceived.sort(
        (a, b) => a.workspace.name.localeCompare(b.workspace.name));
      this.setState({workspaceList: workspacesReceived
          .map(w => new WorkspacePermissions(w))});
      this.setState({workspacesLoading: false});
    } catch (e) {
      const response = ErrorHandlingService.convertAPIError(e) as unknown as ErrorResponse;
      this.setState({errorText: response.message});
    }
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {
      errorText,
      workspaceList,
      workspacesLoading
    } = this.state;

    // Maps each "Filter by" dropdown element to a set of access levels to display.
    const filters = [
      { label: 'Owner',  value: ['OWNER'] },
      { label: 'Writer', value: ['WRITER'] },
      { label: 'Reader', value: ['READER'] },
      { label: 'All',    value: ['OWNER', 'READER', 'WRITER'] },
    ];
    const defaultFilter = filters.find(f => f.label === 'All');

    return <React.Fragment>
      <FadeBox style={styles.fadeBox}>
        <div style={{padding: '0 1rem'}}>
          <ListPageHeader>Workspaces</ListPageHeader>
          <FlexRow style={{marginTop: '0.5em'}}>
            <div style={{margin: '0', padding: '0.5em 0.75em 0 0'}}>Filter by</div>
            <RSelect options={filters}
              defaultValue={defaultFilter}
              onChange={(levels) => {
                this.reloadWorkspaces(level => levels.value.includes(level));
              }}/>
          </FlexRow>
          {errorText && <AlertDanger>
            <ClrIcon shape='exclamation-circle'/>
            {errorText}
          </AlertDanger>}
          <div style={styles.cardArea}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '1.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '1.5rem', flexWrap: 'wrap'}}>
                <CardButton onClick={() => navigate(['workspaces/build'])}
                            style={styles.addCard}>
                  Create a <br/> New Workspace
                  <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                </CardButton>
                {workspaceList.map(wp => {
                  return <WorkspaceCard key={wp.workspace.name}
                                        wp={wp}
                                        userEmail={profile.username}
                                        reload={() => this.reloadWorkspaces(null)}/>;
                })}
              </div>)}
          </div>
        </div>
      </FadeBox>
    </React.Fragment>;
  }


});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceListComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceList, []);
  }
}
