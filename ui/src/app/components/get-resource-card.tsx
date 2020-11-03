import * as fp from 'lodash/fp';
import * as React from 'react';

import {NotebookResourceCard} from 'app/pages/analysis/notebook-resource-card';
import {CohortResourceCard} from 'app/pages/data/cohort/cohort-resource-card';
import {ConceptSetResourceCard} from 'app/pages/data/concept/concept-set-resource-card';
import {DatasetResourceCard} from 'app/pages/data/data-set/dataset-resource-card';
import {isCohort, isCohortReview, isConceptSet, isDataSet, isNotebook} from 'app/utils/resources';
import {BillingStatus, WorkspaceResource} from 'generated/fetch';

interface GetResourceCardProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: Function;
}

function getResourceCard(props: GetResourceCardProps) {
  const {resource} = props;
  const inactiveBilling = (resource.workspaceBillingStatus === BillingStatus.INACTIVE);

  return fp.cond([
      [isCohort, () => <CohortResourceCard {...props}/>],
      [isCohortReview, () => <CohortResourceCard {...props}/>],
      [isConceptSet, () => <ConceptSetResourceCard {...props}/>],
      [isDataSet, () => <DatasetResourceCard {...props} disableExportToNotebook={inactiveBilling}/>],
      [isNotebook, () => <NotebookResourceCard {...props} disableDuplicate={inactiveBilling}/>]
  ])(resource);
}

export {
  getResourceCard
};
