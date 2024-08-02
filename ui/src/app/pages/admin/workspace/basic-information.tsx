import * as React from 'react';
import { useEffect } from 'react';
import { Link } from 'react-router-dom';

import {
  FeaturedWorkspaceCategory,
  Workspace,
  WorkspaceActiveStatus,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { Select } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { FeaturedWorkspaceCategoryOptions } from 'app/pages/admin/admin-featured-category-options';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
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
  const { enablePublishedWorkspacesViaDb } = serverConfigStore.get().config;

  useEffect(() => {
    setFeaturedCategory(workspace.featuredCategory);
    setFeaturedCategoryLoading(false);
  }, [workspace.featuredCategory]);

  const publishingDisabled =
    featuredCategoryLoading ||
    workspace.adminLocked ||
    !featuredCategory ||
    featuredCategory === workspace.featuredCategory;

  const getWorkspacePublishTooltip = () => {
    return cond(
      [
        featuredCategoryLoading,
        'Your workspace is loading, please wait until loading is completed before publishing.',
      ],
      [
        workspace.adminLocked,
        'This workspace is locked and cannot be published.',
      ],
      [!featuredCategory, 'Please select a category to publish the workspace.'],
      [
        featuredCategory === workspace.featuredCategory,
        'This workspace is already published in the selected category.',
      ]
    );
  };

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
          {!enablePublishedWorkspacesViaDb &&
            (workspace.published ? 'Yes' : 'No')}
          {enablePublishedWorkspacesViaDb && (
            <>
              <Select
                key={featuredCategory || 'placeholder'}
                value={featuredCategory}
                placeholder='Select a category...'
                isDisabled={featuredCategoryLoading}
                options={FeaturedWorkspaceCategoryOptions}
                onChange={(v) => setFeaturedCategory(v)}
              />
              <TooltipTrigger
                disabled={!publishingDisabled}
                content={getWorkspacePublishTooltip()}
              >
                <Button
                  type='primary'
                  disabled={publishingDisabled}
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
              </TooltipTrigger>
              <Button
                type='secondaryOutline'
                disabled={
                  featuredCategoryLoading || !workspace.featuredCategory
                }
                onClick={() => {
                  setFeaturedCategory(null);
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
            </>
          )}
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
