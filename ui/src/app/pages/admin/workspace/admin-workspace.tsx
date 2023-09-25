import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import { CloudStorageTraffic, WorkspaceAdminView } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Error as ErrorDiv } from 'app/components/inputs';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AdminLockRequest } from 'app/pages/admin/admin-lock-request';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { DisksTable } from 'app/pages/admin/workspace/disks-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { hasNewValidProps } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

import { BasicInformation } from './basic-information';
import { CloudStorageObjects } from './cloud-storage-objects';
import { CloudStorageTrafficChart } from './cloud-storage-traffic-chart';
import { CohortBuilder } from './cohort-builder';
import { Collaborators } from './collaborators';
import { ResearchPurposeSection } from './research-purpose-section';
import { Runtimes } from './runtimes';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

interface State {
  workspaceDetails?: WorkspaceAdminView;
  cloudStorageTraffic?: CloudStorageTraffic;
  loadingData?: boolean;
  loadingWorkspaceAdminLockedStatus: boolean;
  dataLoadError?: Response;
  showLockWorkspaceModal: boolean;
}

export class AdminWorkspaceImpl extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      workspaceDetails: {},
      cloudStorageTraffic: null,
      loadingWorkspaceAdminLockedStatus: false,
      showLockWorkspaceModal: false,
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    this.populateFederatedWorkspaceInformation();
  }

  componentDidUpdate(prevProps) {
    if (hasNewValidProps(this.props, prevProps, [(p) => p.match.params])) {
      this.populateFederatedWorkspaceInformation();
    }
  }

  async populateFederatedWorkspaceInformation() {
    const { ns } = this.props.match.params;
    this.setState({
      loadingData: true,
    });

    try {
      // Fire off all requests in parallel
      const workspaceDetailsPromise =
        workspaceAdminApi().getWorkspaceAdminView(ns);
      const cloudStorageTrafficPromise =
        workspaceAdminApi().getCloudStorageTraffic(ns);
      // Wait for all promises to complete before updating state.
      const [workspaceDetails, cloudStorageTraffic] = await Promise.all([
        workspaceDetailsPromise,
        cloudStorageTrafficPromise,
      ]);
      this.setState({ workspaceDetails, cloudStorageTraffic });
    } catch (error) {
      if (error instanceof Response) {
        console.log('error', error, await error.json());
        this.setState({ dataLoadError: error });
      }
    } finally {
      this.setState({ loadingData: false });
    }
  }

  async unLockWorkspace() {
    const {
      workspaceDetails: { workspace },
    } = this.state;
    try {
      this.setState({ loadingWorkspaceAdminLockedStatus: true });
      await workspaceAdminApi().setAdminUnlockedState(workspace.namespace);
      await this.populateFederatedWorkspaceInformation();
      this.setState({ loadingWorkspaceAdminLockedStatus: false });
    } catch (error) {
      console.log(error);
    }
  }

  async closeLockModalAndReloadWorkspaceStatus() {
    this.setState({
      loadingWorkspaceAdminLockedStatus: true,
      showLockWorkspaceModal: false,
    });
    await this.populateFederatedWorkspaceInformation();
    this.setState({ loadingWorkspaceAdminLockedStatus: false });
  }

  lockUnlockWorkspace(adminLocked: boolean) {
    adminLocked
      ? this.unLockWorkspace()
      : this.setState({ showLockWorkspaceModal: true });
  }

  render() {
    const {
      cloudStorageTraffic,
      loadingData,
      dataLoadError,
      workspaceDetails: { collaborators, resources, workspace },
      showLockWorkspaceModal,
      loadingWorkspaceAdminLockedStatus,
    } = this.state;
    return (
      <div style={{ margin: '1.5rem' }}>
        {showLockWorkspaceModal && (
          <AdminLockRequest
            workspace={workspace.namespace}
            onLock={() => {
              this.closeLockModalAndReloadWorkspaceStatus();
            }}
            onCancel={() => {
              this.setState({ showLockWorkspaceModal: false });
            }}
          />
        )}
        {dataLoadError && (
          <ErrorDiv>
            Error loading data. Please refresh the page or contact the
            development team.
          </ErrorDiv>
        )}
        {loadingData && <SpinnerOverlay />}

        {workspace && (
          <div>
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
                    onClick={() =>
                      this.lockUnlockWorkspace(workspace.adminLocked)
                    }
                  >
                    <FlexRow>
                      <div style={{ paddingRight: '0.45rem' }}>
                        {loadingWorkspaceAdminLockedStatus && (
                          <Spinner style={{ width: 20, height: 18 }} />
                        )}
                      </div>
                      {workspace.adminLocked
                        ? 'UNLOCK WORKSPACE'
                        : 'LOCK WORKSPACE'}
                    </FlexRow>
                  </Button>
                </FlexColumn>
              </FlexRow>
            </h2>
            <BasicInformation {...{ workspace }} />
            <Collaborators {...{ collaborators }} />
            <CohortBuilder workspaceObjects={resources.workspaceObjects} />
            <CloudStorageObjects
              workspaceNamespace={workspace.namespace}
              cloudStorage={resources.cloudStorage}
            />
            <ResearchPurposeSection
              researchPurpose={workspace.researchPurpose}
            />
          </div>
        )}

        {cloudStorageTraffic?.receivedBytes && (
          <CloudStorageTrafficChart {...{ cloudStorageTraffic }} />
        )}

        {resources && (
          <Runtimes
            {...{ resources }}
            workspaceNamespace={workspace.namespace}
            onDelete={() => this.populateFederatedWorkspaceInformation()}
          />
        )}
        {workspace && (
          <>
            <h2>Egress event history</h2>
            <EgressEventsTable
              displayPageSize={10}
              sourceWorkspaceNamespace={workspace.namespace}
            />
            <h2>Disks</h2>
            <DisksTable sourceWorkspaceNamespace={workspace.namespace} />
          </>
        )}
      </div>
    );
  }
}

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
