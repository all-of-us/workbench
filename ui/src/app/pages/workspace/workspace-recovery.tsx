import * as React from 'react';
import { useEffect, useState } from 'react';
import { Dropdown } from 'primereact/dropdown';

import { WorkspaceRecoveryStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { SmallHeader } from 'app/components/headers';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

interface Props {
  workspace: WorkspaceData;
}

export const WorkspaceRecovery = withCurrentWorkspace()(
  ({ workspace }: Props) => {
    const [navigate] = useNavigation();

    const [selectedPod, setSelectedPod] = useState('');
    const [requestSubmitted, setRequestSubmitted] = useState(false);
    const [loadingPods, setLoadingPods] = useState(false);
    const [pods, setPods] = useState([]);

    useEffect(() => {
      const loadData = async () => {
        try {
          setLoadingPods(true);

          const podsResponse = await workspacesApi().getUserPods();

          setPods(podsResponse || []);
        } catch (e) {
          console.error('Failed loading recovery data', e);
        } finally {
          setLoadingPods(false);
        }
      };

      loadData();
    }, []);

    const { cdrVersionsForMigration } = serverConfigStore.get().config;

    const showDatasetUpgradeWarning = !cdrVersionsForMigration.some(
      (c) => +workspace.cdrVersionId === c.cdrVersionId
    );

    const submitRecovery = async () => {
      if (!selectedPod) {
        return;
      }

      try {
        await workspacesApi().startWorkspaceRecovery(
          workspace.namespace,
          workspace.terraName,
          {
            podId: selectedPod,
          }
        );

        setRequestSubmitted(true);
      } catch (e) {
        console.error('Recovery request failed', e);
      }
    };

    if (!workspace) {
      return null;
    }

    if (requestSubmitted) {
      return (
        <FadeBox
          style={{
            display: 'flex',
            justifyContent: 'center',
            padding: '6rem',
          }}
        >
          <div
            style={{
              width: '720px',
              background: 'white',
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '2rem',
              boxShadow: '0 2px 8px rgba(0,0,0,.08)',
            }}
          >
            <div
              style={{
                fontSize: '28px',
                fontWeight: 600,
                marginBottom: '1rem',
              }}
            >
              Workspace Recovery Request Submitted
            </div>

            <div
              style={{
                marginBottom: '2rem',
                fontSize: '14px',
              }}
            >
              Thank you for submitting a request to recover your archived
              workspace.
              <br />
              <br />
              Your request will take 5–7 business days and a notification will
              be sent once completed.
            </div>

            <Button type='primary' onClick={() => navigate(['workspaces'])}>
              RETURN TO MY WORKSPACES
            </Button>
          </div>
        </FadeBox>
      );
    }

    return (
      <FadeBox
        style={{
          padding: '2rem',
        }}
      >
        <FlexRow
          style={{
            gap: '2rem',
            alignItems: 'flex-start',
          }}
        >
          {/* Left */}

          <div
            style={{
              width: '340px',
              border: '1px solid #ddd',
              borderRadius: '8px',
              background: 'white',
              padding: '1.5rem',
            }}
          >
            <SmallHeader>Recovering Your Workspace</SmallHeader>

            <div
              style={{
                marginTop: '1rem',
                lineHeight: '24px',
              }}
            >
              <b>How It Works</b>

              <ol
                style={{
                  paddingLeft: '1rem',
                }}
              >
                <li>Review workspace details</li>

                <li>Select billing pod</li>

                <li>Request workspace recovery</li>
              </ol>

              <div
                style={{
                  marginTop: '1rem',
                }}
              >
                <b>What Happens Next</b>

                <ul
                  style={{
                    paddingLeft: '1rem',
                  }}
                >
                  <li>Older dataset versions may be upgraded automatically</li>

                  <li>You’ll receive notification once recovery completes</li>

                  <li>Storage costs resume after workspace recovery</li>
                </ul>
              </div>
            </div>
          </div>

          {/* Right */}

          <div
            style={{
              flex: 1,
              border: '1px solid #ddd',
              borderRadius: '8px',
              background: 'white',
              padding: '1.5rem',
            }}
          >
            <SmallHeader>Recover Workspace From Storage</SmallHeader>

            <div
              style={{
                marginTop: '1rem',
                marginBottom: '2rem',
              }}
            >
              This workspace and its data are currently archived in storage.
            </div>

            <FlexRow
              style={{
                justifyContent: 'space-between',
                marginBottom: '2rem',
              }}
            >
              <div>
                <b>Workspace Name</b>

                <div
                  style={{
                    marginTop: '6px',
                  }}
                >
                  {workspace.name}
                </div>
              </div>
            </FlexRow>

            <FlexRow
              style={{
                gap: '2rem',
                marginBottom: '2rem',
              }}
            >
              <div
                style={{
                  flex: 1,
                }}
              >
                <b>Workspace ID</b>

                <div
                  style={{
                    marginTop: '6px',
                  }}
                >
                  {workspace.namespace}
                </div>
              </div>

              <div
                style={{
                  flex: 1,
                }}
              >
                <b>Workspace Creator</b>

                <div
                  style={{
                    marginTop: '6px',
                  }}
                >
                  {workspace.creatorUser?.userName}
                </div>
              </div>
            </FlexRow>

            <FlexRow
              style={{
                gap: '2rem',
                alignItems: 'flex-start',
              }}
            >
              <div
                style={{
                  flex: 1,
                }}
              >
                <b>Workspace Dataset Version</b>

                <div
                  style={{
                    marginTop: '6px',
                  }}
                >
                  {workspace.accessTierShortName}
                </div>
                {showDatasetUpgradeWarning && (
                  <div
                    style={{
                      marginTop: '12px',
                      padding: '12px',
                      background: '#fff2df',
                      borderRadius: '6px',
                      fontSize: '13px',
                    }}
                  >
                    This dataset version is no longer supported.
                    <br />
                    <br />
                    Upon recovery, the workspace will automatically upgrade to
                    the newest dataset version.
                  </div>
                )}
              </div>
              <div
                style={{
                  flex: 1,
                }}
              >
                <b>Select Billing Pod</b>

                <div
                  style={{
                    marginTop: '8px',
                  }}
                >
                  <Dropdown
                    value={selectedPod}
                    options={pods}
                    optionLabel='userFacingId'
                    optionValue='podId'
                    placeholder={
                      loadingPods ? 'Loading pods...' : 'Select a pod'
                    }
                    onChange={(e) => setSelectedPod(e.value)}
                    disabled={loadingPods}
                    style={{
                      width: '100%',
                      borderRadius: '6px',
                    }}
                  />
                </div>
              </div>
            </FlexRow>

            <FlexRow
              style={{
                marginTop: '2rem',
                gap: '12px',
              }}
            >
              <Button onClick={() => navigate(['workspaces'])}>CANCEL</Button>

              <Button
                type='primary'
                disabled={
                  !selectedPod ||
                  workspace.recoveryState === WorkspaceRecoveryStatus.RECOVERING
                }
                onClick={() => void submitRecovery()}
              >
                {workspace.recoveryState === WorkspaceRecoveryStatus.RECOVERING
                  ? 'RECOVERY IN PROGRESS'
                  : 'REQUEST WORKSPACE RECOVERY'}
              </Button>
            </FlexRow>
          </div>
        </FlexRow>
      </FadeBox>
    );
  }
);
