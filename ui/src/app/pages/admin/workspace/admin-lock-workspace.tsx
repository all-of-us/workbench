import * as React from 'react';
import { useState } from 'react';

import { Workspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

import { AdminLockRequest } from './admin-lock-request';

interface LockProps {
  workspace: Workspace;
  reload: () => void;
}

export const AdminLockWorkspace = ({ workspace, reload }: LockProps) => {
  const [loadingLockedStatus, setLoadingLockedStatus] = useState(false);
  const [showLockModal, setShowLockModal] = useState(false);

  const unlock = async () => {
    setLoadingLockedStatus(true);
    await workspaceAdminApi()
      .setAdminUnlockedState(workspace.namespace)
      .catch((e) => {
        console.error(e);
      });
    await reload();
    setLoadingLockedStatus(true);
  };

  const closeModalAndReload = async () => {
    setLoadingLockedStatus(true);
    setShowLockModal(false);
    await workspaceAdminApi()
      .setAdminUnlockedState(workspace.namespace)
      .catch((e) => {
        console.error(e);
      });
    await reload();
    setLoadingLockedStatus(false);
  };

  const toggleWorkspaceLock = () => {
    workspace.adminLocked ? unlock() : setShowLockModal(true);
  };

  return (
    <>
      {showLockModal && (
        <AdminLockRequest
          workspace={workspace.namespace}
          onLock={closeModalAndReload}
          onCancel={() => setShowLockModal(false)}
        />
      )}
      <h2>
        <FlexRow style={{ justifyContent: 'space-between' }}>
          <FlexColumn style={{ justifyContent: 'flex-start' }}>
            Workspace
          </FlexColumn>
          <FlexColumn
            style={{ justifyContent: 'flex-end', marginRight: '4.5rem' }}
          >
            <Button
              data-test-id='lockUnlockButton'
              type='secondary'
              style={{ border: '2px solid' }}
              onClick={toggleWorkspaceLock}
            >
              <FlexRow>
                <div style={{ paddingRight: '0.45rem' }}>
                  {loadingLockedStatus && (
                    <Spinner style={{ width: 20, height: 18 }} />
                  )}
                </div>
                {workspace.adminLocked ? 'UNLOCK WORKSPACE' : 'LOCK WORKSPACE'}
              </FlexRow>
            </Button>
          </FlexColumn>
        </FlexRow>
      </h2>
    </>
  );
};
