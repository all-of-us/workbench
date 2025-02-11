import * as React from 'react';

import { WorkspaceResource } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { CohortActionMenu } from 'app/components/resources/cohort-action-menu';
import { CohortReviewActionMenu } from 'app/components/resources/cohort-review-action-menu';
import { ConceptSetActionMenu } from 'app/components/resources/concept-set-action-menu';
import { DatasetActionMenu } from 'app/components/resources/dataset-action-menu';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';
import {
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
} from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';
import { isValidBilling } from 'app/utils/workspace-utils';

export interface CommonActionMenuProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
}
interface Props extends CommonActionMenuProps {
  workspace: WorkspaceData;
}
export const ResourceListActionMenu = (props: Props) => {
  const { resource, workspace } = props;

  const inactiveBilling = !isValidBilling(workspace);

  return cond(
    [isCohort(resource), () => <CohortActionMenu {...props} />],
    [isCohortReview(resource), () => <CohortReviewActionMenu {...props} />],
    [isConceptSet(resource), () => <ConceptSetActionMenu {...props} />],
    [
      isDataSet(resource),
      () => <DatasetActionMenu {...{ ...props, inactiveBilling }} />,
    ],
    [
      isNotebook(resource),
      () => (
        <NotebookActionMenu
          {...props}
          disableDuplicate={inactiveBilling}
          useAppFilesListIcon={false}
        />
      ),
    ],
    null
  );
};
