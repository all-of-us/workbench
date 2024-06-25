import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import {
  CloudStorageTraffic,
  FeaturedWorkspaceCategory,
  ListRuntimeResponse,
  UserAppEnvironment,
  WorkspaceActiveStatus,
  WorkspaceAdminView,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Error as ErrorDiv } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { DisksTable } from 'app/pages/admin/workspace/disks-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { hasNewValidProps } from 'app/utils';
import { MatchParams, serverConfigStore } from 'app/utils/stores';

import { AdminLockWorkspace } from './admin-lock-workspace';
import { BasicInformation } from './basic-information';
import { CloudEnvironmentsTable } from './cloud-environments-table';
import { CloudStorageObjects } from './cloud-storage-objects';
import { CloudStorageTrafficChart } from './cloud-storage-traffic-chart';
import { CohortBuilder } from './cohort-builder';
import { Collaborators } from './collaborators';
import { ResearchPurposeSection } from './research-purpose-section';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

interface State {
  workspaceDetails?: WorkspaceAdminView;
  cloudStorageTraffic?: CloudStorageTraffic;
  loadingWorkspace?: boolean;
  dataLoadError?: Response;
  runtimes?: ListRuntimeResponse[];
  userApps?: UserAppEnvironment[];
}

export class AdminWorkspaceImpl extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      workspaceDetails: {},
      cloudStorageTraffic: null,
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    this.populateFederatedWorkspaceInformation();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      hasNewValidProps(this.props, prevProps, [(p: Props) => p.match.params.ns])
    ) {
      this.populateFederatedWorkspaceInformation();
    }
  }

  handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
      this.setState({ dataLoadError: error });
    }
  };

  async populateFederatedWorkspaceInformation() {
    const { ns } = this.props.match.params;
    this.setState({ loadingWorkspace: true });

    // cloud storage traffic isn't always available (e.g. for a deleted workspace) so we need to allow for that
    workspaceAdminApi()
      .getCloudStorageTraffic(ns)
      .then((cloudStorageTraffic) => this.setState({ cloudStorageTraffic }))
      .catch(() => {});

    // runtimes and user apps calls have error modes which cause them to be slow, so execute these in parallel
    // to the main admin view call

    workspaceAdminApi()
      .adminListRuntimesInWorkspace(ns)
      .then((runtimes) => this.setState({ runtimes }))
      .catch(this.handleDataLoadError);

    workspaceAdminApi()
      .adminListUserAppsInWorkspace(ns)
      .then((userApps) => this.setState({ userApps }))
      .catch(this.handleDataLoadError);

    workspaceAdminApi()
      .getWorkspaceAdminView(ns)
      .then((workspaceDetails) => this.setState({ workspaceDetails }))
      .catch(this.handleDataLoadError)
      .finally(() => this.setState({ loadingWorkspace: false }));
  }

  async callToPublish() {
    await workspaceAdminApi().publishWorkspaceviaDB(
      this.props.match.params.ns,
      {
        category: FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY,
        description: 'This is just a test',
      }
    );
  }

  async callToUnPublish() {
    await workspaceAdminApi().unPublishWorkspaceviaDB(
      this.props.match.params.ns
    );
  }

  render() {
    const {
      cloudStorageTraffic,
      loadingWorkspace,
      dataLoadError,
      workspaceDetails: { collaborators, resources, workspace, activeStatus },
      runtimes,
      userApps,
    } = this.state;
    const { workspaceObjects, cloudStorage } = resources || {};
    const { researchPurpose } = workspace || {};
    return (
      <div style={{ margin: '1.5rem' }}>
        {dataLoadError && (
          <ErrorDiv>
            Error loading data. Please refresh the page or contact the
            development team.
          </ErrorDiv>
        )}
        {loadingWorkspace && <SpinnerOverlay />}
        {workspace && (
          <div>
            {activeStatus === WorkspaceActiveStatus.ACTIVE && (
              <AdminLockWorkspace
                {...{ workspace }}
                reload={async () =>
                  await this.populateFederatedWorkspaceInformation()
                }
              />
            )}
            <BasicInformation {...{ workspace, activeStatus }} />
            <ResearchPurposeSection {...{ researchPurpose }} />
            {activeStatus === WorkspaceActiveStatus.ACTIVE && (
              <>
                <Collaborators {...{ collaborators }} />
                <CohortBuilder {...{ workspaceObjects }} />
                <CloudStorageObjects
                  {...{ cloudStorage }}
                  workspaceNamespace={workspace.namespace}
                />
                {cloudStorageTraffic?.receivedBytes && (
                  <CloudStorageTrafficChart {...{ cloudStorageTraffic }} />
                )}
                <h2>Cloud Environments</h2>
                <CloudEnvironmentsTable
                  {...{ runtimes, userApps }}
                  workspaceNamespace={workspace.namespace}
                  onDelete={() => this.populateFederatedWorkspaceInformation()}
                />
                <h2>Egress event history</h2>
                <EgressEventsTable
                  displayPageSize={10}
                  sourceWorkspaceNamespace={workspace.namespace}
                />
                <h2>Disks</h2>
                <DisksTable sourceWorkspaceNamespace={workspace.namespace} />

                {serverConfigStore.get().config
                  .enableAdminPublishedWorkspaces && (
                  <div>
                    <h2>
                      This is temp just to verify api This is not a final UI
                    </h2>
                    <Button
                      data-test-id='publishButton'
                      type='secondary'
                      style={{ border: '2px solid' }}
                      onClick={() => this.callToPublish()}
                    >
                      Publish Workspace
                    </Button>

                    <Button
                      data-test-id='unPublishButton'
                      type='secondary'
                      style={{ border: '2px solid' }}
                      onClick={() => this.callToUnPublish()}
                    >
                      UnPublish Workspace
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    );
  }
}

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
