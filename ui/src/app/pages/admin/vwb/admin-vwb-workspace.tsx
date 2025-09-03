import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  UserRole,
  VwbWorkspaceAdminView,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { WorkspaceInfoField } from 'app/pages/admin/workspace/workspace-info-field';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { MatchParams, serverConfigStore } from 'app/utils/stores';

const collabList = (users: UserRole[]) => {
  return users?.length > 0
    ? users.map((c) => (
        <div style={{ marginLeft: '1rem' }} key={c.email}>
          {c.email}
        </div>
      ))
    : 'None';
};

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
              {workspace.billingAccountId ===
              serverConfigStore.get().config.initialCreditsBillingAccountId
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
            <WorkspaceInfoField labelText='Creation Time'>
              {new Date(workspace.creationTime).toDateString()}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Description'>
              {workspace.description}
            </WorkspaceInfoField>
            <h3>Collaborators</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              <div style={{ marginBottom: '1rem' }}>
                Creator and Owner: {workspace.createdBy}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Other Owners:{' '}
                {collabList(
                  collaborators.filter(
                    (c) =>
                      c.role === WorkspaceAccessLevel.OWNER &&
                      c.email !== workspace.createdBy
                  )
                )}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Writers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.WRITER
                  )
                )}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Readers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.READER
                  )
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
});
