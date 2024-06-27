import * as React from 'react';
import { Link } from 'react-router-dom';

import {
  AnnotationType,
  Workspace,
  WorkspaceActiveStatus,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { Select } from 'app/components/inputs';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
  activeStatus: WorkspaceActiveStatus;
}
export const BasicInformation = ({ workspace, activeStatus }: Props) => (
  <>
    <h3>Basic Information</h3>
    <div className='basic-info' style={{ marginTop: '1.5rem' }}>
      <WorkspaceInfoField labelText='Active Status'>
        {activeStatus}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Billing Status'>
        {workspace.billingStatus}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Workspace Name'>
        {workspace.name}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Terra Name (often incorrectly called "id")'>
        {workspace.id}
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
      <WorkspaceInfoField labelText='Billing Account Type'>
        {isUsingFreeTierBillingAccount(workspace)
          ? 'Initial credits'
          : 'User provided'}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Creation Time'>
        {new Date(workspace.creationTime).toDateString()}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Last Modified Time'>
        {new Date(workspace.lastModifiedTime).toDateString()}
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Workspace Published'>
        <Select
          value={null}
          options={[
            { value: 'COMMUNITY', label: 'Community' },
            { value: 'DEMO_PROJECTS', label: 'Demo Projects' },
            { value: 'PHENOTYPE_LIBRAY', label: 'Phenotype Library' },
            { value: 'TUTORIAL_WORKSPACES', label: 'Tutorial Workspaces' },
          ]}
          onChange={(v) => console.log(v)}
        />
        <Button type='primary' disabled>
          Publish
        </Button>
        <Button type='secondaryOutline'>Unpublish</Button>
      </WorkspaceInfoField>
      <WorkspaceInfoField labelText='Audit'>
        {
          <Link to={`/admin/workspace-audit/${workspace.namespace}`}>
            Audit History
          </Link>
        }
      </WorkspaceInfoField>
    </div>
  </>
);
