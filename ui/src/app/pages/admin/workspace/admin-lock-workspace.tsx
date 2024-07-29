import * as React from 'react';
import { useState } from 'react';

import { Workspace } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
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
    setShowSpinner(false);
  };

  const reloadAfterLocking = async () => {
    setShowSpinner(true);
    setShowLockModal(false);
    await reload();
    setShowSpinner(false);
  };

  const buttonText = cond(
    [showSpinner && workspace.adminLocked, () => 'UNLOCKING WORKSPACE'],
    [showSpinner && !workspace.adminLocked, () => 'LOCKING WORKSPACE'],
    [!showSpinner && workspace.adminLocked, () => 'UNLOCK WORKSPACE'],
    [!showSpinner && !workspace.adminLocked, () => 'LOCK WORKSPACE']
  );
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
            <TooltipTrigger
              disabled={!workspace.featuredCategory}
              content={'Cannot lock published workspace'}
            >
              <Button
                data-test-id='lockUnlockButton'
                type='secondary'
                style={{ border: '2px solid' }}
                disabled={!!workspace.featuredCategory}
                onClick={() => {
                  workspace.adminLocked ? unlock() : setShowLockModal(true);
                }}
              >
                <FlexRow>
                  {showSpinner && (
                    <div style={{ paddingRight: '0.45rem' }}>
                      <Spinner style={{ width: 20, height: 18 }} />
                    </div>
                  )}
                  {buttonText}
                </FlexRow>
              </Button>
            </TooltipTrigger>
          </FlexColumn>
        </FlexRow>
      </h2>
    </>
  );
};
