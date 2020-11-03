import * as fp from 'lodash/fp';

import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import colors from 'app/styles/colors';
import {ResourceType, WorkspaceResource} from 'generated/fetch';
import {formatWorkspaceResourceDisplayDate, switchCase} from './index';
import {encodeURIComponentStrict} from './navigation';

const isCohort = (resource: WorkspaceResource): boolean => !!resource.cohort;
const isCohortReview = (resource: WorkspaceResource): boolean => !!resource.cohortReview;
const isConceptSet = (resource: WorkspaceResource): boolean => !!resource.conceptSet;
const isDataSet = (resource: WorkspaceResource): boolean => !!resource.dataSet;
const isNotebook = (resource: WorkspaceResource): boolean => !!resource.notebook;

const getModifiedDate = (resource: WorkspaceResource): string => formatWorkspaceResourceDisplayDate(resource.modifiedTime);

function toDisplay(resourceType: ResourceType): string {
  return switchCase(resourceType, [
      [ResourceType.COHORT, 'Cohort'],
      [ResourceType.COHORTREVIEW, 'Cohort Review'],
      [ResourceType.COHORTSEARCHGROUP, 'Group'],
      [ResourceType.COHORTSEARCHITEM, 'Item'],
      [ResourceType.CONCEPTSET, 'Concept Set'],
      [ResourceType.DATASET, 'Dataset'],
      [ResourceType.NOTEBOOK, 'Notebook'],
      [ResourceType.WORKSPACE, 'Workspace'],
  ]);
}

const getTypeString = (resource: WorkspaceResource): string => toDisplay(getType(resource));

function getColor(resource: WorkspaceResource): string {
  return fp.cond([
      [isCohort, () => colors.resourceCardHighlights.cohort],
      [isCohortReview, () => colors.resourceCardHighlights.cohortReview],
      [isConceptSet, () => colors.resourceCardHighlights.conceptSet],
      [isDataSet, () => colors.resourceCardHighlights.dataSet],
      [isNotebook, () => colors.resourceCardHighlights.notebook],
  ])(resource);
}

function getDescription(resource: WorkspaceResource): string {
  return fp.cond([
      [isCohort, r => r.cohort.description],
      [isCohortReview, r => r.cohortReview.description],
      [isConceptSet, r => r.conceptSet.description],
      [isDataSet, r => r.dataSet.description],
      [isNotebook, fp.stubString /* notebooks don't have descriptions */]
  ])(resource);
}

function getDisplayName(resource: WorkspaceResource): string {
  return fp.cond([
      [isCohort, r => r.cohort.name],
      [isCohortReview, r => r.cohortReview.cohortName],
      [isConceptSet, r => r.conceptSet.name],
      [isDataSet, r => r.dataSet.name],
      [isNotebook, r => dropNotebookFileSuffix(r.notebook.name)],
  ])(resource);
}

function getId(resource: WorkspaceResource): number {
  // Notebooks do not have IDs
  return fp.cond([
      [isCohort, r => r.cohort.id],
      [isCohortReview, r => r.cohortReview.cohortReviewId],
      [isConceptSet, r => r.conceptSet.id],
      [isDataSet, r => r.dataSet.id],
  ])(resource);
}

function getResourceUrl(resource: WorkspaceResource): string {
  const {workspaceNamespace, workspaceFirecloudName} = resource;
  const workspacePrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}`;

  return fp.cond([
      [isCohort, r => `${workspacePrefix}/data/cohorts/build?cohortId=${r.cohort.id}`],
      [isCohortReview, r => `${workspacePrefix}/data/cohorts/${r.cohortReview.cohortId}/review`],
      [isConceptSet, r => `${workspacePrefix}/data/concepts/sets/${r.conceptSet.id}`],
      [isDataSet, r => `${workspacePrefix}/data/data-sets/${r.dataSet.id}`],
      [isNotebook, r => `${workspacePrefix}/notebooks/preview/${encodeURIComponentStrict(r.notebook.name)}`],
  ])(resource);
}

function getType(resource: WorkspaceResource): ResourceType {
  return fp.cond([
      [isCohort, () => ResourceType.COHORT],
      [isCohortReview, () => ResourceType.COHORTREVIEW],
      [isConceptSet, () => ResourceType.CONCEPTSET],
      [isDataSet, () => ResourceType.DATASET],
      [isNotebook, () => ResourceType.NOTEBOOK],
  ])(resource);
}

export {
    isCohort,
    isCohortReview,
    isConceptSet,
    isDataSet,
    isNotebook,
    getModifiedDate,
    getTypeString,
    toDisplay,
    getColor,
    getDescription,
    getDisplayName,
    getId,
    getResourceUrl,
    getType,
};
