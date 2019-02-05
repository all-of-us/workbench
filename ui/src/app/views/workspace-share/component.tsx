import {Component, Input, OnInit} from '@angular/core';

import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
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
  userNotFound: string;
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
}

export class WorkspaceShare extends React.Component<WorkspaceShareProps, WorkspaceShareState> {

  constructor(props: WorkspaceShareProps) {
    super(props);
    this.state = {
      autocompleteLoading: false,
      autocompleteUsers: [],
      userNotFound: '',
      workspaceShareError: false,
      usersLoading: false,
      userRolesList: this.props.workspace.userRoles || [],
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
        const updatedWorkspace = this.state.workspace;
        updatedWorkspace.etag = resp.workspaceEtag;
        updatedWorkspace.userRoles = resp.items;
        this.setState({usersLoading: false, userNotFound: '',
          searchTerm: '', workspace: updatedWorkspace});
        this.props.closeFunction();
      }).catch(error => {
        if (error.status === 400) {
          this.setState({userNotFound: error});
        } else if (error.status === 409) {
          this.setState({workspaceUpdateConflictError: true});
        } else {
          this.setState({workspaceShareError: true});
        }
        this.setState({usersLoading: false});
      });
  }

  removeCollaborator(user: UserRole): void {
    this.setState(({userRolesList}) => (
      {userRolesList: fp.remove(({email}) => user.email === email, userRolesList)}
    ));
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
    const userRole: UserRole = {givenName: user.givenName, familyName: user.familyName,
      email: user.email, role: WorkspaceAccessLevel.READER};
    this.setState(({userRolesList}) => (
      {searchTerm: '', autocompleteLoading: false, autocompleteUsers: [],
        userRolesList: fp.concat(userRolesList, [userRole])}
    ));
  }

  searchTermChangedEvent($event: string) {
    this.userSearch($event);
  }

  userSearch(value: string): void {
    this.setState({autocompleteLoading: true, autocompleteUsers: [], searchTerm: value});
    if (!value.trim()) {
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
        response.users = fp.differenceWith((a, b) => {
          return a.email === b.email;
        }, response.users, this.state.userRolesList);
        this.setState({
          autocompleteUsers: response.users.splice(0, 4),
          autocompleteLoading: false
        });
      });
  }

  resetModalState(): void {
    this.setState({
      userNotFound: '',
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
            Search for a collaborator that has an All of Us research account. Collaborators can
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
          <div style={styles.dropdown}>
            <ClrIcon shape='search' style={{width: '21px', height: '21px'}}/>
            <input style={styles.noBorder} type='text' placeholder='Find Collaborators'
                   disabled={!this.hasPermission} onChange={
                     e => this.searchTermChangedEvent(e.target.value)}/>
            {/* TODO: US 1/29/19 should use new spinner? */}
            {this.state.autocompleteLoading && <span style={styles.spinner}/>}
            {this.showAutocompleteNoResults && <div style={{...styles.dropdownMenu, ...styles.open,
              overflowY: 'hidden'}}>
              <div style={{height: '60px'}}>
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
                    <ClrIcon shape='plus-circle' data-test-id={'add-collab-' + user.email}
                             style={{height: '21px', width: '21px'}}
                             onClick={() => { this.addCollaborator(user); }}/>
                  </div>
                </div>; })}
            </div>}
          </div>
          {this.state.userNotFound && <div style={{color: 'red'}}>
            {this.state.userNotFound}
          </div>}
          {this.state.workspaceShareError && <div style={{color: 'red'}}>
            Failed to share workspace. Please try again.
          </div>}
          {this.state.usersLoading && <div>
            <span style={styles.spinner}/>
            <span>Loading updated user list...</span>
          </div>}
            <h3>Current Collaborators</h3>
          <div style={{overflowY: 'auto'}}>
            {console.log(JSON.stringify(this.state))}
            {this.state.userRolesList.map((user, i) => {
              return <div key={i}>
                <div style={styles.wrapper}>
                  <div style={styles.box}>
                    <h5 data-test-id='collab-user-name'
                        style={{...styles.userName, ...styles.collabUser}}>
                      {user.givenName} {user.familyName}
                    </h5>
                    <div data-test-id='collab-user-email'
                         style={styles.userName}>{user.email}</div>
                    <label>
                      <select style={{width: '6rem'}}
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
                    <ClrIcon data-test-id={'remove-collab-' + user.email} shape='minus-circle'
                             style={{height: '21px', width: '21px'}}
                             onClick={() => this.removeCollaborator(user)}/>}
                  </div>
                </div>
                </div>
                <div style={{borderTop: '1px solid grey', width: '100%', marginTop: '.5rem'}}/>
              </div>;
            })}
          </div>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'flex-end'}}>
            <Button type='secondary' style={{margin: '.25rem .5rem .25rem 0', border: '0'}}
                    onClick={() => this.props.closeFunction()}>Cancel</Button>
            <Button style={{margin: '.25rem .5rem .25rem 0', backgroundColor: '#2691D0'}}
                    disabled={!this.hasPermission} onClick={() => this.save()}>Save</Button>
          </div>
        </ModalBody>
      </Modal>}
      {!this.state.workspaceFound && <div>
        <h3>The workspace {this.state.workspace.id} could not be found</h3></div>}
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

  constructor() {
    super(WorkspaceShare, ['workspace', 'accessLevel', 'sharing', 'closeFunction', 'userEmail']);
  }

}
