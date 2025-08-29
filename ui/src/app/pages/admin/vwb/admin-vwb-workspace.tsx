import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { VwbWorkspaceAdminView } from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { WorkspaceInfoField } from 'app/pages/admin/workspace/workspace-info-field';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { MatchParams, serverConfigStore } from 'app/utils/stores';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

export const AdminVwbWorkspace = fp.flow(withRouter)((props: Props) => {
  const [workspaceDetails, setWorkspaceDetails] =
    useState<VwbWorkspaceAdminView>();
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [dataLoadError, setDataLoadError] = useState<Response>();

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
      setDataLoadError(error);
    }
  };

  const populateWorkspaceDetails = async () => {
    const { ufid } = props.match.params;
    setLoadingWorkspace(true);

    vwbWorkspaceAdminApi()
      .getVwbWorkspaceAdminView(ufid)
      .then(setWorkspaceDetails)
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspace(false));
  };

  useEffect(() => {
    props.hideSpinner();
  }, []);

  useEffect(() => {
    populateWorkspaceDetails();
  }, [props.match.params.ufid]);

  const { collaborators, workspace } = workspaceDetails || {};

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
          <h3>Basic Information</h3>
          <div className='basic-info' style={{ marginTop: '1.5rem' }}>
            <WorkspaceInfoField labelText='Billing Account Type'>
              {workspace.podUserFacingId ===
              serverConfigStore.get().config.initialCreditsPodUserFacingId
                ? `Initial credits (${workspace.createdBy})`
                : 'User provided'}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Google Project Id'>
              {workspace.googleProjectId}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace ID'>
              {workspace.id}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='User Facing ID'>
              {workspace.userFacingId}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace Name'>
              {workspace.displayName}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Creator'>
              {workspace.createdBy}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Creation Time'>
              {new Date(workspace.creationTime).toDateString()}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Description'>
              {workspace.description}
            </WorkspaceInfoField>
            <h3>Collaborators</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              {collaborators.map((collaborator, c) => (
                <div key={c} style={{ marginBottom: '1rem' }}>
                  {collaborator}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
});
