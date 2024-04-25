import * as React from 'react';

import { BillingStatus, WorkspaceResource } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';
import { CohortResourceCard } from 'app/pages/data/cohort/cohort-resource-card';
import { CohortReviewResourceCard } from 'app/pages/data/cohort-review/cohort-review-resource-card';
import { ConceptSetResourceCard } from 'app/pages/data/concept/concept-set-resource-card';
import { DatasetResourceCard } from 'app/pages/data/data-set/dataset-resource-card';
import {
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
} from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

interface RenderResourceCardProps {
  resource: WorkspaceResource;
  workspace: WorkspaceData;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  menuOnly: boolean;
}

export const renderResourceMenu = (
  resource: WorkspaceResource,
  workspace: WorkspaceData,
  existingNameList: string[],
  onUpdate: () => Promise<void>
) => {
  const props: RenderResourceCardProps = {
    resource,
    workspace,
    existingNameList,
    onUpdate,
    menuOnly: true,
  };

  const inactiveBilling =
    resource.workspaceBillingStatus === BillingStatus.INACTIVE;

  return cond(
    [isCohort(resource), () => <CohortResourceCard {...props} />],
    [isCohortReview(resource), () => <CohortReviewResourceCard {...props} />],
    [isConceptSet(resource), () => <ConceptSetResourceCard {...props} />],
    [
      isDataSet(resource),
      () => <DatasetResourceCard {...{ ...props, inactiveBilling }} />,
    ],
    [
      isNotebook(resource),
      () => (
        <NotebookActionMenu {...props} disableDuplicate={inactiveBilling} />
      ),
    ]
  );
};
