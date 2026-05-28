import * as React from 'react';
import { RouteComponentProps } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import { Workspace, WorkspaceRecoveryStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { SmallHeader } from 'app/components/headers';
import { Spinner } from 'app/components/spinners';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';

interface MatchParams {
  ns: string;
  terraName: string;
}

export const WorkspaceRecovery = fp.flow(withNavigation)(
  class extends React.Component<
    NavigationProps & RouteComponentProps<MatchParams>,
    {
      selectedPod: string;
      requestSubmitted: boolean;
      loading: boolean;
      loadingPods: boolean;
      workspace: Workspace;
      pods: any[];
    }
  > {
    constructor(props) {
      super(props);

      this.state = {
        selectedPod: '',
        requestSubmitted: false,
        loading: true,
        loadingPods: false,
        workspace: undefined,
        pods: [],
      };
    }

    async componentDidMount() {
      const {
        match: {
          params: { ns, terraName },
        },
      } = this.props;

      try {
        this.setState({
          loading: true,
          loadingPods: true,
        });

        const workspaceResponse = await workspacesApi().getWorkspace(
          ns,
          terraName
        );

        const podsResponse = await workspacesApi().getUserPods();

        this.setState({
          workspace: workspaceResponse.workspace,
          pods: podsResponse || [],
          loading: false,
          loadingPods: false,
        });
      } catch (e) {
        console.error('Failed loading recovery data', e);

        this.setState({
          loading: false,
          loadingPods: false,
        });
      }
    }

    submitRecovery = async () => {
      const { selectedPod } = this.state;

      const {
        match: {
          params: { ns, terraName },
        },
      } = this.props;

      if (!selectedPod) {
        return;
      }

      try {
        await workspacesApi().startWorkspaceRecovery(ns, terraName, {
          podId: selectedPod,
        });

        this.setState({
          requestSubmitted: true,
        });
      } catch (e) {
        console.error('Recovery request failed', e);
      }
    };

    render() {
      const { navigate } = this.props;

      const {
        selectedPod,
        requestSubmitted,
        loading,
        loadingPods,
        workspace,
        pods,
      } = this.state;

      if (loading || !workspace) {
        return (
          <FadeBox
            style={{
              padding: '4rem',
              display: 'flex',
              justifyContent: 'center',
            }}
          >
            <Spinner />
          </FadeBox>
        );
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
                    <li>
                      Older dataset versions may be upgraded automatically
                    </li>

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
                  justifyContent: 'space-between',
                  marginBottom: '2rem',
                }}
              >
                <div>
                  <b>Workspace ID</b>

                  <div
                    style={{
                      marginTop: '6px',
                    }}
                  >
                    {workspace.namespace}
                  </div>
                </div>

                <div>
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
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                }}
              >
                <div
                  style={{
                    width: '45%',
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
                </div>

                <div
                  style={{
                    width: '40%',
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
                      onChange={(e) =>
                        this.setState({
                          selectedPod: e.value,
                        })
                      }
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
                    workspace.recoveryState ===
                      WorkspaceRecoveryStatus.RECOVERING
                  }
                  onClick={this.submitRecovery}
                >
                  {workspace.recoveryState ===
                  WorkspaceRecoveryStatus.RECOVERING
                    ? 'RECOVERY IN PROGRESS'
                    : 'REQUEST WORKSPACE RECOVERY'}
                </Button>
              </FlexRow>
            </div>
          </FlexRow>
        </FadeBox>
      );
    }
  }
);
