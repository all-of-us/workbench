import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  VwbAddGroupMemberRequestRoleEnum,
  VwbGroupDescription,
  VwbGroupMember,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Error, TextInputWithLabel } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { vwbGroupAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';

export const AdminVwbGroups = (spinnerProps: WithSpinnerOverlayProps) => {
  const [groups, setGroups] = useState<VwbGroupDescription[]>(null);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(false);

  const [selectedGroup, setSelectedGroup] = useState<VwbGroupDescription>(null);
  const [members, setMembers] = useState<VwbGroupMember[]>(null);
  const [membersLoading, setMembersLoading] = useState(false);
  const [membersError, setMembersError] = useState(false);
  const [memberFilter, setMemberFilter] = useState('');

  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [newMemberRole, setNewMemberRole] =
    useState<VwbAddGroupMemberRequestRoleEnum>(
      VwbAddGroupMemberRequestRoleEnum.MEMBER
    );
  const [newMemberReason, setNewMemberReason] = useState('');
  const [addMemberLoading, setAddMemberLoading] = useState(false);
  const [addMemberError, setAddMemberError] = useState('');

  const [removingMember, setRemovingMember] = useState<string>(null);

  const [debouncedSetMemberFilter] = useState(() =>
    fp.debounce(300, (value: string) => setMemberFilter(value))
  );

  const loadGroups = useCallback(async () => {
    try {
      setFetchError(false);
      setLoading(true);
      const response = await vwbGroupAdminApi().listVwbGroups();
      setGroups(response.groups);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => spinnerProps.hideSpinner(), []);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

  const loadMembers = async (groupName: string) => {
    try {
      setMembersError(false);
      setMembersLoading(true);
      setMembers(null);
      setMemberFilter('');
      const response = await vwbGroupAdminApi().listVwbGroupMembers(groupName);
      setMembers(response.members);
    } catch (error) {
      console.error(error);
      setMembersError(true);
    } finally {
      setMembersLoading(false);
    }
  };

  const handleGroupClick = (group: VwbGroupDescription) => {
    setSelectedGroup(group);
    loadMembers(group.groupName);
  };

  const handleAddMember = async () => {
    try {
      setAddMemberError('');
      setAddMemberLoading(true);
      await vwbGroupAdminApi().addVwbGroupMember(selectedGroup.groupName, {
        email: newMemberEmail,
        role: newMemberRole,
        reason: newMemberReason,
      });
      setShowAddMemberModal(false);
      setNewMemberEmail('');
      setNewMemberRole(VwbAddGroupMemberRequestRoleEnum.MEMBER);
      setNewMemberReason('');
      loadMembers(selectedGroup.groupName);
    } catch (error) {
      console.error(error);
      const errorMessage =
        error instanceof Response
          ? await error.json().then((body) => body?.message).catch(() => null)
          : null;
      setAddMemberError(
        errorMessage || 'Failed to add member. Please check the email and try again.'
      );
    } finally {
      setAddMemberLoading(false);
    }
  };

  const handleRemoveMember = async (email: string) => {
    try {
      setRemovingMember(email);
      await vwbGroupAdminApi().removeVwbGroupMember(selectedGroup.groupName, {
        email,
      });
      setMembers((prev) => prev.filter((m) => m.email !== email));
    } catch (error) {
      console.error(error);
    } finally {
      setRemovingMember(null);
    }
  };

  return (
    <div style={{ margin: '1.5rem' }}>
      <style>{`
        .vwb-groups-table .p-datatable-tbody > tr.p-highlight > td {
          background: #2691D0 !important;
          color: #ffffff !important;
        }
      `}</style>
      <h2>VWB Groups</h2>
      {fetchError && (
        <Error>
          Error loading groups. Please refresh the page or contact the
          development team.
        </Error>
      )}
      {loading ? (
        <SpinnerOverlay />
      ) : (
        groups !== null && (
          <div style={{ display: 'flex', gap: '1.5rem' }}>
            {/* Groups list */}
            <div style={{ flex: '0 0 40%' }} className='vwb-groups-table'>
              <div style={headerStyles.formLabel}>Organization Groups</div>
              <DataTable
                paginator
                rows={50}
                rowsPerPageOptions={[20, 50, 100, 500]}
                emptyMessage='No groups found'
                value={groups}
                selectionMode='single'
                selection={selectedGroup}
                onSelectionChange={(e) => {
                  const group = e.value as VwbGroupDescription;
                  if (group) {
                    handleGroupClick(group);
                  }
                }}
                dataKey='groupName'
              >
                <Column
                  field='groupName'
                  header='Group Name'
                  sortable
                  body={(group: VwbGroupDescription) => {
                    if (group.managed) {
                      return (
                        <span>
                          {group.groupName}
                          <span
                            style={{
                              marginLeft: '0.5rem',
                              fontSize: '0.7rem',
                              backgroundColor: colors.light,
                              padding: '0.15rem 0.4rem',
                              borderRadius: '3px',
                            }}
                          >
                            System Managed
                          </span>
                        </span>
                      );
                    }
                    return group.groupName;
                  }}
                />
                <Column
                  field='createdBy'
                  header='Created By'
                  headerStyle={{ width: '200px' }}
                  sortable
                />
              </DataTable>
            </div>

            {/* Members panel */}
            <div style={{ flex: '1' }}>
              {selectedGroup ? (
                <>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <div style={headerStyles.formLabel}>
                      Members of: {selectedGroup.groupName}
                    </div>
                    {!selectedGroup.managed && (
                      <Button
                        style={{ height: '2.25rem' }}
                        onClick={() => setShowAddMemberModal(true)}
                      >
                        Add Member
                      </Button>
                    )}
                    {selectedGroup.managed && (
                      <TooltipTrigger
                        content='This group is managed by data access requirements by system. Humans cannot update membership.'
                        side='left'
                      >
                        <Button style={{ height: '2.25rem' }} disabled={true}>
                          Add Member
                        </Button>
                      </TooltipTrigger>
                    )}
                  </div>
                  {membersError && <Error>Error loading members.</Error>}
                  {membersLoading ? (
                    <SpinnerOverlay />
                  ) : (
                    members !== null && (
                      <>
                        <input
                          data-test-id='member-search'
                          style={{
                            marginBottom: '.5em',
                            width: '300px',
                          }}
                          type='text'
                          placeholder='Search'
                          onChange={(e) =>
                            debouncedSetMemberFilter(e.target.value)
                          }
                        />
                        <DataTable
                          paginator
                          rows={50}
                          rowsPerPageOptions={[20, 50, 100, 500]}
                          emptyMessage='No members found'
                          value={members}
                          globalFilter={memberFilter}
                        >
                          <Column
                            field='email'
                            header='Email'
                            sortable
                            filterField='email'
                            filterMatchMode='contains'
                          />
                          <Column
                            field='roles'
                            header='Roles'
                            headerStyle={{ width: '150px' }}
                            body={(member: VwbGroupMember) =>
                              member.roles ? member.roles.join(', ') : ''
                            }
                            excludeGlobalFilter
                          />
                          {!selectedGroup.managed && (
                            <Column
                              header='Actions'
                              headerStyle={{ width: '100px' }}
                              excludeGlobalFilter
                              body={(member: VwbGroupMember) => (
                                <Button
                                  type='secondaryLight'
                                  style={{
                                    height: '1.75rem',
                                    fontSize: '0.8rem',
                                    padding: '0 0.5rem',
                                  }}
                                  disabled={removingMember === member.email}
                                  onClick={() =>
                                    handleRemoveMember(member.email)
                                  }
                                >
                                  {removingMember === member.email
                                    ? 'Removing...'
                                    : 'Remove'}
                                </Button>
                              )}
                            />
                          )}
                        </DataTable>
                      </>
                    )
                  )}
                </>
              ) : (
                <div
                  style={{
                    padding: '3rem',
                    textAlign: 'center',
                    color: colors.secondary,
                  }}
                >
                  Select a group to view its members
                </div>
              )}
            </div>
          </div>
        )
      )}

      {/* Add Member Modal */}
      {showAddMemberModal && (
        <Modal onRequestClose={() => setShowAddMemberModal(false)}>
          <ModalTitle>Add Member to {selectedGroup.groupName}</ModalTitle>
          <ModalBody>
            <TextInputWithLabel
              labelText='User Email'
              value={newMemberEmail}
              onChange={setNewMemberEmail}
              style={{ width: '100%' }}
            />
            <div style={{ marginTop: '0.75rem' }}>
              <label
                style={{
                  display: 'block',
                  fontWeight: 600,
                  marginBottom: '0.25rem',
                }}
              >
                Role
              </label>
              <select
                value={newMemberRole}
                onChange={(e) =>
                  setNewMemberRole(
                    e.target.value as VwbAddGroupMemberRequestRoleEnum
                  )
                }
                style={{ width: '100%', padding: '0.5rem' }}
              >
                <option value={VwbAddGroupMemberRequestRoleEnum.MEMBER}>
                  Member
                </option>
                <option value={VwbAddGroupMemberRequestRoleEnum.ADMIN}>
                  Admin
                </option>
              </select>
            </div>
            <TextInputWithLabel
              labelText='Reason'
              value={newMemberReason}
              onChange={setNewMemberReason}
              style={{ width: '100%', marginTop: '0.75rem' }}
            />
            {addMemberError && (
              <div style={{ color: colors.danger, marginTop: '0.5rem' }}>
                {addMemberError}
              </div>
            )}
          </ModalBody>
          <ModalFooter>
            <Button
              type='secondary'
              onClick={() => {
                setShowAddMemberModal(false);
                setNewMemberEmail('');
                setNewMemberRole(VwbAddGroupMemberRequestRoleEnum.MEMBER);
                setNewMemberReason('');
                setAddMemberError('');
              }}
              style={{ marginRight: '0.75rem' }}
            >
              Cancel
            </Button>
            <Button
              disabled={!newMemberEmail || !newMemberReason || addMemberLoading}
              onClick={handleAddMember}
            >
              {addMemberLoading ? 'Adding...' : 'Add'}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};
