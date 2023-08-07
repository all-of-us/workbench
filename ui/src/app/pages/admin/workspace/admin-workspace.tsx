import * as React from 'react';
import { ReactFragment } from 'react';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';
import * as HighCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import {
  CloudStorageTraffic,
  ListRuntimeResponse,
  WorkspaceAdminView,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Error as ErrorDiv } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AdminLockRequest } from 'app/pages/admin/admin-lock-request';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { DisksTable } from 'app/pages/admin/workspace/disks-table';
import {
  FileDetailsTable,
  formatMB,
} from 'app/pages/admin/workspace/file-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { hasNewValidProps, reactStyles } from 'app/utils';
import {
  getSelectedPopulations,
  getSelectedPrimaryPurposeItems,
} from 'app/utils/research-purpose';
import { getCreator } from 'app/utils/runtime-utils';
import { MatchParams } from 'app/utils/stores';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';
import moment from 'moment';

const styles = reactStyles({
  infoRow: {
    width: '80%',
    maxWidth: '1000px',
  },
  infoLabel: {
    width: '300px',
    minWidth: '180px',
    textAlign: 'right',
    marginRight: '1.5rem',
  },
  infoValue: {
    flex: 1,
    wordWrap: 'break-word',
  },
  wideWithMargin: {
    width: '30rem',
    marginRight: '1.5rem',
  },
  narrowWithMargin: {
    width: '15rem',
    marginRight: '1.5rem',
  },
  fileDetailsTable: {
    maxWidth: '1000px',
    marginTop: '1.5rem',
  },
  accessReasonText: {
    maxWidth: '1000px',
    height: '4.5rem',
  },
  previewButton: {
    marginLeft: '20px',
    height: '1.5rem',
  },
});

export const PurpleLabel = ({ style = {}, children }) => {
  return <label style={{ color: colors.primary, ...style }}>{children}</label>;
};

const WorkspaceInfoField = ({ labelText, children }) => {
  return (
    <FlexRow style={styles.infoRow}>
      <PurpleLabel style={styles.infoLabel}>{labelText}</PurpleLabel>
      <div style={styles.infoValue}>{children}</div>
    </FlexRow>
  );
};

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

interface State {
  workspaceDetails?: WorkspaceAdminView;
  cloudStorageTraffic?: CloudStorageTraffic;
  loadingData?: boolean;
  loadingWorkspaceAdminLockedStatus: boolean;
  runtimeToDelete?: ListRuntimeResponse;
  confirmDeleteRuntime?: boolean;
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
    this.getFederatedWorkspaceInformation();
  }

  componentDidUpdate(prevProps) {
    if (hasNewValidProps(this.props, prevProps, [(p) => p.match.params])) {
      this.getFederatedWorkspaceInformation();
    }
  }

  async getFederatedWorkspaceInformation() {
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

  maybeGetFederatedWorkspaceInformation(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      return this.getFederatedWorkspaceInformation();
    }
  }

  renderHighChart(cloudStorageTraffic: CloudStorageTraffic): ReactFragment {
    HighCharts.setOptions({
      time: {
        useUTC: false,
      },
      lang: {
        decimalPoint: '.',
        thousandsSep: ',',
      },
    });
    const options = {
      animation: false,
      chart: {
        animation: false,
        height: '150px',
      },
      credits: {
        enabled: false,
      },
      legend: {
        enabled: false,
      },
      title: {
        text: undefined,
      },
      tooltip: {
        xDateFormat: '%A, %b %e, %H:%M',
        valueDecimals: 0,
      },
      xAxis: {
        min: moment().subtract(6, 'hours').valueOf(),
        max: moment().valueOf(),
        title: {
          enabled: false,
        },
        type: 'datetime',
        zoomEnabled: false,
      },
      yAxis: {
        title: {
          enabled: false,
        },
        zoomEnabled: false,
      },
      series: [
        {
          data: cloudStorageTraffic.receivedBytes.map((x) => [
            x.timestamp,
            x.value,
          ]),
          lineWidth: 0.5,
          name: 'GCS received bytes',
        },
      ],
    };
    return (
      <div style={{ width: '500px', zIndex: 1001 }}>
        <HighchartsReact highcharts={HighCharts} options={options} />
      </div>
    );
  }

  private async deleteRuntime() {
    await workspaceAdminApi().deleteRuntimesInWorkspace(
      this.props.match.params.ns,
      { runtimesToDelete: [this.state.runtimeToDelete.runtimeName] }
    );
    this.setState({ runtimeToDelete: null });
    await this.getFederatedWorkspaceInformation();
  }

  private cancelDeleteRuntime() {
    this.setState({
      confirmDeleteRuntime: false,
      runtimeToDelete: null,
    });
  }

  async unLockWorkspace() {
    const {
      workspaceDetails: { workspace },
    } = this.state;
    try {
      this.setState({ loadingWorkspaceAdminLockedStatus: true });
      await workspaceAdminApi().setAdminUnlockedState(workspace.namespace);
      await this.getFederatedWorkspaceInformation();
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
    await this.getFederatedWorkspaceInformation();
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
      runtimeToDelete,
      confirmDeleteRuntime,
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
            <h3>Basic Information</h3>
            <div className='basic-info' style={{ marginTop: '1.5rem' }}>
              <WorkspaceInfoField labelText='Workspace Name'>
                {workspace.name}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Workspace Namespace'>
                {workspace.namespace}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Access Tier'>
                {workspace.accessTierShortName?.toUpperCase()}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Google Project Id'>
                {workspace.googleProject}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Billing Status'>
                {workspace.billingStatus}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Billing Account Type'>
                {isUsingFreeTierBillingAccount(workspace)
                  ? 'Free tier'
                  : 'User provided'}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Creation Time'>
                {new Date(workspace.creationTime).toDateString()}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Last Modified Time'>
                {new Date(workspace.lastModifiedTime).toDateString()}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Workspace Published'>
                {workspace.published ? 'Yes' : 'No'}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Audit'>
                {
                  <Link to={`/admin/workspace-audit/${workspace.namespace}`}>
                    Audit History
                  </Link>
                }
              </WorkspaceInfoField>
            </div>
            <h3>Collaborators</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              {collaborators.map((workspaceUserAdminView, i) => (
                <div key={i}>
                  {workspaceUserAdminView.userModel.userName +
                    ': ' +
                    workspaceUserAdminView.role}
                </div>
              ))}
            </div>
            <h3>Cohort Builder</h3>
            <div className='cohort-builder' style={{ marginTop: '1.5rem' }}>
              <WorkspaceInfoField labelText='# of Cohorts'>
                {resources.workspaceObjects.cohortCount}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='# of Concept Sets'>
                {resources.workspaceObjects.conceptSetCount}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='# of Data Sets'>
                {resources.workspaceObjects.datasetCount}
              </WorkspaceInfoField>
            </div>
            <h3>Cloud Storage Objects</h3>
            <div
              className='cloud-storage-objects'
              style={{ marginTop: '1.5rem' }}
            >
              <div
                style={{
                  color: colors.warning,
                  fontWeight: 'bold',
                  maxWidth: '1000px',
                }}
              >
                NOTE: if there are more than ~1000 files in the bucket, these
                counts and the table below may be incomplete because we process
                only a single page of storage list results.
              </div>
              <WorkspaceInfoField labelText='GCS bucket path'>
                {resources.cloudStorage.storageBucketPath}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='# of Workbench-managed notebook files'>
                {resources.cloudStorage.notebookFileCount}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='# of other files'>
                {resources.cloudStorage.nonNotebookFileCount}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Storage used (MB)'>
                {formatMB(resources.cloudStorage.storageBytesUsed)}
              </WorkspaceInfoField>
            </div>
            <FileDetailsTable
              workspaceNamespace={workspace.namespace}
              bucket={resources.cloudStorage.storageBucketPath}
            />

            <h3>Research Purpose</h3>
            <div className='research-purpose' style={{ marginTop: '1.5rem' }}>
              <WorkspaceInfoField labelText='Primary purpose of project'>
                {getSelectedPrimaryPurposeItems(workspace.researchPurpose).map(
                  (researchPurposeItem, i) => (
                    <div key={i}>{researchPurposeItem}</div>
                  )
                )}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Reason for choosing All of Us'>
                {workspace.researchPurpose.reasonForAllOfUs}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Area of intended study'>
                {workspace.researchPurpose.intendedStudy}
              </WorkspaceInfoField>
              <WorkspaceInfoField labelText='Anticipated findings'>
                {workspace.researchPurpose.anticipatedFindings}
              </WorkspaceInfoField>
              {workspace.researchPurpose.populationDetails.length > 0 && (
                <WorkspaceInfoField labelText='Population area(s) of focus'>
                  {getSelectedPopulations(workspace.researchPurpose)}
                </WorkspaceInfoField>
              )}
            </div>
          </div>
        )}

        {cloudStorageTraffic?.receivedBytes && (
          <div>
            <h2>Cloud Storage Traffic</h2>
            <div>
              Cloud Storage <i>received_bytes_count</i> over the past 6 hours.
            </div>
            {this.renderHighChart(cloudStorageTraffic)}
          </div>
        )}

        {resources && resources.runtimes.length === 0 && (
          <div>
            <h2>Runtimes</h2>
            <p>No active runtimes exist for this workspace.</p>
          </div>
        )}
        {resources && resources.runtimes.length > 0 && (
          <div>
            <h2>Runtimes</h2>
            <FlexColumn>
              <FlexRow>
                <PurpleLabel style={styles.narrowWithMargin}>
                  Runtime Name
                </PurpleLabel>
                <PurpleLabel style={styles.narrowWithMargin}>
                  Creator
                </PurpleLabel>
                <PurpleLabel style={styles.narrowWithMargin}>
                  Created Time
                </PurpleLabel>
                <PurpleLabel style={styles.narrowWithMargin}>
                  Last Accessed Time
                </PurpleLabel>
                <PurpleLabel style={styles.narrowWithMargin}>
                  Status
                </PurpleLabel>
              </FlexRow>
              {resources.runtimes.map((runtime, i) => (
                <FlexRow key={i}>
                  <div style={styles.narrowWithMargin}>
                    {runtime.runtimeName}
                  </div>
                  <div style={styles.narrowWithMargin}>
                    {getCreator(runtime)}
                  </div>
                  <div style={styles.narrowWithMargin}>
                    {new Date(runtime.createdDate).toDateString()}
                  </div>
                  <div style={styles.narrowWithMargin}>
                    {new Date(runtime.dateAccessed).toDateString()}
                  </div>
                  <div style={styles.narrowWithMargin}>{runtime.status}</div>
                  <Button
                    onClick={() =>
                      this.setState({
                        runtimeToDelete: runtime,
                        confirmDeleteRuntime: true,
                      })
                    }
                    disabled={
                      runtimeToDelete &&
                      runtimeToDelete.runtimeName === runtime.runtimeName
                    }
                  >
                    Delete
                  </Button>
                </FlexRow>
              ))}
            </FlexColumn>
          </div>
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
        {confirmDeleteRuntime && (
          <Modal onRequestClose={() => this.cancelDeleteRuntime()}>
            <ModalTitle>Delete Runtime</ModalTitle>
            <ModalBody>
              This will immediately delete the given runtime. This will disrupt
              the user's work and may cause data loss.
              <br />
              <br />
              <b>Are you sure?</b>
            </ModalBody>
            <ModalFooter>
              <Button
                type='secondary'
                onClick={() => this.cancelDeleteRuntime()}
              >
                Cancel
              </Button>
              <Button
                style={{ marginLeft: '0.75rem' }}
                onClick={() => {
                  this.setState({ confirmDeleteRuntime: false });
                  this.deleteRuntime();
                }}
              >
                Delete
              </Button>
            </ModalFooter>
          </Modal>
        )}
      </div>
    );
  }
}

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
