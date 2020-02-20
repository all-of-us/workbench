import {Component, Input, OnInit} from '@angular/core';

import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {isBlank, reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';

import {Select} from 'app/components/inputs';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {User} from 'generated';

import {
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceUserRolesResponse,
} from 'generated/fetch/api';

import {Button} from 'app/components/buttons';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AnalyticsTracker} from 'app/utils/analytics';

const styles = reactStyles( {
  tooltipLabel: {
    paddingBottom: '0.5rem',
    wordWrap: 'break-word',
  },

  modalTitle: {
    margin: '0',
    fontSize: '.91667rem',
    fontWeight: 600,
    paddingBottom: '1rem'
  },

  tooltipPoint: {
    listStylePosition: 'outside',
    marginLeft: '0.6rem'
  },

  collabUser: {
    fontSize: '.66667rem',
    marginTop: '1rem',
    letterSpacing: '.01em',
    color: colors.primary
  },

  dropdown: {
    width: '100%',
    height: '1.75rem',
    marginTop: '0',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.85),
    borderRadius: '5px'
  },

  open: {
    overflow: 'hidden',
    position: 'absolute',
    backgroundColor: colors.white,
    border: '1px solid'
  },

  noBorder: {
    border: 'none',
    background: 'none',
    width: '90%',
    marginTop: '8px'
  },

  spinner: {
    animation: '1s linear infinite spin',
    position: 'relative',
    display: 'inline-block',
    verticalAlign: 'text-bottom'
  },

  dropdownMenu: {
    display: 'block',
    maxHeight: '12rem',
    minHeight: '30px',
    visibility: 'visible',
    overflowY: 'scroll',
    maxWidth: 'calc(100% - 2.25rem)',
    width: '100%',
    marginTop: '.25rem',
    zIndex: 100
  },

  wrapper: {
    display: 'grid',
    gridTemplateColumns: '9rem 0.1rem 0.1rem',
    gridGap: '10px',
    alignItems: 'center',
    height: '6.38%'
  },

  box: {
    borderRadius: '5px',
    paddingTop: '0.2rem',
    paddingLeft: '0.2rem'
  },

  userName: {
    marginTop: '0',
    paddingTop: '0',
    fontSize: '0.58333rem',
    height: '4.5%',
    color: colors.primary,
    fontFamily: 'Montserrat',
    lineHeight: '1rem',
    whiteSpace: 'nowrap'
  },

  collaboratorIcon: {
    margin: '0 0 0 5rem',
    color: colors.accent,
    cursor: 'pointer'
  },

  sharingBody: {
    maxHeight: '70vh',
    overflowY: 'hidden',
    overflowX: 'visible',
    padding: '0 .125rem',
    display: 'flex',
    flexDirection: 'column'
  },

});

export const UserRoleOptions = [
  {value: WorkspaceAccessLevel.READER, label: 'Reader'},
  {value: WorkspaceAccessLevel.WRITER, label: 'Writer'},
  {value: WorkspaceAccessLevel.OWNER, label: 'Owner'}
];

export interface State {
  autocompleteLoading: boolean;
  autocompleteUsers: User[];
  userNotFound: string;
  workspaceShareError: boolean;
  saving: boolean;
  workspaceFound: boolean;
  workspaceUpdateConflictError: boolean;
  userRoles: UserRole[];
  searchTerm: string;
  dropDown: boolean;
}

export interface Props {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  userEmail: string;
  onClose: Function;
  // The userRoles to pre-populate the share dialog. Must be filled with all
  // pre-existing roles on the workspace for this dialog to work correctly.
  userRoles: UserRole[];
}

export const WorkspaceShare = withCurrentWorkspace()(class extends React.Component<Props, State> {
  searchTermChangedEvent: Function;
  searchingNode: HTMLElement;

  constructor(props: Props) {
    super(props);
    this.state = {
      autocompleteLoading: false,
      autocompleteUsers: [],
      userNotFound: '',
      workspaceShareError: false,
      saving: false,
      workspaceFound: (this.props.workspace !== null),
      workspaceUpdateConflictError: false,
      userRoles: fp.sortBy('familyName', this.props.userRoles),
      searchTerm: '',
      dropDown: false,
    };
    this.searchTermChangedEvent = fp.debounce(300, this.userSearch);
  }

  componentDidMount(): void {
    document.addEventListener('mousedown', this.handleClickOutsideSearch, false);
  }

  componentWillUnmount(): void {
    document.removeEventListener('mousedown', this.handleClickOutsideSearch, false);
  }

  async loadUserRoles() {
    try {
      const resp = await workspacesApi()
        .getFirecloudWorkspaceUserRoles(this.props.workspace.namespace, this.props.workspace.id);
      this.setState({userRoles: fp.sortBy('familyName', resp.items)});
    } catch (error) {
      if (error.status === 404) {
        this.setState({workspaceFound: false});
      }
    }
  }

  save(): void {
    if (this.state.saving) {
      return;
    }
    this.setState({saving: true, workspaceShareError: false});
    workspacesApi().shareWorkspace(this.props.workspace.namespace,
      this.props.workspace.id,
      {workspaceEtag: this.props.workspace.etag, items: this.state.userRoles})
      .then((resp: WorkspaceUserRolesResponse) => {
        currentWorkspaceStore.next({
          ...currentWorkspaceStore.getValue(),
          etag: resp.workspaceEtag,
          userRoles: resp.items
        } as WorkspaceData);
        this.props.onClose();
      }).catch(error => {
        if (error.status === 400) {
          this.setState({userNotFound: error});
        } else if (error.status === 409) {
          this.setState({workspaceUpdateConflictError: true});
        } else {
          this.setState({workspaceShareError: true});
        }
        this.setState({saving: false});
      });
  }

  onCancel(): void {
    this.props.onClose();
  }

  removeCollaborator(user: UserRole): void {
    this.setState({
      userRoles: fp.remove(({email}) => {
        return user.email === email;
      }, this.state.userRoles)});
  }

  addCollaborator(user: User): void {
    const userRole: UserRole = {givenName: user.givenName, familyName: user.familyName,
      email: user.email, role: WorkspaceAccessLevel.READER};
    this.setState({
      searchTerm: '', autocompleteLoading: false, autocompleteUsers: [], dropDown: false,
      userRoles: fp.sortBy('familyName', this.state.userRoles.concat(userRole))});
  }

  userSearch(value: string): void {
    this.setState({autocompleteLoading: true, autocompleteUsers: [], searchTerm: value});
    if (!value.trim()) {
      this.setState({autocompleteLoading: false, dropDown: false});
      return;
    }
    const searchTerm = this.state.searchTerm;
    userApi().user(this.state.searchTerm)
      .then((response) => {
        if (this.state.searchTerm !== searchTerm) {
          return;
        }
        response.users = fp.differenceWith((a, b) => {
          return a.email === b.email;
        }, response.users, this.state.userRoles);
        this.setState({
          autocompleteUsers: response.users.splice(0, 4),
          autocompleteLoading: false,
          dropDown: true
        });
      });
  }

  setRole(e, user): void {
    const oldUserRoles = this.state.userRoles;
    const newUserRoleList = fp.map((u) => {
      return u.email === user.email ? {...u, role: e} : u;
    }, oldUserRoles);
    this.setState({userRoles: newUserRoleList});
  }

  resetModalState(): void {
    this.setState({
      userNotFound: '',
      workspaceShareError: false,
      workspaceUpdateConflictError: false,
      saving: false,
    });
  }

  openDropdown(): void {
    this.setState({dropDown: true});
  }

  closeDropdown(): void {
    this.setState({dropDown: false});
  }

  handleClickOutsideSearch = (e) => {
    if (this.searchingNode) {
      if (this.searchingNode.contains(e.target)) {
        return;
      }
      this.closeDropdown();
    }
  }

  cleanClassNameForSelect(value: string): string {
    return (value + '-user-role').replace(/[@\.]/g, '');
  }

  get hasPermission(): boolean {
    return this.props.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  get showSearchResults(): boolean {
    return !this.state.autocompleteLoading &&
      this.state.autocompleteUsers.length > 0 &&
      !isBlank(this.state.searchTerm) && this.state.dropDown;
  }

  get showAutocompleteNoResults(): boolean {
    return !this.state.autocompleteLoading && (this.state.autocompleteUsers.length === 0) &&
      !isBlank(this.state.searchTerm) && this.state.dropDown;
  }

  render() {
    return <React.Fragment>
      {this.state.workspaceFound &&
      <Modal width={450}>
        {this.state.saving && <SpinnerOverlay />}
        <ModalTitle style={styles.modalTitle}>
          <div>
          <label>Share {this.props.workspace.name}</label>
          <TooltipTrigger content={<div style={styles.tooltipLabel}>
            Search for a collaborator that has an <i>All of Us</i> research account. Collaborators can
            be assigned different permissions within your Workspace.
            <ul>
              <li style={styles.tooltipPoint}>A <span style={{'textDecoration': 'underline'}}>
                Reader</span> can view your notebooks, but not make edits, deletes or share
                contents of the Workspace.</li>
              <li style={styles.tooltipPoint}>A <span style={{'textDecoration': 'underline'}}>
                Writer</span> can view, edit and delete content in the Workspace but not share
                the Workspace with others.</li>
              <li style={styles.tooltipPoint}>An <span style={{'textDecoration': 'underline'}}>
                Owner</span> can view, edit, delete and share contents in the Workspace.</li>
            </ul>
          </div>}>
            <InfoIcon style={{width: '13px', height: '14px', marginLeft: '.4rem'}}/>
          </TooltipTrigger>
          </div>
        </ModalTitle>
        <ModalBody style={styles.sharingBody}>
          <div ref={node => this.searchingNode = node} style={styles.dropdown} >
            <ClrIcon shape='search' style={{width: '21px', height: '21px', paddingLeft: '3px'}}/>
            <input data-test-id='search' style={styles.noBorder} type='text'
                   placeholder='Find Collaborators'
                   disabled={!this.hasPermission}
                   onChange={e => this.searchTermChangedEvent(e.target.value)}
                   onFocus={() => this.openDropdown()}/>
            {this.state.autocompleteLoading && <span style={styles.spinner}/>}
            {this.showAutocompleteNoResults &&
              <div data-test-id='drop-down'
                   style={{...styles.dropdownMenu, ...styles.open, overflowY: 'hidden'}}>
              <div style={{height: '60px'}}>
                <em>No results based on your search</em>
              </div></div>}
            {this.showSearchResults &&
              <div data-test-id='drop-down' style={{...styles.dropdownMenu, ...styles.open}}>
              {this.state.autocompleteUsers.map((user) => {
                return <div key={user.email}><div style={styles.wrapper}>
                  <div style={styles.box}>
                    <h5 style={styles.userName}>{user.givenName} {user.familyName}</h5>
                    <div data-test-id='user-email' style={styles.userName}>{user.email}</div>
                  </div>
                  <div style={styles.collaboratorIcon}>
                    <ClrIcon shape='plus-circle' data-test-id={'add-collab-' + user.email}
                             style={{height: '21px', width: '21px'}}
                             onClick={() => { this.addCollaborator(user); }}/>
                  </div>
                </div>
                  <div style={{borderTop: '1px solid grey', width: '100%', marginTop: '.5rem'}}/>
                </div>; })}
            </div>}
          </div>
          {this.state.userNotFound && <div style={{color: 'red'}}>
            {this.state.userNotFound}
          </div>}
          {this.state.workspaceShareError && <div style={{color: 'red'}}>
            Failed to share workspace. Please try again.
          </div>}
            <h3>Current Collaborators</h3>
          <div style={{overflowY: this.state.dropDown ? 'hidden' : 'auto'}}>
            {this.state.userRoles.map((user, i) => {
              return <div key={user.email}>
                <div style={styles.wrapper}>
                  <div style={styles.box}>
                    <h5 data-test-id='collab-user-name'
                        style={{...styles.userName, ...styles.collabUser}}>
                      {user.givenName} {user.familyName}
                    </h5>
                    <div data-test-id='collab-user-email'
                         style={styles.userName}>{user.email}</div>
                    <Select value={user.role}
                            menuPortalTarget={document.getElementById('popup-root')}
                            isDisabled={user.email === this.props.userEmail}
                            classNamePrefix={this.cleanClassNameForSelect(user.email)}
                            data-test-id={user.email + '-user-role'}
                            onChange={e => this.setRole(e, user)}
                            options={UserRoleOptions}/>
                  </div>
                <div style={styles.box}>
                  <div style={styles.collaboratorIcon}>
                    {(this.hasPermission && (user.email !== this.props.userEmail)) &&
                    <ClrIcon data-test-id={'remove-collab-' + user.email} shape='minus-circle'
                             style={{height: '21px', width: '21px'}}
                             onClick={() => this.removeCollaborator(user)}/>}
                  </div>
                </div>
                </div>
                  {(this.state.userRoles.length !== i + 1) &&
                  <div style={{borderTop: '1px solid grey', width: '100%', marginTop: '.5rem'}}/>}
              </div>;
            })}
          </div>
        </ModalBody>
        <ModalFooter style={{alignItems: 'center'}}>
            <Button type='link' style={{marginRight: '.8rem', border: 'none'}}
                    onClick={() => this.onCancel()}>Cancel</Button>
            <Button type='primary' data-test-id='save' disabled={!this.hasPermission}
                    onClick={() => {
                      AnalyticsTracker.Workspaces.Share();
                      this.save();
                    }}>Save</Button>
        </ModalFooter>
      </Modal>}
      {!this.state.workspaceFound && <div>
        <h3>The workspace {this.props.workspace.id} could not be found</h3></div>}
      {this.state.workspaceUpdateConflictError && <Modal>
        <ModalTitle>Conflicting update:</ModalTitle>
        <ModalBody>
          Another client has modified this workspace since the beginning of this
          sharing session. Please reload to avoid overwriting those changes.
        </ModalBody>
        <ModalFooter>
          <Button onClick={() => this.loadUserRoles()}>Reload Workspace</Button>
          <Button onClick={() => this.onCancel()}>Cancel Sharing</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }
});

@Component({
  selector: 'app-workspace-share',
  template: '<div #root></div>'
})
export class WorkspaceShareComponent extends ReactWrapperBase implements OnInit {
  @Input('workspace') workspace: Workspace;
  @Input('accessLevel') accessLevel: WorkspaceAccessLevel;
  @Input('userEmail') userEmail: Props['userEmail'];
  @Input('onClose') onClose: Props['onClose'];
  @Input('userRoles') userRoles: Props['userRoles'];

  constructor() {
    super(WorkspaceShare, ['workspace', 'accessLevel', 'onClose', 'userEmail', 'userRoles']);
  }

}
