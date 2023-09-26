import * as React from 'react';

import { AdminWorkspaceObjectsCounts } from 'generated/fetch';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspaceObjects: AdminWorkspaceObjectsCounts;
}
export const CohortBuilder = ({ workspaceObjects }: Props) => {
  const { cohortCount, conceptSetCount, datasetCount } = workspaceObjects;
  return (
    <>
      <h3>Cohort Builder</h3>
      <div className='cohort-builder' style={{ marginTop: '1.5rem' }}>
        <WorkspaceInfoField labelText='# of Cohorts'>
          {cohortCount}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='# of Concept Sets'>
          {conceptSetCount}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='# of Data Sets'>
          {datasetCount}
        </WorkspaceInfoField>
      </div>
    </>
  );
};
