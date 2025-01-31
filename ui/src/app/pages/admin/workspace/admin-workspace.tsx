import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { Accordion, AccordionTab } from 'primereact/accordion';

import {
  AdminRuntimeFields,
  CloudStorageTraffic,
  UserAppEnvironment,
  WorkspaceActiveStatus,
  WorkspaceAdminView,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { ResearchPurposeSection } from 'app/components/research-purpose-section';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { DisksTable } from 'app/pages/admin/workspace/disks-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import {
  AuthorityGuardedAction,
  renderIfAuthorized,
} from 'app/utils/authorities';
import { MatchParams, profileStore } from 'app/utils/stores';

import { AdminLockWorkspace } from './admin-lock-workspace';
import { BasicInformation } from './basic-information';
import { CloudEnvironmentsTable } from './cloud-environments-table';
import { CloudStorageObjects } from './cloud-storage-objects';
import { CloudStorageTrafficChart } from './cloud-storage-traffic-chart';
import { CohortBuilder } from './cohort-builder';
import { Collaborators } from './collaborators';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

const AdminWorkspaceImpl = (props: Props) => {
  const [workspaceDetails, setWorkspaceDetails] =
    useState<WorkspaceAdminView>();
  const [cloudStorageTraffic, setCloudStorageTraffic] =
    useState<CloudStorageTraffic>();
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [dataLoadError, setDataLoadError] = useState<Response>();
  const [runtimes, setRuntimes] = useState<AdminRuntimeFields[]>();
  const [userApps, setUserApps] = useState<UserAppEnvironment[]>();

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
      setDataLoadError(error);
    }
  };

  const populateFederatedWorkspaceInformation = async () => {
    const { ns } = props.match.params;
    setLoadingWorkspace(true);

    try {
      const newCloudStorageTraffic =
        await workspaceAdminApi().getCloudStorageTraffic(ns);
      setCloudStorageTraffic(newCloudStorageTraffic);
    } catch (error) {
      console.log('Error loading cloud storage traffic: ', error);
    }

    try {
      const newRuntimes = await workspaceAdminApi().adminListRuntimes(ns);
      setRuntimes(newRuntimes);
    } catch (error) {
      handleDataLoadError(error);
    }

    try {
      const newUserApps =
        await workspaceAdminApi().adminListUserAppsInWorkspace(ns);
      setUserApps(newUserApps);
    } catch (error) {
      handleDataLoadError(error);
    }

    try {
      const newWorkspaceDetails =
        await workspaceAdminApi().getWorkspaceAdminView(ns);
      setWorkspaceDetails(newWorkspaceDetails);
    } catch (error) {
      handleDataLoadError(error);
    } finally {
      setLoadingWorkspace(false);
    }
  };

  useEffect(() => {
    props.hideSpinner();
    populateFederatedWorkspaceInformation();
  }, []);

  useEffect(() => {
    populateFederatedWorkspaceInformation();
  }, [props.match.params.ns]);

  const populateWorkspaceDetails = async () => {
    const { ns } = props.match.params;
    setLoadingWorkspace(true);

    try {
      const newWorkspaceDetails =
        await workspaceAdminApi().getWorkspaceAdminView(ns);
      setWorkspaceDetails(newWorkspaceDetails);
    } catch (error) {
      handleDataLoadError(error);
    } finally {
      setLoadingWorkspace(false);
    }
  };

  const { profile } = profileStore.get();
  const { collaborators, resources, workspace, activeStatus } =
    workspaceDetails || {};
  const { workspaceObjects, cloudStorage } = resources || {};
  const { researchPurpose } = workspace || {};

  return (
    <div style={{ margin: '1.5rem' }}>
      {dataLoadError && (
        <ErrorDiv>
          Error loading data. Please refresh the page or contact the development
          team.
        </ErrorDiv>
      )}
      {loadingWorkspace && <SpinnerOverlay />}
      {workspace && (
        <div>
          {activeStatus === WorkspaceActiveStatus.ACTIVE && (
            <AdminLockWorkspace
              {...{ workspace }}
              reload={async () => await populateFederatedWorkspaceInformation()}
            />
          )}
          <BasicInformation
            {...{ workspace, activeStatus }}
            reload={async () => await populateWorkspaceDetails()}
          />
          <Accordion>
            <AccordionTab header='Research Purpose'>
              <ResearchPurposeSection {...{ researchPurpose }} />
            </AccordionTab>
          </Accordion>
          {activeStatus === WorkspaceActiveStatus.ACTIVE && (
            <>
              <Collaborators
                {...{ collaborators }}
                creator={workspace.creatorUser.userName}
              />
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
                onDelete={() => populateFederatedWorkspaceInformation()}
              />
              <h2>Egress event history</h2>
              {renderIfAuthorized(
                profile,
                AuthorityGuardedAction.EGRESS_EVENTS,
                () => (
                  <EgressEventsTable
                    displayPageSize={10}
                    sourceWorkspaceNamespace={workspace.namespace}
                  />
                )
              )}
              <h2>Disks</h2>
              <DisksTable sourceWorkspaceNamespace={workspace.namespace} />
            </>
          )}
        </div>
      )}
    </div>
  );
};

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
