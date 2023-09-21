import * as React from 'react';
import { useState } from 'react';

import { Workspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

import { AdminLockModal } from './admin-lock-modal';

interface LockProps {
  workspace: Workspace;
  reload: () => Promise<void>;
}
export const AdminLockWorkspace = ({ workspace, reload }: LockProps) => {
  const [showSpinner, setShowSpinner] = useState(false);
  const [showLockModal, setShowLockModal] = useState(false);

  const unlock = async () => {
    setShowSpinner(true);
    await workspaceAdminApi()
      .setAdminUnlockedState(workspace.namespace)
      .catch((e) => {
        console.error(e);
      });
    await reload();
    setShowSpinner(true);
  };

  const reloadAfterLocking = async () => {
    setShowSpinner(true);
    setShowLockModal(false);
    await reload();
    setShowSpinner(false);
  };

  return (
    <>
      {showLockModal && (
        <AdminLockModal
          workspaceNamespace={workspace.namespace}
          onLock={reloadAfterLocking}
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
              onClick={() => {
                workspace.adminLocked ? unlock() : setShowLockModal(true);
              }}
            >
              <FlexRow>
                <div style={{ paddingRight: '0.45rem' }}>
                  {showSpinner && <Spinner style={{ width: 20, height: 18 }} />}
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
