import * as React from 'react';
import { useEffect } from 'react';
import { Link } from 'react-router-dom';

import {
  FeaturedWorkspaceCategory,
  Workspace,
  WorkspaceActiveStatus,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Select } from 'app/components/inputs';
import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
  activeStatus: WorkspaceActiveStatus;
  reload: () => Promise<void>;
}
export const BasicInformation = ({
  workspace,
  activeStatus,
  reload,
}: Props) => {
  const [featuredCategory, setFeaturedCategory] =
    React.useState<FeaturedWorkspaceCategory>(workspace.featuredCategory);
  const [featuredCategoryLoading, setFeaturedCategoryLoading] =
    React.useState<boolean>(false);

  console.log('workspace.featuredCategory', workspace.featuredCategory);

  useEffect(() => {
    setFeaturedCategory(workspace.featuredCategory);
    setFeaturedCategoryLoading(false);
  }, [workspace.featuredCategory]);
  return (
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
            key={featuredCategory || 'placeholder'}
            value={featuredCategory || null}
            placeholder='Select a category...'
            isDisabled={featuredCategoryLoading}
            options={[
              {
                value: FeaturedWorkspaceCategory.DEMO_PROJECTS,
                label: 'Demo Projects',
              },
              {
                value: FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY,
                label: 'Phenotype Library',
              },
              {
                value: FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES,
                label: 'Tutorial Workspaces',
              },
              {
                value: FeaturedWorkspaceCategory.COMMUNITY,
                label: 'Community',
              },
            ]}
            onChange={(v) => setFeaturedCategory(v)}
          />
          <Button
            type='primary'
            disabled={
              featuredCategoryLoading ||
              !featuredCategory ||
              featuredCategory === workspace.featuredCategory
            }
            onClick={() => {
              setFeaturedCategoryLoading(true);
              workspaceAdminApi()
                .publishWorkspaceViaDB(workspace.namespace, {
                  category: featuredCategory,
                })
                .then(async () => {
                  await reload();
                });
            }}
          >
            Publish
          </Button>
          <Button
            type='secondaryOutline'
            disabled={featuredCategoryLoading || !workspace.featuredCategory}
            onClick={() => {
              setFeaturedCategory(undefined);
              setFeaturedCategoryLoading(true);
              workspaceAdminApi()
                .unpublishWorkspaceViaDB(workspace.namespace)
                .then(async () => {
                  await reload();
                });
            }}
          >
            Unpublish
          </Button>
          {featuredCategoryLoading && <Spinner size={36} />}
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
};
