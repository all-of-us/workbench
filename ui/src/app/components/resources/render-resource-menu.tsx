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

export interface ResourceActionMenuProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
}

// TODO: this is only used by resource-list.  move it there?
export const renderResourceMenu = (
  resource: WorkspaceResource,
  workspace: WorkspaceData,
  existingNameList: string[],
  onUpdate: () => Promise<void>
) => {
  const commonProps: ResourceActionMenuProps = {
    resource,
    existingNameList,
    onUpdate,
  };

  const inactiveBilling =
    resource.workspaceBillingStatus === BillingStatus.INACTIVE;

  return cond(
    [isCohort(resource), () => <CohortResourceCard {...commonProps} />],
    [
      isCohortReview(resource),
      () => <CohortReviewResourceCard {...commonProps} />,
    ],
    [isConceptSet(resource), () => <ConceptSetResourceCard {...commonProps} />],
    [
      isDataSet(resource),
      () => (
        <DatasetResourceCard
          {...{ ...commonProps, workspace, inactiveBilling }}
        />
      ),
    ],
    [
      isNotebook(resource),
      () => (
        <NotebookActionMenu
          {...commonProps}
          disableDuplicate={inactiveBilling}
        />
      ),
    ]
  );
};
