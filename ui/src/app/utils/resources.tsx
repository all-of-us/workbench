import * as fp from 'lodash/fp';

import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {
    Cohort,
    CohortReview,
    ConceptSet,
    Dataset,
    FileDetail,
    ResourceType,
    WorkspaceAccessLevel,
    WorkspaceResource
} from 'generated/fetch';
import {encodeURIComponentStrict} from './navigation';
import {WorkspaceData} from './workspace-data';

const isCohort = (resource: WorkspaceResource): boolean => !!resource.cohort;
const isCohortReview = (resource: WorkspaceResource): boolean => !!resource.cohortReview;
const isConceptSet = (resource: WorkspaceResource): boolean => !!resource.conceptSet;
const isDataset = (resource: WorkspaceResource): boolean => !!resource.dataset;
const isNotebook = (resource: WorkspaceResource): boolean => !!resource.notebook;

function toDisplay(resourceType: ResourceType): string {
  return fp.cond([
      [rt => rt === ResourceType.COHORT, () => 'Cohort'],
      [rt => rt === ResourceType.COHORTREVIEW, () => 'Cohort Review'],
      [rt => rt === ResourceType.CONCEPTSET, () => 'Concept Set'],
      [rt => rt === ResourceType.DATASET, () => 'Dataset'],
      [rt => rt === ResourceType.NOTEBOOK, () => 'Notebook'],

      [rt => rt === ResourceType.COHORTSEARCHGROUP, () => 'Group'],
      [rt => rt === ResourceType.COHORTSEARCHITEM, () => 'Item'],
      [rt => rt === ResourceType.WORKSPACE, () => 'Workspace'],
  ])(resourceType);
}

const getTypeString = (resource: WorkspaceResource): string => toDisplay(getType(resource));

function getDescription(resource: WorkspaceResource): string {
  return fp.cond([
      [isCohort, r => r.cohort.description],
      [isCohortReview, r => r.cohortReview.description],
      [isConceptSet, r => r.conceptSet.description],
      [isDataset, r => r.dataset.description],
      [isNotebook, fp.stubString /* notebooks don't have descriptions */]
  ])(resource);
}

function getDisplayName(resource: WorkspaceResource): string {
  return fp.cond([
      [isCohort, r => r.cohort.name],
      [isCohortReview, r => r.cohortReview.cohortName],
      [isConceptSet, r => r.conceptSet.name],
      [isDataset, r => r.dataset.name],
      [isNotebook, r => dropNotebookFileSuffix(r.notebook.name)],
  ])(resource);
}

function getId(resource: WorkspaceResource): number {
  // Notebooks do not have IDs
  return fp.cond([
      [isCohort, r => r.cohort.id],
      [isCohortReview, r => r.cohortReview.cohortReviewId],
      [isConceptSet, r => r.conceptSet.id],
      [isDataset, r => r.dataset.id],
  ])(resource);
}

function getResourceUrl(resource: WorkspaceResource): string {
  const {workspaceNamespace, workspaceFirecloudName} = resource;
  const workspacePrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}`;

  return fp.cond([
      [isCohort, r => `${workspacePrefix}/data/cohorts/build?cohortId=${r.cohort.id}`],
      [isCohortReview, r => `${workspacePrefix}/data/cohorts/${r.cohortReview.cohortId}/review`],
      [isConceptSet, r => `${workspacePrefix}/data/concepts/sets/${r.conceptSet.id}`],
      [isDataset, r => `${workspacePrefix}/data/data-sets/${r.dataset.id}`],
      [isNotebook, r => `${workspacePrefix}/notebooks/preview/${encodeURIComponentStrict(r.notebook.name)}`],
  ])(resource);
}

function getType(resource: WorkspaceResource): ResourceType {
  return fp.cond([
      [isCohort, () => ResourceType.COHORT],
      [isCohortReview, () => ResourceType.COHORTREVIEW],
      [isConceptSet, () => ResourceType.CONCEPTSET],
      [isDataset, () => ResourceType.DATASET],
      [isNotebook, () => ResourceType.NOTEBOOK],
  ])(resource);
}

function convertToResource(
  inputResource: FileDetail | Cohort | CohortReview | ConceptSet | Dataset,
  resourceType: ResourceType,
  workspace: WorkspaceData): WorkspaceResource {
  const {namespace, id, accessLevel, cdrVersionId, billingStatus} = workspace;
  return {
    workspaceNamespace: namespace,
    workspaceFirecloudName: id,
    permission: WorkspaceAccessLevel[accessLevel],
    modifiedTime: inputResource.lastModifiedTime ? new Date(inputResource.lastModifiedTime).toString() : new Date().toDateString(),
    cdrVersionId,
    workspaceBillingStatus: billingStatus,
    cohort: resourceType === ResourceType.COHORT ? inputResource as Cohort : null,
    cohortReview: resourceType === ResourceType.COHORTREVIEW ? inputResource as CohortReview : null,
    conceptSet: resourceType === ResourceType.CONCEPTSET ? inputResource as ConceptSet : null,
    dataset: resourceType === ResourceType.DATASET ? inputResource as Dataset : null,
    notebook: resourceType === ResourceType.NOTEBOOK ? inputResource as FileDetail : null,
  };
}

export {
    isCohort,
    isCohortReview,
    isConceptSet,
    isDataset,
    isNotebook,
    getTypeString,
    toDisplay,
    getDescription,
    getDisplayName,
    getId,
    getResourceUrl,
    getType,
    convertToResource,
};
