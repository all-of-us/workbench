import * as React from 'react';
import * as fp from 'lodash/fp';

import { BillingStatus, WorkspaceResource } from 'generated/fetch';

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

function renderResourceCard(props: RenderResourceCardProps) {
  const { resource } = props;
  const inactiveBilling =
    resource.workspaceBillingStatus === BillingStatus.INACTIVE;

  return fp.cond([
    [isCohort, () => <CohortResourceCard {...props} />],
    [isCohortReview, () => <CohortReviewResourceCard {...props} />],
    [isConceptSet, () => <ConceptSetResourceCard {...props} />],
    [
      isDataSet,
      () => <DatasetResourceCard {...{ ...props, inactiveBilling }} />,
    ],
    [
      isNotebook,
      () => (
        <NotebookActionMenu {...props} disableDuplicate={inactiveBilling} />
      ),
    ],
  ])(resource);
}

export { renderResourceCard };
