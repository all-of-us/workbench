import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dropdown } from 'primereact/dropdown';

import {
  MigrationState,
  PreprodMigrationRequest,
  PreprodWorkspace,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Error, TextInputWithLabel } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { rwToVwbResearchPurpose } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import {
  profileApi,
  vwbWorkspaceAdminApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';

export const AdminVwbPreprodMigration = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [preprodNamespace, setPreprodNamespace] = useState('');
  const [prodUsername, setProdUsername] = useState('');
  const [bucketName, setBucketName] = useState('');
  const [preprodWorkspaces, setPreprodWorkspaces] =
    useState<PreprodWorkspace[]>(null);
  const [fetchError, setFetchError] = useState(false);
  const [loading, setLoading] = useState(false);
  const [checkingUsername, setCheckingUsername] = useState(false);
  const [validUsername, setValidUsername] = useState(false);
  const [selectedPod, setSelectedPod] = useState('');
  const [pods, setPods] = useState<string[]>([]);
  const [loadingPods, setLoadingPods] = useState(false);
  const [startingMigration, setStartingMigration] = useState(false);
  const [migrationState, setMigrationState] = useState<MigrationState>(
    MigrationState.NOT_STARTED
  );
  const [workspaceToMigrate, setWorkspaceToMigrate] = useState<number>(-1);

  useEffect(() => spinnerProps.hideSpinner(), []);

  const findPreprodWorkspace = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      setPreprodWorkspaces(null);
      const response = await vwbWorkspaceAdminApi().getPreprodWorkspace(
        preprodNamespace
      );
      setPreprodWorkspaces(response);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  };

  const clearForm = () => {
    setWorkspaceToMigrate(-1);
    setProdUsername('');
    setBucketName('');
    setSelectedPod('');
    setValidUsername(false);
    setMigrationState(MigrationState.NOT_STARTED);
  };

  const handlePreprodMigration = async () => {
    try {
      setMigrationState(MigrationState.STARTING);
      setStartingMigration(true);
      // Strip nulls to prevent validation error
      Object.entries(
        preprodWorkspaces[workspaceToMigrate].researchPurpose
      ).forEach(([key, value]) => {
        if (value === null) {
          preprodWorkspaces[workspaceToMigrate].researchPurpose[key] = '';
        }
      });
      const preprodMigrationRequest: PreprodMigrationRequest = {
        preprodWorkspace: preprodWorkspaces[workspaceToMigrate],
        ownerEmail: prodUsername,
        researchPurpose: JSON.stringify(
          rwToVwbResearchPurpose(
            preprodWorkspaces[workspaceToMigrate].researchPurpose
          )
        ),
        sourceBucket: bucketName,
      };
      await vwbWorkspaceAdminApi().migratePreprodWorkspace(
        preprodMigrationRequest
      );
      clearForm();
    } catch (error) {
      console.error(error);
      setMigrationState(MigrationState.FAILED);
    } finally {
      setStartingMigration(false);
    }
  };

  const getPods = async () => {
    setLoadingPods(true);
    try {
      const podsResp = await vwbWorkspaceAdminApi().getPods(prodUsername);
      setPods(podsResp);
      setPods([
        'nph-consortium-users',
        ...(podsResp || []),
        'aou-system-billing-prod',
      ]);
    } catch (error) {
      console.error(error);
    } finally {
      setLoadingPods(false);
    }
  };

  const checkUsername = async () => {
    setCheckingUsername(true);
    try {
      const userResp = await profileApi().isUsernameTaken(prodUsername);
      setValidUsername(userResp.taken);
    } catch (error) {
      console.error(error);
    } finally {
      setCheckingUsername(false);
    }
  };

  return (
    <div style={{ margin: '1.5rem' }}>
      <h2>Migrate Preprod Workspaces to RW 2.0</h2>
      <TextInputWithLabel
        containerStyle={{ display: 'inline-block', marginRight: '1.5rem' }}
        style={{ width: '22.5rem', margin: '1.5rem' }}
        labelText='Workspace namespace'
        value={preprodNamespace}
        onChange={setPreprodNamespace}
        onKeyDown={(event: KeyboardEvent) => {
          if (!!preprodNamespace && event.key === 'Enter') {
            void findPreprodWorkspace();
          }
        }}
      />
      <Button
        style={{ height: '2.25rem' }}
        disabled={!preprodNamespace}
        onClick={findPreprodWorkspace}
      >
        Get Workspaces
      </Button>
      <div style={{ marginTop: '1.5rem' }}>
        {fetchError && (
          <Error>
            Error loading data. Please refresh the page or contact the
            development team.
          </Error>
        )}
        {loading ? (
          <SpinnerOverlay />
        ) : (
          preprodWorkspaces !== null && (
            <DataTable
              paginator
              rows={10}
              emptyMessage='No workspaces found'
              loading={loading}
              value={preprodWorkspaces}
            >
              <Column field='workspaceId' header='ID' />
              <Column field='workspaceNamespace' header='Workspace Namespace' />
              <Column field='displayName' header='Name' />
              <Column
                field='accessTierShortName'
                header='Access Tier'
                style={{ textTransform: 'capitalize' }}
              />
              <Column
                body={(_, { rowIndex }) => (
                  <Button onClick={() => setWorkspaceToMigrate(rowIndex)}>
                    Start Migration
                  </Button>
                )}
              />
            </DataTable>
          )
        )}
      </div>
      {workspaceToMigrate > -1 && (
        <Modal
          data-test-id='vwb-migrate-preprod-workspace-modal'
          aria={{
            label: 'Migrate Preprod Workspace Modal',
          }}
        >
          {/* TITLE */}
          <ModalTitle>Preprod Workspace Details</ModalTitle>

          {/* BODY */}
          <ModalBody>
            <div style={{ color: colors.dark, fontSize: '14px' }}>
              <TextInputWithLabel
                containerStyle={{
                  display: 'inline-block',
                  marginRight: '1.5rem',
                }}
                style={{ width: '22.5rem', margin: '1.5rem' }}
                labelText='Prod username of owner'
                value={prodUsername}
                invalid={!!prodUsername && !checkingUsername && !validUsername}
                onChange={setProdUsername}
                onBlur={checkUsername}
              />
              <TextInputWithLabel
                containerStyle={{
                  display: 'inline-block',
                  marginRight: '1.5rem',
                }}
                style={{ width: '22.5rem', margin: '1.5rem' }}
                labelText='Preprod workspace bucket name'
                value={bucketName}
                onChange={setBucketName}
              />
              <div
                style={{
                  fontSize: '14px',
                  color: colors.primary,
                  fontWeight: 600,
                  marginBottom: '4px',
                }}
              >
                Researcher Workbench 2.0 billing pod
              </div>
              <Dropdown
                value={selectedPod}
                options={pods}
                placeholder={loadingPods ? 'Loading pods...' : 'Select a pod'}
                onChange={(e) => setSelectedPod(e.value)}
                disabled={loadingPods}
                style={{
                  width: '300px',
                  borderRadius: '6px',
                }}
              />
              <Button
                type='primarySmall'
                style={{ marginTop: '8px' }}
                onClick={() => getPods()}
                disabled={
                  startingMigration ||
                  loadingPods ||
                  !prodUsername ||
                  !validUsername ||
                  checkingUsername
                }
              >
                Load user pods
              </Button>
            </div>
          </ModalBody>

          {/* FOOTER */}
          <ModalFooter style={{ justifyContent: 'flex-end', gap: '1rem' }}>
            <Button
              type='secondary'
              onClick={clearForm}
              disabled={startingMigration}
            >
              Cancel
            </Button>

            <Button
              type='primary'
              onClick={() => handlePreprodMigration()}
              disabled={
                startingMigration ||
                !prodUsername ||
                checkingUsername ||
                !validUsername ||
                !bucketName ||
                !selectedPod
              }
            >
              {startingMigration
                ? 'Starting...'
                : migrationState === MigrationState.STARTING
                ? 'Migration in progress'
                : migrationState === MigrationState.FINISHED
                ? 'Migrated'
                : migrationState === MigrationState.FAILED
                ? 'Retry migration'
                : 'Start migration'}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};
