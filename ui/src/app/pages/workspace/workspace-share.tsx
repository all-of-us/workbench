import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Profile,
  User,
  UserRole,
  WorkspaceAccessLevel,
  WorkspaceUserRolesResponse,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon, InfoIcon } from 'app/components/icons';
import { Select } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import { userApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { isBlank, reactStyles, withUserProfile } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

const styles = reactStyles({
  tooltipLabel: {
    paddingBottom: '0.75rem',
    wordWrap: 'break-word',
  },

  modalTitle: {
    margin: '0',
    fontSize: '1.375rem',
    fontWeight: 600,
    paddingBottom: '0.375rem',
  },

  tooltipPoint: {
    listStylePosition: 'outside',
    marginLeft: '0.9rem',
  },

  collabUser: {
    fontSize: '1rem',
    marginTop: '1.5rem',
    letterSpacing: '.01em',
    color: colors.primary,
  },

  dropdown: {
    width: '100%',
    height: '2.625rem',
    marginTop: '0',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.85),
    borderRadius: '5px',
  },

  open: {
    overflow: 'hidden',
    position: 'absolute',
    backgroundColor: colors.white,
    border: '1px solid',
  },

  noBorder: {
    border: 'none',
    background: 'none',
    width: '90%',
    marginTop: '8px',
  },

  spinner: {
    animation: '1s linear infinite spin',
    position: 'relative',
    display: 'inline-block',
    verticalAlign: 'text-bottom',
  },

  dropdownMenu: {
    display: 'block',
    maxHeight: '18rem',
    minHeight: '30px',
    visibility: 'visible',
    overflowY: 'scroll',
    maxWidth: 'calc(100% - 3.375rem)',
    width: '100%',
    marginTop: '.375rem',
    zIndex: 100,
  },

  wrapper: {
    display: 'grid',
    gridTemplateColumns: '13.5rem 0.15rem 0.15rem',
    gridGap: '10px',
    alignItems: 'center',
    height: '6.38%',
  },

  box: {
    borderRadius: '5px',
    paddingTop: '0.3rem',
    paddingLeft: '0.3rem',
  },

  userName: {
    marginTop: '0',
    paddingTop: '0',
    fontSize: '0.875rem',
    height: '4.5%',
    color: colors.primary,
    fontFamily: 'Montserrat',
    lineHeight: '1.5rem',
    whiteSpace: 'nowrap',
  },

  collaboratorIcon: {
    margin: '0 0 0 7.5rem',
    color: colors.accent,
    cursor: 'pointer',
  },

  sharingBody: {
    maxHeight: '70vh',
    overflowY: 'hidden',
    overflowX: 'visible',
    padding: '0 .1875rem',
    display: 'flex',
    flexDirection: 'column',
  },
});

export const UserRoleOptions = [
  { value: WorkspaceAccessLevel.READER, label: 'Reader' },
  { value: WorkspaceAccessLevel.WRITER, label: 'Writer' },
  { value: WorkspaceAccessLevel.OWNER, label: 'Owner' },
];

interface ConflictProps {
  reloadAction: () => void;
  cancelAction: () => void;
}
const ConflictModal = (props: ConflictProps) => (
  <Modal>
    <ModalTitle>Conflicting update:</ModalTitle>
    <ModalBody>
      Another client has modified this workspace since the beginning of this
      sharing session. Please reload to avoid overwriting those changes.
    </ModalBody>
    <ModalFooter>
      <Button onClick={() => props.reloadAction()}>Reload Workspace</Button>
      <Button onClick={() => props.cancelAction()}>Cancel Sharing</Button>
    </ModalFooter>
  </Modal>
);

interface State {
  autocompleteLoading: boolean;
  autocompleteUsers: User[];
  userNotFound: string;
  workspaceShareError: boolean;
  saving: boolean;
  workspaceFound: boolean;
  workspaceUpdateConflictError: boolean;
  loadingUserRoles: boolean;
  userRoles: UserRole[];
  userRolesToChange: UserRole[];
  searchTerm: string;
  dropDown: boolean;
}

export interface WorkspaceShareProps {
  onClose: Function;
  workspace: WorkspaceData;
}

interface HocProps extends WorkspaceShareProps {
  profileState: { profile: Profile; reload: Function; updateCache: Function };
}

export const WorkspaceShare = fp.flow(withUserProfile())(
  class extends React.Component<HocProps, State> {
    searchTermChangedEvent: Function;
    searchingNode: HTMLElement;

    constructor(props: HocProps) {
      super(props);
      this.state = {
        autocompleteLoading: false,
        autocompleteUsers: [],
        userNotFound: '',
        workspaceShareError: false,
        saving: false,
        workspaceFound: this.props.workspace !== null,
        workspaceUpdateConflictError: false,
        loadingUserRoles: true,
        userRoles: [],
        userRolesToChange: [],
        searchTerm: '',
        dropDown: false,
      };
      this.searchTermChangedEvent = fp.debounce(300, this.userSearch);
    }

    componentDidMount(): void {
      document
        .getElementById('root')
        .addEventListener('mousedown', this.handleClickOutsideSearch, false);

      this.loadUserRoles();
    }

    componentWillUnmount(): void {
      document
        .getElementById('root')
        .removeEventListener('mousedown', this.handleClickOutsideSearch, false);
    }

    async loadUserRoles() {
      this.setState({ loadingUserRoles: true });
      try {
        const resp = await workspacesApi().getFirecloudWorkspaceUserRoles(
          this.props.workspace.namespace,
          this.props.workspace.id
        );
        this.setState({
          userRoles: fp.sortBy('familyName', resp.items),
          userRolesToChange: [],
        });
      } catch (error) {
        if (error.status === 404) {
          this.setState({ workspaceFound: false });
        }
      } finally {
        this.setState({ loadingUserRoles: false });
      }
    }

    save(): void {
      if (this.state.saving) {
        return;
      }
      this.setState({ saving: true });
      workspacesApi()
        .shareWorkspacePatch(
          this.props.workspace.namespace,
          this.props.workspace.id,
          {
            workspaceEtag: this.props.workspace.etag,
            items: this.state.userRolesToChange,
          }
        )
        .then((resp: WorkspaceUserRolesResponse) => {
          currentWorkspaceStore.next({
            ...currentWorkspaceStore.getValue(),
            etag: resp.workspaceEtag,
          } as WorkspaceData);
          this.setState({
            userRoles: resp.items,
            userRolesToChange: [],
          });
          this.props.onClose();
        })
        .catch((error) => {
          if (error.status === 400) {
            this.setState({ userNotFound: error });
          } else if (error.status === 409) {
            this.setState({ workspaceUpdateConflictError: true });
          } else {
            this.setState({ workspaceShareError: true });
          }
          this.setState({ saving: false });
        });
    }

    onCancel(): void {
      this.props.onClose();
    }

    removeCollaborator(user: UserRole): void {
      // remove from userRoles if it exists
      const userRoles = this.state.userRoles.filter(
        ({ email }) => user.email !== email
      );

      // may or may not exist in the changeset (may have been added previously)
      const userRolesToChange = this.state.userRolesToChange
        .filter(({ email }) => user.email !== email)
        .concat({
          ...user,
          role: WorkspaceAccessLevel.NO_ACCESS,
        });

      this.setState({
        userRoles,
        userRolesToChange,
      });
    }

    addCollaborator(user: User): void {
      const userRole: UserRole = {
        givenName: user.givenName,
        familyName: user.familyName,
        email: user.email,
        role: WorkspaceAccessLevel.READER,
      };

      const userRoles = fp.sortBy(
        'familyName',
        this.state.userRoles.concat(userRole)
      );

      // may or may not exist in the changeset (may have been set to NOACCESS previously)
      const userRolesToChange = this.state.userRolesToChange
        .filter(({ email }) => user.email !== email)
        .concat(userRole);

      this.setState({
        searchTerm: '',
        autocompleteLoading: false,
        autocompleteUsers: [],
        dropDown: false,
        userRoles,
        userRolesToChange,
      });
    }

    userSearch(value: string): void {
      const { accessTierShortName } = this.props.workspace;
      this.setState({
        autocompleteLoading: true,
        autocompleteUsers: [],
        searchTerm: value,
      });
      if (!value.trim()) {
        this.setState({ autocompleteLoading: false, dropDown: false });
        return;
      }
      const searchTerm = this.state.searchTerm;
      userApi()
        .userSearch(accessTierShortName, this.state.searchTerm)
        .then((response) => {
          if (this.state.searchTerm !== searchTerm) {
            return;
          }
          response.users = fp.differenceWith(
            (a, b) => {
              return a.email === b.email;
            },
            response.users,
            this.state.userRoles
          );
          this.setState({
            autocompleteUsers: response.users.splice(0, 4),
            autocompleteLoading: false,
            dropDown: true,
          });
        });
    }

    setRole(e, user: UserRole): void {
      const userRoles = fp.map((u) => {
        return u.email === user.email ? { ...u, role: e } : u;
      }, this.state.userRoles);

      // may or may not already exist in the changeset
      const userRolesToChange = this.state.userRolesToChange
        .filter(({ email }) => user.email !== email)
        .concat({ ...user, role: e });

      this.setState({
        userRoles,
        userRolesToChange,
      });
    }

    openDropdown(): void {
      this.setState({ dropDown: true });
    }

    closeDropdown(): void {
      this.setState({ dropDown: false });
    }

    handleClickOutsideSearch = (e) => {
      if (this.searchingNode) {
        if (this.searchingNode.contains(e.target)) {
          return;
        }
        this.closeDropdown();
      }
    };

    cleanClassNameForSelect(value: string): string {
      return (value + '-user-role').replace(/[@\.]/g, '');
    }

    get hasPermission(): boolean {
      return this.props.workspace.accessLevel === WorkspaceAccessLevel.OWNER;
    }

    get showSearchResults(): boolean {
      return (
        !this.state.autocompleteLoading &&
        this.state.autocompleteUsers.length > 0 &&
        !isBlank(this.state.searchTerm) &&
        this.state.dropDown
      );
    }

    get showAutocompleteNoResults(): boolean {
      return (
        !this.state.autocompleteLoading &&
        this.state.autocompleteUsers.length === 0 &&
        !isBlank(this.state.searchTerm) &&
        this.state.dropDown
      );
    }

    render() {
      return (
        <React.Fragment>
          {this.state.workspaceFound && (
            <Modal width={450}>
              {this.state.saving && <SpinnerOverlay />}
              <ModalTitle style={styles.modalTitle}>
                <FlexRow
                  style={{
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <label>Share {this.props.workspace.name}</label>
                  </div>
                  <TooltipTrigger
                    content={
                      <div style={styles.tooltipLabel}>
                        Search for a collaborator that has an <AoU /> research
                        account. Collaborators can be assigned different
                        permissions within your Workspace.
                        <ul>
                          <li style={styles.tooltipPoint}>
                            A{' '}
                            <span style={{ textDecoration: 'underline' }}>
                              Reader
                            </span>{' '}
                            can view your notebooks, but not make edits, deletes
                            or share contents of the Workspace.
                          </li>
                          <li style={styles.tooltipPoint}>
                            A{' '}
                            <span style={{ textDecoration: 'underline' }}>
                              Writer
                            </span>{' '}
                            can view, edit and delete content in the Workspace
                            but not share the Workspace with others.
                          </li>
                          <li style={styles.tooltipPoint}>
                            An{' '}
                            <span style={{ textDecoration: 'underline' }}>
                              Owner
                            </span>{' '}
                            can view, edit, delete and share contents in the
                            Workspace.
                          </li>
                        </ul>
                      </div>
                    }
                  >
                    <InfoIcon
                      style={{
                        width: '14px',
                        height: '14px',
                        marginLeft: '.6rem',
                      }}
                    />
                  </TooltipTrigger>
                </FlexRow>
                {isUsingFreeTierBillingAccount(this.props.workspace) && (
                  <div
                    style={{
                      color: colors.primary,
                      fontSize: 14,
                      fontWeight: 400,
                    }}
                  >
                    When you share this workspace as a ‘Writer’ or an ‘Owner’,
                    the initial credits of the creator of the workspace (
                    {this.props.workspace.creator}) will be used for all
                    analysis in this workspace.
                  </div>
                )}
              </ModalTitle>
              <ModalBody style={styles.sharingBody}>
                <div
                  ref={(node) => (this.searchingNode = node)}
                  style={styles.dropdown}
                >
                  <ClrIcon
                    shape='search'
                    style={{
                      width: '21px',
                      height: '21px',
                      paddingLeft: '3px',
                    }}
                  />
                  <input
                    data-test-id='search'
                    style={styles.noBorder}
                    type='text'
                    placeholder='Find Collaborators'
                    disabled={!this.hasPermission}
                    onChange={(e) =>
                      this.searchTermChangedEvent(e.target.value)
                    }
                    onFocus={() => this.openDropdown()}
                  />
                  {this.state.autocompleteLoading && (
                    <span style={styles.spinner} />
                  )}
                  {this.showAutocompleteNoResults && (
                    <div
                      data-test-id='drop-down'
                      style={{
                        ...styles.dropdownMenu,
                        ...styles.open,
                        overflowY: 'hidden',
                      }}
                    >
                      <div style={{ height: '60px' }}>
                        <em>No results based on your search</em>
                      </div>
                    </div>
                  )}
                  {this.showSearchResults && (
                    <div
                      data-test-id='drop-down'
                      style={{ ...styles.dropdownMenu, ...styles.open }}
                    >
                      {this.state.autocompleteUsers.map((user) => {
                        return (
                          <div key={user.email}>
                            <div style={styles.wrapper}>
                              <div style={styles.box}>
                                <h5 style={styles.userName}>
                                  {user.givenName} {user.familyName}
                                </h5>
                                <div
                                  data-test-id='user-email'
                                  style={styles.userName}
                                >
                                  {user.email}
                                </div>
                              </div>
                              <div style={styles.collaboratorIcon}>
                                <ClrIcon
                                  shape='plus-circle'
                                  data-test-id={'add-collab-' + user.email}
                                  style={{ height: '21px', width: '21px' }}
                                  onClick={() => {
                                    this.addCollaborator(user);
                                  }}
                                />
                              </div>
                            </div>
                            <div
                              style={{
                                borderTop: '1px solid grey',
                                width: '100%',
                                marginTop: '.75rem',
                              }}
                            />
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
                {this.state.userNotFound && (
                  <div style={{ color: 'red' }}>{this.state.userNotFound}</div>
                )}
                {this.state.workspaceShareError && (
                  <div style={{ color: 'red' }}>
                    Failed to share workspace. Please try again.
                  </div>
                )}
                <h3>Current Collaborators</h3>
                {this.state.loadingUserRoles ? (
                  <Spinner
                    size={36}
                    style={{ margin: 'auto', marginTop: '1.5rem' }}
                  />
                ) : (
                  <div
                    style={{
                      overflowY: this.state.dropDown ? 'hidden' : 'auto',
                    }}
                  >
                    {this.state.userRoles.map((user, i) => {
                      return (
                        <div key={user.email}>
                          <div
                            data-test-id='collab-user-row'
                            style={styles.wrapper}
                          >
                            <div style={styles.box}>
                              <h5
                                data-test-id='collab-user-name'
                                style={{
                                  ...styles.userName,
                                  ...styles.collabUser,
                                }}
                              >
                                {user.givenName} {user.familyName}
                              </h5>
                              <div
                                data-test-id='collab-user-email'
                                style={styles.userName}
                              >
                                {user.email}
                              </div>
                              {/* Minimally, the z-index must be higher than that of the
                        modal. See https://react-select.com/advanced#portaling */}
                              <Select
                                value={user.role}
                                styles={{
                                  menuPortal: (base) => ({
                                    ...base,
                                    zIndex: 110,
                                  }),
                                }}
                                menuPortalTarget={document.getElementById(
                                  'popup-root'
                                )}
                                isDisabled={
                                  user.email ===
                                  this.props.profileState.profile.username
                                }
                                classNamePrefix={this.cleanClassNameForSelect(
                                  user.email
                                )}
                                data-test-id={user.email + '-user-role'}
                                onChange={(e) => this.setRole(e, user)}
                                options={UserRoleOptions}
                              />
                            </div>
                            <div style={styles.box}>
                              <div style={styles.collaboratorIcon}>
                                {this.hasPermission &&
                                  user.email !==
                                    this.props.profileState.profile
                                      .username && (
                                    <ClrIcon
                                      data-test-id={
                                        'remove-collab-' + user.email
                                      }
                                      shape='minus-circle'
                                      style={{ height: '21px', width: '21px' }}
                                      onClick={() =>
                                        this.removeCollaborator(user)
                                      }
                                    />
                                  )}
                              </div>
                            </div>
                          </div>
                          {this.state.userRoles.length !== i + 1 && (
                            <div
                              style={{
                                borderTop: '1px solid grey',
                                width: '100%',
                                marginTop: '.75rem',
                              }}
                            />
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </ModalBody>
              <ModalFooter style={{ alignItems: 'center' }}>
                <Button
                  type='link'
                  style={{ marginRight: '1.2rem', border: 'none' }}
                  onClick={() => this.onCancel()}
                >
                  Cancel
                </Button>
                <Button
                  type='primary'
                  data-test-id='save'
                  disabled={!this.hasPermission}
                  onClick={() => {
                    AnalyticsTracker.Workspaces.Share();
                    this.save();
                  }}
                >
                  Save
                </Button>
              </ModalFooter>
            </Modal>
          )}
          {!this.state.workspaceFound && (
            <div>
              <h3>
                The workspace {this.props.workspace.id} could not be found
              </h3>
            </div>
          )}
          {this.state.workspaceUpdateConflictError && (
            <ConflictModal
              reloadAction={() => this.loadUserRoles()}
              cancelAction={() => this.onCancel()}
            />
          )}
        </React.Fragment>
      );
    }
  }
);
