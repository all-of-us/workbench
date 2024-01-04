import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import {
  CloudStorageTraffic,
  WorkspaceActiveStatus,
  WorkspaceAdminView,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { DisksTable } from 'app/pages/admin/workspace/disks-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { hasNewValidProps } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

import { AdminLockWorkspace } from './admin-lock-workspace';
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
  dataLoadError?: Response;
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

  async populateFederatedWorkspaceInformation() {
    const { ns } = this.props.match.params;
    this.setState({
      loadingData: true,
    });

    // cloud storage traffic isn't always available (e.g. for a deleted workspace) so we need to allow for that
    workspaceAdminApi()
      .getCloudStorageTraffic(ns)
      .then((cloudStorageTraffic) => this.setState({ cloudStorageTraffic }))
      .catch(() => {});

    try {
      const workspaceDetails = await workspaceAdminApi().getWorkspaceAdminView(
        ns
      );
      this.setState({ workspaceDetails });
    } catch (error) {
      if (error instanceof Response) {
        console.log('error', error, await error.json());
        this.setState({ dataLoadError: error });
      }
    } finally {
      this.setState({ loadingData: false });
    }
  }

  render() {
    const {
      cloudStorageTraffic,
      loadingData,
      dataLoadError,
      workspaceDetails: { collaborators, resources, workspace, activeStatus },
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
        {loadingData && <SpinnerOverlay />}
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
                {resources && (
                  <Runtimes
                    {...{ resources }}
                    workspaceNamespace={workspace.namespace}
                    onDelete={() =>
                      this.populateFederatedWorkspaceInformation()
                    }
                  />
                )}
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
        )}
      </div>
    );
  }
}

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
