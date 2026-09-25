import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { Accordion, AccordionTab } from 'primereact/accordion';

import {
  WorkspaceActiveStatus,
  WorkspaceAdminView,
  WorkspaceUserAdminView,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { ResearchPurposeSection } from 'app/components/research-purpose-section';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import {
  AuthorityGuardedAction,
  renderIfAuthorized,
} from 'app/utils/authorities';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { cdrVersionStore, MatchParams, profileStore } from 'app/utils/stores';
import { showAIANResearchPurpose } from 'app/utils/workspace-utils';

import { AdminLockWorkspace } from './admin-lock-workspace';
import { AdminWorkspaceRecoveryModal } from './admin-workspace-recovery-modal';
import { BasicInformation } from './basic-information';
import { Collaborators } from './collaborators';
import { WorkspaceArchiveInfo } from './workspace-archival-info';
import { WorkspaceMigrationInfo } from './workspace-migration-info';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

const AdminWorkspaceImpl = (props: Props) => {
  const [workspaceDetails, setWorkspaceDetails] =
    useState<WorkspaceAdminView>();
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [dataLoadError, setDataLoadError] = useState<Response>();
  const [showRecoveryModal, setShowRecoveryModal] = useState(false);
  const [workspaceCollaborators, setWorkspaceCollaborators] =
    useState<WorkspaceUserAdminView[]>();

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
      setDataLoadError(error);
    }
  };

  const populateFederatedWorkspaceInformation = async () => {
    const { ns } = props.match.params;
    setLoadingWorkspace(true);

    workspaceAdminApi()
      .getWorkspaceCollaborators(ns)
      .then(setWorkspaceCollaborators)
      .catch((e) => console.error(e));

    workspaceAdminApi()
      .getWorkspaceAdminView(ns)
      .then(setWorkspaceDetails)
      .catch(handleDataLoadError)
      .finally(() => setLoadingWorkspace(false));
  };

  useEffect(() => {
    props.hideSpinner();
  }, []);

  useEffect(() => {
    populateFederatedWorkspaceInformation();
  }, [props.match.params.ns]);

  const populateWorkspaceDetails = async () => {
    const { ns } = props.match.params;
    setLoadingWorkspace(true);

    workspaceAdminApi()
      .getWorkspaceAdminView(ns)
      .then(setWorkspaceDetails)
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspace(false));
  };

  const { profile } = profileStore.get();
  const { collaborators, workspace, activeStatus } = workspaceDetails || {};
  const { researchPurpose } = workspace || {};
  const cdrVersion = findCdrVersion(
    workspace?.cdrVersionId,
    cdrVersionStore.get()
  );

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
              reload={populateFederatedWorkspaceInformation}
            />
          )}
          <BasicInformation
            {...{ workspace, activeStatus }}
            reload={populateWorkspaceDetails}
          />
          <WorkspaceMigrationInfo workspace={workspace} />
          <WorkspaceArchiveInfo
            workspace={workspace}
            onRecover={() => setShowRecoveryModal(true)}
          />
          <Accordion>
            <AccordionTab header='Research Purpose'>
              <ResearchPurposeSection
                {...{ researchPurpose }}
                showAIAN={
                  cdrVersion &&
                  showAIANResearchPurpose(cdrVersion.publicReleaseNumber)
                }
              />
            </AccordionTab>
          </Accordion>
          {activeStatus === WorkspaceActiveStatus.ACTIVE && (
            <>
              <Collaborators
                {...{ collaborators }}
                creator={workspace.creatorUser.userName}
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
            </>
          )}

          {showRecoveryModal && (
            <AdminWorkspaceRecoveryModal
              workspace={workspace}
              collaborators={collaborators || workspaceCollaborators}
              onClose={() => setShowRecoveryModal(false)}
              reload={populateWorkspaceDetails}
            />
          )}
        </div>
      )}
    </div>
  );
};

export const AdminWorkspace = withRouter(AdminWorkspaceImpl);
