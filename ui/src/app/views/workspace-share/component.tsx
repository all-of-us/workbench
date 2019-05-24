import {Component, Input, OnInit} from '@angular/core';

import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors'
import {isBlank, reactStyles, ReactWrapperBase} from 'app/utils';

import * as fp from 'lodash/fp';
import * as React from 'react';
import Select from 'react-select';

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

const selectStyles = {
  option: (libstyles, {isSelected}) => ({
    ...libstyles,
    lineHeight: '1rem',
    fontSize: '12px',
    color: colors.black[0],
    backgroundColor: isSelected ? '#E0EAF1' : '#FFFFFF'
  }),
  control: (libstyles, {isDisabled}) => ({
    color: isDisabled ? '#7f7f7f' : '#000000',
    display: 'flex',
    height: '1rem',
    fontSize: '12px',
    fontSpacing: '13px'
  }),
  menu: (libstyles) => ({
    ...libstyles,
    top: '-.15rem',
    position: 'absolute',
  }),
  container: (libstyles, {isDisabled}) => ({
    color: isDisabled ? '#7f7f7f' : '#000000',
    borderRadius: '5px',
    border: isDisabled ? 0 : '1px solid #CCCCCC',
    borderWidth: '1px',
    width: '6rem',
    position: 'relative'
  }),
  dropdownIndicator: (libstyles, {isDisabled}) => ({
    ...libstyles,
    color: colors.black[0],
    paddingTop: '12px',
    display: isDisabled ? 'none' : ''
  }),
  indicatorSeparator: () => ({
    display: 'none'
  })
};

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
    height: '1.75rem',
    marginTop: '0',
    backgroundColor: '#E0EAF1',
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
    color: '#888',
    fontFamily: 'Montserrat',
    lineHeight: '1rem',
    whiteSpace: 'nowrap'
  },

  collaboratorIcon: {
    margin: '0 0 0 5rem',
    color: colors.blue[0],
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

export interface WorkspaceShareState {
  autocompleteLoading: boolean;
  autocompleteUsers: User[];
  userNotFound: string;
  workspaceShareError: boolean;
  usersLoading: boolean;
  workspaceFound: boolean;
  workspaceUpdateConflictError: boolean;
  workspace: Workspace;
  searchTerm: string;
  dropDown: boolean;
}

export interface WorkspaceShareProps {
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  userEmail: string;
  onClose: Function;
  sharing: boolean;
}

export class WorkspaceShare extends React.Component<WorkspaceShareProps, WorkspaceShareState> {
  searchTermChangedEvent: Function;
  searchingNode: HTMLElement;

  constructor(props: WorkspaceShareProps) {
    super(props);
    this.state = {
      autocompleteLoading: false,
      autocompleteUsers: [],
      userNotFound: '',
      workspaceShareError: false,
      usersLoading: false,
      workspaceFound: (this.props.workspace !== null),
      workspaceUpdateConflictError: false,
      workspace: this.props.workspace,
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

  save(): void {
    if (this.state.usersLoading) {
      return;
    }
    this.setState({usersLoading: true, workspaceShareError: false});
    workspacesApi().shareWorkspace(this.state.workspace.namespace,
      this.state.workspace.id,
      {workspaceEtag: this.state.workspace.etag, items: this.state.workspace.userRoles})
      .then((resp: ShareWorkspaceResponse) => {
        const updatedWorkspace = {
          ...this.state.workspace,
          etag: resp.workspaceEtag,
          userRoles: resp.items
        };
        this.setState({usersLoading: false, userNotFound: '',
          searchTerm: '', workspace: updatedWorkspace});
        this.props.onClose();
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

  onCancel(): void {
    // Reload the workspace to remove the temp added user/user roles in state variable workspace
    this.reloadWorkspace();
    this.props.onClose();
  }

  removeCollaborator(user: UserRole): void {
    this.setState(({workspace}) => ({
      workspace: {...workspace,
        userRoles: fp.remove(({email}) => {
          return user.email === email;
        }, workspace.userRoles)} as Workspace}
    ));
  }

  reloadWorkspace(): void {
    workspacesApi().getWorkspace(this.state.workspace.namespace, this.state.workspace.id)
      .then((workspaceResponse) => {
        this.setState({workspace: workspaceResponse.workspace});
        this.resetModalState();
      })
      .catch(({status}) => {
        if (status === 404) {
          this.setState({workspaceFound: false});
        }
      });
  }

  addCollaborator(user: User): void {
    const userRole: UserRole = {givenName: user.givenName, familyName: user.familyName,
      email: user.email, role: WorkspaceAccessLevel.READER};
    this.setState(({workspace}) => ({
      searchTerm: '', autocompleteLoading: false, autocompleteUsers: [], dropDown: false,
      workspace: {...workspace, userRoles: fp.concat(workspace.userRoles, [userRole])} as Workspace}
    ));
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
        }, response.users, this.state.workspace.userRoles);
        this.setState({
          autocompleteUsers: response.users.splice(0, 4),
          autocompleteLoading: false,
          dropDown: true
        });
      });
  }

  setRole = (e, user)  => {
    const oldUserRoles = this.state.workspace.userRoles;
    const newUserRoleList = fp.map((u) => {
      return u.email === user.email ? {...u, role: e.value} : u;
    }, oldUserRoles);
    this.setState(({workspace}) => ({
      workspace: {...workspace, userRoles: newUserRoleList} as Workspace}));
  }

  resetModalState(): void {
    this.setState({
      userNotFound: '',
      workspaceShareError: false,
      workspaceUpdateConflictError: false,
      usersLoading: false,
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
          {this.state.usersLoading && <div>
            <span style={styles.spinner}/>
            <span>Loading updated user list...</span>
          </div>}
            <h3>Current Collaborators</h3>
          <div style={{overflowY: this.state.dropDown ? 'hidden' : 'auto'}}>
            {this.state.workspace.userRoles.map((user, i) => {
              return <div key={user.email}>
                <div style={styles.wrapper}>
                  <div style={styles.box}>
                    <h5 data-test-id='collab-user-name'
                        style={{...styles.userName, ...styles.collabUser}}>
                      {user.givenName} {user.familyName}
                    </h5>
                    <div data-test-id='collab-user-email'
                         style={styles.userName}>{user.email}</div>
                    <label>
                      <Select styles={selectStyles} value={fp.find((u) => {
                        return u.value === user.role;
                      }, UserRoleOptions)}
                              menuPortalTarget={document.getElementById('popup-root')}
                              isDisabled={user.email === this.props.userEmail}
                              classNamePrefix={this.cleanClassNameForSelect(user.email)}
                              data-test-id={user.email + '-user-role'}
                              onChange={e => this.setRole(e, user)}
                              options={UserRoleOptions}/>
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
                  {(this.state.workspace.userRoles.length !== i + 1) &&
                  <div style={{borderTop: '1px solid grey', width: '100%', marginTop: '.5rem'}}/>}
              </div>;
            })}
          </div>
        </ModalBody>
        <ModalFooter>
            <Button type='secondary' style={{marginRight: '.8rem', border: 'none'}}
                    onClick={() => this.onCancel()}>Cancel</Button>
            <Button data-test-id='save' disabled={!this.hasPermission}
                    onClick={() => this.save()}>Save</Button>
        </ModalFooter>
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
          <Button onClick={() => this.onCancel()}>Cancel Sharing</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
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
  @Input('onClose') onClose: WorkspaceShareProps['onClose'];

  constructor() {
    super(WorkspaceShare, ['workspace', 'accessLevel', 'sharing', 'onClose', 'userEmail']);
  }

}
