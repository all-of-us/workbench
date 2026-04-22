import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { VwbGroupDescription, VwbGroupMember } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Error, TextInputWithLabel } from 'app/components/inputs';
import { Modal, ModalBody, ModalFooter, ModalTitle } from 'app/components/modals';
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

  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [addMemberLoading, setAddMemberLoading] = useState(false);
  const [addMemberError, setAddMemberError] = useState('');

  useEffect(() => spinnerProps.hideSpinner(), []);

  useEffect(() => {
    loadGroups();
  }, []);

  const loadGroups = async () => {
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
  };

  const loadMembers = async (groupName: string) => {
    try {
      setMembersError(false);
      setMembersLoading(true);
      setMembers(null);
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
      });
      setShowAddMemberModal(false);
      setNewMemberEmail('');
      loadMembers(selectedGroup.groupName);
    } catch (error) {
      console.error(error);
      setAddMemberError('Failed to add member. Please check the email and try again.');
    } finally {
      setAddMemberLoading(false);
    }
  };

  return (
    <div style={{ margin: '1.5rem' }}>
      <h2>VWB Groups</h2>
      {fetchError && (
        <Error>
          Error loading groups. Please refresh the page or contact the development team.
        </Error>
      )}
      {loading ? (
        <SpinnerOverlay />
      ) : (
        groups !== null && (
          <div style={{ display: 'flex', gap: '1.5rem' }}>
            {/* Groups list */}
            <div style={{ flex: '0 0 40%' }}>
              <div style={headerStyles.formLabel}>Organization Groups</div>
              <DataTable
                paginator
                rows={20}
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
                  body={(group: VwbGroupDescription) => {
                    if (group.managed) {
                      return (
                        <TooltipTrigger
                          content='This group is managed by data access requirements by system. Humans cannot update membership.'
                          side='right'
                        >
                          <span style={{ color: colors.disabled, cursor: 'not-allowed' }}>
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
                        </TooltipTrigger>
                      );
                    }
                    return group.groupName;
                  }}
                />
                <Column
                  field='createdBy'
                  header='Created By'
                  headerStyle={{ width: '200px' }}
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
                  {membersError && (
                    <Error>Error loading members.</Error>
                  )}
                  {membersLoading ? (
                    <SpinnerOverlay />
                  ) : (
                    members !== null && (
                      <DataTable
                        paginator
                        rows={20}
                        emptyMessage='No members found'
                        value={members}
                      >
                        <Column field='email' header='Email' />
                        <Column
                          field='roles'
                          header='Roles'
                          headerStyle={{ width: '200px' }}
                          body={(member: VwbGroupMember) =>
                            member.roles ? member.roles.join(', ') : ''
                          }
                        />
                      </DataTable>
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
                setAddMemberError('');
              }}
              style={{ marginRight: '0.75rem' }}
            >
              Cancel
            </Button>
            <Button
              disabled={!newMemberEmail || addMemberLoading}
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
