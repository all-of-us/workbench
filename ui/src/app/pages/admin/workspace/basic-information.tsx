import * as React from 'react';
import { Link } from 'react-router-dom';

import { Workspace } from 'generated/fetch';

import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
}
export const BasicInformation = ({ workspace }: Props) => (
  <>
    <h3>Basic Information</h3>
    <div className='basic-info' style={{ marginTop: '1.5rem' }}>
      <WorkspaceInfoField labelText='Workspace Name'>
        {workspace.name}
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
      <WorkspaceInfoField labelText='Billing Status'>
        {workspace.billingStatus}
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
        {workspace.published ? 'Yes' : 'No'}
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
