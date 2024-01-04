import * as React from 'react';
import { useState } from 'react';

import { AdminWorkspaceResources, ListRuntimeResponse } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { reactStyles } from 'app/utils';
import { getCreator } from 'app/utils/runtime-utils';

import { PurpleLabel } from './workspace-info-field';

const styles = reactStyles({
  wideWithMargin: {
    width: '30rem',
    marginRight: '1.5rem',
  },
  narrowWithMargin: {
    width: '15rem',
    marginRight: '1.5rem',
  },
});

interface Props {
  resources: AdminWorkspaceResources;
  workspaceNamespace: string;
  onDelete: () => void;
}
export const CloudEnvironments = ({
  resources: { runtimes, userApps },
  workspaceNamespace,
  onDelete,
}: Props) => {
  const [runtimeToDelete, setRuntimeToDelete] =
    useState<ListRuntimeResponse>(null);
  const [confirmDeleteRuntime, setConfirmDeleteRuntime] = useState(false);

  const deleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    workspaceAdminApi()
      .deleteRuntimesInWorkspace(workspaceNamespace, {
        runtimesToDelete: [runtimeToDelete.runtimeName],
      })
      .then(() => {
        setRuntimeToDelete(null);
        onDelete();
      });
  };

  const cancelDeleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    setRuntimeToDelete(null);
  };

  const hasCloudEnvironments = runtimes?.length > 0 || userApps?.length > 0;

  return (
    <div>
      {confirmDeleteRuntime && (
        <Modal onRequestClose={cancelDeleteRuntime}>
          <ModalTitle>Delete Runtime</ModalTitle>
          <ModalBody>
            This will immediately delete the given runtime. This will disrupt
            the user's work and may cause data loss.
            <br />
            <br />
            <b>Are you sure?</b>
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={cancelDeleteRuntime}>
              Cancel
            </Button>
            <Button style={{ marginLeft: '0.75rem' }} onClick={deleteRuntime}>
              Delete
            </Button>
          </ModalFooter>
        </Modal>
      )}
      <h2>Cloud Environments</h2>
      {!hasCloudEnvironments ? (
        <p>No active cloud environments exist for this workspace.</p>
      ) : (
        <FlexColumn>
          <FlexRow>
            <PurpleLabel style={styles.narrowWithMargin}>
              Runtime Name
            </PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Creator</PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>
              Created Time
            </PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>
              Last Accessed Time
            </PurpleLabel>
            <PurpleLabel style={styles.narrowWithMargin}>Status</PurpleLabel>
          </FlexRow>
          {runtimes.map((runtime, i) => (
            <FlexRow key={i}>
              <div style={styles.narrowWithMargin}>{runtime.runtimeName}</div>
              <div style={styles.narrowWithMargin}>{getCreator(runtime)}</div>
              <div style={styles.narrowWithMargin}>
                {new Date(runtime.createdDate).toDateString()}
              </div>
              <div style={styles.narrowWithMargin}>
                {new Date(runtime.dateAccessed).toDateString()}
              </div>
              <div style={styles.narrowWithMargin}>{runtime.status}</div>
              <Button
                onClick={() => {
                  setRuntimeToDelete(runtime);
                  setConfirmDeleteRuntime(true);
                }}
                disabled={runtimeToDelete?.runtimeName === runtime.runtimeName}
              >
                Delete
              </Button>
            </FlexRow>
          ))}
        </FlexColumn>
      )}
    </div>
  );
};
