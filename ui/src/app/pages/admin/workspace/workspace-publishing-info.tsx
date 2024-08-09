import * as React from 'react';
import { useEffect, useState } from 'react';

import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { Select } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { FeaturedWorkspaceCategoryOptions } from 'app/pages/admin/admin-featured-category-options';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import { WorkspaceInfoField } from './workspace-info-field';

interface PublishingProps {
  workspace: Workspace;
  reload: () => Promise<void>;
}

export const WorkspacePublishingInfo = ({
  workspace,
  reload,
}: PublishingProps) => {
  const { enablePublishedWorkspacesViaDb } = serverConfigStore.get().config;

  const [featuredCategory, setFeaturedCategory] = useState(
    workspace.featuredCategory
  );
  const [featuredCategoryLoading, setFeaturedCategoryLoading] = useState(false);

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
    <WorkspaceInfoField labelText='Workspace Published'>
      {enablePublishedWorkspacesViaDb ? (
        <>
          <Select
            key={featuredCategory || 'placeholder'}
            value={featuredCategory}
            placeholder='Select a category...'
            isDisabled={featuredCategoryLoading}
            options={FeaturedWorkspaceCategoryOptions}
            onChange={(v: FeaturedWorkspaceCategory) => setFeaturedCategory(v)}
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
            disabled={featuredCategoryLoading || !workspace.featuredCategory}
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
      ) : workspace.published ? (
        'Yes'
      ) : (
        'No'
      )}
    </WorkspaceInfoField>
  );
};
