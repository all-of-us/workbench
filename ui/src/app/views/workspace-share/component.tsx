import {Component, Input, OnInit} from '@angular/core';

import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {isBlank, reactStyles, ReactWrapperBase} from 'app/utils';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {User} from 'generated';

import {
  ShareWorkspaceResponse,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
} from 'generated/fetch/api';

import {Button} from 'app/components/buttons';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';

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
    color: '#565656'
  },

  dropdown: {
    width: '100%',
    marginTop: '0',
    borderBottom: '1px solid gray',
    paddingBottom: '.5rem',
  },

  open: {
    overflow: 'hidden',
    position: 'absolute',
    backgroundColor: '#FFFFFF',
    border: '1px solid'
  },

  noBorder: {
    border: 'none',
    background: 'none',
    width: '90%'
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
    maxWidth: '16rem',
    width: '100%',
    marginTop: '.25rem'
  },

  dropdownItem: {
    height: '60px'
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
    color: '#888',
    fontFamily: 'Montserrat',
    lineHeight: '1rem',
    whiteSpace: 'nowrap'
  },

  collaboratorIcon: {
    margin: '0 0 0 4rem',
    color: '#2691D0',
    cursor: 'pointer'
  },

  errorMessage: {
    color: 'red',
  },

  roles: {
    width: '6rem'
  },

  sharingBody: {
    maxHeight: '70vh',
    overflowY: 'hidden',
    overflowX: 'visible',
    padding: '0 .125rem',
    display: 'flex',
    flexDirection: 'column'
  }

});

export interface WorkspaceShareState {
  autocompleteLoading: boolean;
  autocompleteUsers: User[];
  userNotFound: boolean;
  toShare: string;
  workspaceShareError: boolean;
  usersLoading: boolean;
  userRolesList: UserRole[];
  workspaceFound: boolean;
  workspaceUpdateConflictError: boolean;
  workspace: Workspace;
  searchTerm: string;
}

export interface WorkspaceShareProps {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  userEmail: string;
  closeFunction: Function;
  sharing: boolean;
  onInit: () => void;
}
export class WorkspaceShare extends React.Component<WorkspaceShareProps, WorkspaceShareState> {

  constructor(props: WorkspaceShareProps) {
    super(props);
    this.state = {
      autocompleteLoading: false,
      autocompleteUsers: [],
      userNotFound: false,
      toShare: '',
      workspaceShareError: false,
      usersLoading: false,
      userRolesList: this.props.workspace.userRoles,
      workspaceFound: (this.props.workspace !== null),
      workspaceUpdateConflictError: false,
      workspace: this.props.workspace,
      searchTerm: '',
    };
    this.searchTermChangedEvent = fp.debounce(300, this.userSearch);
  }

  save(): void {
    if (this.state.usersLoading) {
      return;
    }
    this.setState({usersLoading: true, workspaceShareError: false});
    workspacesApi().shareWorkspace(this.state.workspace.namespace,
      this.state.workspace.id,
      {workspaceEtag: this.state.workspace.etag, items: this.state.workspace.userRoles})
      .then((resp: ShareWorkspaceResponse) => {
        this.setState({usersLoading: false, toShare: '', searchTerm: ''});
        this.setState(({workspace}) =>
          ({workspace: fp.set('etag', resp.workspaceEtag, workspace)}));
        this.setState(({workspace}) => ({workspace: fp.set('userRoles', resp.items, workspace)}));
        this.props.closeFunction();
      }).catch(error => {
      if (error.status === 400) {
        this.setState({userNotFound: true});
      } else if (error.status === 409) {
        this.setState({workspaceUpdateConflictError: true});
      } else {
        this.setState({workspaceShareError: true});
      }
      this.setState({usersLoading: false});
      });
  }

  removeCollaborator(user: UserRole): void {
    if (!this.state.usersLoading) {
      this.setState({usersLoading: true});
      const position = this.state.userRolesList.findIndex((userRole) => {
        return user.email === userRole.email;
      });
      this.state.userRolesList.splice(position, 1);
      this.setState({userRolesList: this.state.userRolesList, usersLoading: false});
    }
  }

  reloadWorkspace(): void {
    workspacesApi().getWorkspace(this.state.workspace.namespace, this.state.workspace.id)
      .then((workspaceResponse) => {
        this.setState({workspace: workspaceResponse.workspace});
        this.resetModalState();
      })
      .catch(error => {
        if (error.status === 404) {
          this.setState({workspaceFound: false});
        }
      });
  }

  addCollaborator(user: User): void {
    this.setState({toShare: user.email, searchTerm: '',
      autocompleteLoading: false, autocompleteUsers: []});
    const userRole: UserRole = {
      givenName: user.givenName,
      familyName: user.familyName,
      email: user.email,
      role: WorkspaceAccessLevel.READER
    };
    this.state.userRolesList.splice(0, 0, userRole);
    this.setState({userRolesList: this.state.userRolesList});
  }

  searchTermChangedEvent($event: string) {
    this.userSearch($event);
  }

  userSearch(value: string): void {
    this.setState({autocompleteLoading: true, autocompleteUsers: [], searchTerm: value});
    if (!this.state.searchTerm.trim()) {
      this.setState({autocompleteLoading: false});
      return;
    }
    const searchTerm = this.state.searchTerm;
    userApi().user(this.state.searchTerm)
      .then((response) => {
        if (this.searchTerm !== searchTerm) {
          return;
        }
        this.setState({autocompleteLoading: false});
        const userResponse = response;
        userResponse.users = fp.uniqBy(user =>
          [user.email, user.familyName, user.givenName].join(), response.users);
        this.setState({autocompleteUsers: userResponse.users.splice(0,4)});
      });
  }

  resetModalState(): void {
    this.setState({
      userNotFound: false,
      workspaceShareError: false,
      workspaceUpdateConflictError: false,
      usersLoading: false,
    });
  }

  get hasPermission(): boolean {
    return this.props.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  get showSearchResults(): boolean {
    return !this.state.autocompleteLoading &&
      this.state.autocompleteUsers.length > 0 &&
      !isBlank(this.state.searchTerm);
  }

  get showAutocompleteNoResults(): boolean {
    return !this.state.autocompleteLoading && (this.state.autocompleteUsers.length === 0) &&
      !isBlank(this.state.searchTerm);
  }

  render() {
    return <div>
      {this.state.workspaceFound && this.props.sharing &&
      <Modal width={450}>
        <ModalTitle style={styles.modalTitle}>
          <div>
          <label>Share {this.props.workspace.name}</label>
          <TooltipTrigger content={<div style={styles.tooltipLabel}>
            Search for a collaborator that has an All of Us research account. Collaborators can be assigned different permissions within your Workspace.
            <ul>
              <li style={styles.tooltipPoint}>A <span style={{'textDecoration': 'underline'}}>Reader</span> can view your notebooks, but not make edits, deletes or share contents of the Workspace.</li>
              <li style={styles.tooltipPoint}>A <span style={{'textDecoration': 'underline'}}>Writer</span> can view, edit and delete content in the Workspace but not share the Workspace with others.</li>
              <li style={styles.tooltipPoint}>An <span style={{'textDecoration': 'underline'}}>Owner</span> can view, edit, delete and share contents in the Workspace.</li>
            </ul>
          </div>}>
            <InfoIcon style={{width: '13px', height: '14px', marginLeft: '.4rem'}}/>
          </TooltipTrigger>
          </div>
        </ModalTitle>
        <ModalBody style={styles.sharingBody}>
          <div style={styles.dropdown}>
            <ClrIcon shape='search' style={{width: '21px', height: '21px'}}/>
            <input style={styles.noBorder} type='text' placeholder='Find Collaborators'
                   disabled={!this.hasPermission} onChange={e => this.searchTermChangedEvent(e.target.value)}/>
            {/* TODO: US 1/29/19 should use new spinner? */}
            {this.state.autocompleteLoading && <span style={styles.spinner}/>}
            {this.showAutocompleteNoResults && <div style={{...styles.dropdownMenu, ...styles.open, overflowY: 'hidden'}}>
              <div style={styles.dropdownItem}>
                <em>No results based on your search</em>
              </div></div>}
            {this.showSearchResults && <div style={{...styles.dropdownMenu, ...styles.open}}>
              {this.state.autocompleteUsers.map((user, i) => {
                return <div style={styles.wrapper} key={i}>
                  <div style={styles.box}>
                    <h5 style={styles.userName}>{user.givenName} {user.familyName}</h5>
                    <div style={styles.userName}>{user.email}</div>
                  </div>
                  <div style={styles.collaboratorIcon}>
                    <ClrIcon shape='plus-circle' style={{height: '21px', width: '21px'}} onClick={() => {this.addCollaborator(user)}}/>
                  </div>
                </div>;})}
            </div>}
          </div>
          {this.state.userNotFound && <div style={styles.errorMessage}>
            User {this.state.toShare} not found.
          </div>}
          {this.state.workspaceShareError && <div style={styles.errorMessage}>
            Failed to share workspace. Please try again.
          </div>}
          {this.state.usersLoading && <div>
            <span style={styles.spinner}/>
            <span>Loading updated user list...</span>
          </div>}
            <h3>Current Collaborators</h3>
          <div style={{overflowY: 'auto'}}>
            {this.state.userRolesList.map((user, i) => {
              return <div key={i}>
                <div style={styles.wrapper}>
                  <div style={styles.box}>
                    <h5 style={{...styles.userName, ...styles.collabUser}}>{user.givenName} {user.familyName}</h5>
                    <div style={styles.userName}>{user.email}</div>
                    <label>
                      <select style={styles.roles}
                              disabled={!this.hasPermission || user.email === this.props.userEmail}>
                        <option value='READER'>Reader</option>
                        <option value='WRITER'>Writer</option>
                        <option value='OWNER'>Owner</option>
                      </select>
                    </label>
                  </div>
                <div style={styles.box}>
                  <div style={styles.collaboratorIcon}>
                    {(this.hasPermission && (user.email !== this.props.userEmail)) &&
                    <ClrIcon shape='minus-circle' style={{height: '21px', width: '21px'}} onClick={() => this.removeCollaborator(user)}/>
                    }
                  </div>
                </div>
                </div>
                <div style={{borderTop: '1px solid grey', width: '100%', marginTop: '.5rem'}}/>
              </div>;
            })}
          </div>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'flex-end'}}>
            <Button type='secondary' style={{margin: '.25rem .5rem .25rem 0', border: '0'}} onClick={() => this.props.closeFunction()}>Cancel</Button>
            <Button style={{margin: '.25rem .5rem .25rem 0', backgroundColor: '#2691D0'}} disabled={!this.hasPermission} onClick={() => this.save()}>Save</Button>
          </div>
        </ModalBody>
      </Modal>}
      {/* TODO: fill in workspace name here once routing is available to React*/}
      {/* ?? Possibly work when nav bar conversion done?? */}
      {!this.state.workspaceFound && <div><h3>Workspace could not be found</h3></div>}
      {this.state.workspaceUpdateConflictError && <Modal>
        <ModalTitle>Conflicting update:</ModalTitle>
        <ModalBody>
          Another client has modified this workspace since the beginning of this
          sharing session. Please reload to avoid overwriting those changes.
        </ModalBody>
        <ModalFooter>
          <Button onClick={() => this.reloadWorkspace()}>Reload Workspace</Button>
          <Button onClick={() => this.props.closeFunction()}>Cancel Sharing</Button>
        </ModalFooter>
      </Modal>}
    </div>;
  }
}

@Component({
  selector: 'app-workspace-share',
  template: '<div #root></div>'
})
export class WorkspaceShareComponent extends ReactWrapperBase implements OnInit {
  @Input('workspace') workspace: Workspace;
  @Input('accessLevel') accessLevel: WorkspaceAccessLevel;
  @Input('userEmail') userEmail: WorkspaceShareProps['userEmail'];
  @Input('sharing') sharing: boolean;
  @Input('closeFunction') closeFunction: WorkspaceShareProps['closeFunction'];

  constructor(public profileStorageService: ProfileStorageService) {
    super(WorkspaceShare, ['workspace', 'accessLevel', 'sharing', 'closeFunction', 'userEmail']);
  }

}
