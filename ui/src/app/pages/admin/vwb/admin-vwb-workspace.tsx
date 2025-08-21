import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { VwbWorkspace, VwbWorkspaceSearchParamType } from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { WorkspaceInfoField } from 'app/pages/admin/workspace/workspace-info-field';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { MatchParams, profileStore } from 'app/utils/stores';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

export const AdminVwbWorkspace = fp.flow(withRouter)((props: Props) => {
  const [workspace, setWorkspace] = useState<VwbWorkspace>();
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
      .getVwbWorkspaceBySearchParam(
        VwbWorkspaceSearchParamType.USER_FACING_ID,
        ufid
      )
      .then((resp) => {
        setWorkspace(resp.items[0]);
      })
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspace(false));
  };

  useEffect(() => {
    console.log(props.match);
    props.hideSpinner();
  }, []);

  useEffect(() => {
    if (props.match.params.ufid) {
      populateWorkspaceDetails();
    }
  }, [props.match.params.ufid]);

  const { profile } = profileStore.get();

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
            {/*<WorkspaceInfoField labelText='Active Status'>*/}
            {/*  {activeStatus}*/}
            {/*</WorkspaceInfoField>*/}
            {/*<WorkspaceInfoField labelText='Billing Account Type'>*/}
            {/*  {isUsingInitialCredits(workspace)*/}
            {/*    ? `Initial credits (${workspace.creatorUser.userName})`*/}
            {/*    : 'User provided'}*/}
            {/*</WorkspaceInfoField>*/}
            {/*{isUsingInitialCredits(workspace) && (*/}
            {/*  <WorkspaceInfoField labelText='Initial Credits Billing Status'>*/}
            {/*    {workspace.initialCredits?.exhausted*/}
            {/*      ? 'Exhausted'*/}
            {/*      : 'Not Exhausted'}*/}
            {/*    ,{' '}*/}
            {/*    {workspace.initialCredits?.expirationEpochMillis <= Date.now()*/}
            {/*      ? 'Expired'*/}
            {/*      : 'Not Expired'}*/}
            {/*  </WorkspaceInfoField>*/}
            {/*)}*/}
            <WorkspaceInfoField labelText='Workspace Name'>
              {workspace.displayName}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='User Facing ID'>
              {workspace.userFacingId}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Creator'>
              {workspace.createdBy}
            </WorkspaceInfoField>
            {/*<WorkspaceInfoField labelText='Google Project Id'>*/}
            {/*  {workspace.googleProject}*/}
            {/*</WorkspaceInfoField>*/}
            <WorkspaceInfoField labelText='Creation Time'>
              {new Date(workspace.creationTime).toDateString()}
            </WorkspaceInfoField>
          </div>
          {/*<Accordion>*/}
          {/*  <AccordionTab header='Research Purpose'>*/}
          {/*    <ResearchPurposeSection*/}
          {/*      {...{researchPurpose}}*/}
          {/*      showAIAN={*/}
          {/*        cdrVersion &&*/}
          {/*        showAIANResearchPurpose(cdrVersion.publicReleaseNumber)*/}
          {/*      }*/}
          {/*    />*/}
          {/*  </AccordionTab>*/}
          {/*</Accordion>*/}
          {/*{activeStatus === WorkspaceActiveStatus.ACTIVE && (*/}
          {/*  <>*/}
          {/*    <Collaborators*/}
          {/*      {...{collaborators}}*/}
          {/*      creator={workspace.creatorUser.userName}*/}
          {/*    />*/}
          {/*    <CohortBuilder {...{workspaceObjects}} />*/}
          {/*    <CloudStorageObjects*/}
          {/*      {...{cloudStorage}}*/}
          {/*      workspaceNamespace={workspace.namespace}*/}
          {/*    />*/}
          {/*    {cloudStorageTraffic?.receivedBytes && (*/}
          {/*      <CloudStorageTrafficChart {...{cloudStorageTraffic}} />*/}
          {/*    )}*/}
          {/*    <h2>Cloud Environments</h2>*/}
          {/*    <CloudEnvironmentsTable*/}
          {/*      {...{runtimes, userApps}}*/}
          {/*      workspaceNamespace={workspace.namespace}*/}
          {/*      onDelete={populateFederatedWorkspaceInformation}*/}
          {/*    />*/}
          {/*    <h2>Egress event history</h2>*/}
          {/*    {renderIfAuthorized(*/}
          {/*      profile,*/}
          {/*      AuthorityGuardedAction.EGRESS_EVENTS,*/}
          {/*      () => (*/}
          {/*        <EgressEventsTable*/}
          {/*          displayPageSize={10}*/}
          {/*          sourceWorkspaceNamespace={workspace.namespace}*/}
          {/*        />*/}
          {/*      )*/}
          {/*    )}*/}
          {/*    <h2>Disks</h2>*/}
          {/*    <DisksTable sourceWorkspaceNamespace={workspace.namespace}/>*/}
          {/*  </>*/}
          {/*)}*/}
        </div>
      )}
    </div>
  );
});
