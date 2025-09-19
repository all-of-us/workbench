import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  UserRole,
  VwbWorkspaceAdminView,
  VwbWorkspaceAuditLog,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
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
  const [workspaceActivity, setWorkspaceActivity] =
    useState<VwbWorkspaceAuditLog[]>();
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [loadingWorkspaceActivity, setLoadingWorkspaceActivity] =
    useState<boolean>(false);
  const [dataLoadError, setDataLoadError] = useState<Response>();

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
      setDataLoadError(error);
    }
  };

  const getWorkspaceActivity = async (workspaceId: string) => {
    setLoadingWorkspaceActivity(true);

    vwbWorkspaceAdminApi()
      .getVwbWorkspaceAuditLogs(workspaceId)
      .then(setWorkspaceActivity)
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspaceActivity(false));
  };

  const populateWorkspaceDetails = async () => {
    const { ufid } = props.match.params;
    setLoadingWorkspace(true);

    vwbWorkspaceAdminApi()
      .getVwbWorkspaceAdminView(ufid)
      .then((resp) => {
        setWorkspaceDetails(resp);
        getWorkspaceActivity(resp.workspace.id);
      })
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
            <h3>Workspace Activity</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              {loadingWorkspaceActivity ? (
                <Spinner style={{ marginLeft: '40%' }} />
              ) : (
                <DataTable
                  paginator
                  rows={10}
                  emptyMessage='No workspace activity found'
                  loading={loadingWorkspaceActivity}
                  value={workspaceActivity}
                >
                  <Column
                    field='changeType'
                    header='Change Type'
                    headerStyle={{ width: '250px' }}
                  />
                  <Column
                    field='actorEmail'
                    header='Change By'
                    headerStyle={{ width: '150px' }}
                  />
                  <Column
                    field='changeTime'
                    header='Change Time'
                    headerStyle={{ width: '150px' }}
                    body={({ changeTime }) =>
                      new Date(changeTime).toLocaleString()
                    }
                  />
                </DataTable>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
});
