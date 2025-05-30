import * as fp from 'lodash/fp';
import validate from 'validate.js';

import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  ResourceType,
  WorkspaceAccessLevel,
  WorkspaceResource,
} from 'generated/fetch';

import { appendJupyterNotebookFileSuffix } from 'app/pages/analysis/util';
import { analysisTabPath, dataTabPath } from 'app/routing/utils';

import { encodeURIComponentStrict, UrlObj } from './navigation';
import { WorkspaceData } from './workspace-data';

export const isCohort = (resource: WorkspaceResource): boolean =>
  !!resource.cohort;
export const isCohortReview = (resource: WorkspaceResource): boolean =>
  !!resource.cohortReview;
export const isConceptSet = (resource: WorkspaceResource): boolean =>
  !!resource.conceptSet;
export const isDataSet = (resource: WorkspaceResource): boolean =>
  !!resource.dataSet;
export const isNotebook = (resource: WorkspaceResource): boolean =>
  !!resource.notebook;

export function toDisplay(resourceType: ResourceType): string {
  return fp.cond([
    [(rt) => rt === ResourceType.COHORT, () => 'Cohort'],
    [(rt) => rt === ResourceType.COHORT_REVIEW, () => 'Cohort Review'],
    [(rt) => rt === ResourceType.CONCEPT_SET, () => 'Concept Set'],
    [(rt) => rt === ResourceType.DATASET, () => 'Dataset'],
    [(rt) => rt === ResourceType.NOTEBOOK, () => 'Notebook'],

    [(rt) => rt === ResourceType.COHORT_SEARCH_GROUP, () => 'Group'],
    [(rt) => rt === ResourceType.COHORT_SEARCH_ITEM, () => 'Item'],
    [(rt) => rt === ResourceType.WORKSPACE, () => 'Workspace'],
  ])(resourceType);
}

export function getDescription(resource: WorkspaceResource): string {
  return fp.cond([
    [isCohort, (r) => r.cohort.description],
    [isCohortReview, (r) => r.cohortReview.description],
    [isConceptSet, (r) => r.conceptSet.description],
    [isDataSet, (r) => r.dataSet.description],
    [isNotebook, fp.stubString /* notebooks don't have descriptions */],
  ])(resource);
}

export function getDisplayName(resource: WorkspaceResource): string {
  return fp.cond([
    [isCohort, (r) => r.cohort.name],
    [isCohortReview, (r) => r.cohortReview.cohortName],
    [isConceptSet, (r) => r.conceptSet.name],
    [isDataSet, (r) => r.dataSet.name],
    [isNotebook, (r) => r.notebook.name],
  ])(resource);
}

export function getId(resource: WorkspaceResource): number {
  // Notebooks do not have IDs
  return fp.cond([
    [isCohort, (r) => r.cohort.id],
    [isCohortReview, (r) => r.cohortReview.cohortReviewId],
    [isConceptSet, (r) => r.conceptSet.id],
    [isDataSet, (r) => r.dataSet.id],
  ])(resource);
}

export function getResourceUrl(resource: WorkspaceResource): UrlObj {
  const { workspaceNamespace, workspaceFirecloudName } = resource;
  const dataTabPrefix = dataTabPath(workspaceNamespace, workspaceFirecloudName);
  const analysisTabPrefix = analysisTabPath(
    workspaceNamespace,
    workspaceFirecloudName
  );

  return fp.cond([
    [
      isCohort,
      (r) => ({
        url: `${dataTabPrefix}/cohorts/build`,
        queryParams: { cohortId: r.cohort.id },
      }),
    ],
    [
      isCohortReview,
      (r) => ({
        url: `${dataTabPrefix}/cohorts/${r.cohortReview.cohortId}/reviews/${r.cohortReview.cohortReviewId}`,
      }),
    ],
    [
      isConceptSet,
      (r) => ({
        url: `${dataTabPrefix}/concepts/sets/${r.conceptSet.id}`,
      }),
    ],
    [isDataSet, (r) => ({ url: `${dataTabPrefix}/data-sets/${r.dataSet.id}` })],
    [
      isNotebook,
      (r) => ({
        url: `${analysisTabPrefix}/preview/${encodeURIComponentStrict(
          r.notebook.name
        )}`,
      }),
    ],
  ])(resource);
}

export function getType(resource: WorkspaceResource): ResourceType {
  return fp.cond([
    [isCohort, () => ResourceType.COHORT],
    [isCohortReview, () => ResourceType.COHORT_REVIEW],
    [isConceptSet, () => ResourceType.CONCEPT_SET],
    [isDataSet, () => ResourceType.DATASET],
    [isNotebook, () => ResourceType.NOTEBOOK],
  ])(resource);
}

export const getTypeString = (resource: WorkspaceResource): string =>
  toDisplay(getType(resource));

export function convertToResource(
  inputResource: FileDetail | Cohort | CohortReview | ConceptSet | DataSet,
  resourceType: ResourceType,
  workspace: WorkspaceData
): WorkspaceResource {
  const {
    namespace,
    terraName,
    accessLevel,
    accessTierShortName,
    adminLocked,
    cdrVersionId,
  } = workspace;
  return {
    workspaceNamespace: namespace,
    workspaceFirecloudName: terraName,
    permission: WorkspaceAccessLevel[accessLevel],
    accessTierShortName,
    cdrVersionId,
    cohort:
      resourceType === ResourceType.COHORT ? (inputResource as Cohort) : null,
    cohortReview:
      resourceType === ResourceType.COHORT_REVIEW
        ? (inputResource as CohortReview)
        : null,
    conceptSet:
      resourceType === ResourceType.CONCEPT_SET
        ? (inputResource as ConceptSet)
        : null,
    dataSet:
      resourceType === ResourceType.DATASET ? (inputResource as DataSet) : null,
    notebook:
      resourceType === ResourceType.NOTEBOOK
        ? (inputResource as FileDetail)
        : null,
    lastModifiedEpochMillis: inputResource.lastModifiedTime,
    lastModifiedBy: inputResource.lastModifiedBy,
    adminLocked,
  };
}

export const convertToResources = (
  notebookList: FileDetail[],
  workspace: WorkspaceData
) =>
  notebookList.map((notebook) =>
    convertToResource(notebook, ResourceType.NOTEBOOK, workspace)
  );

/* Name validation for resources.
 *
 * Notebooks have characters that are disallowed. Specifically, Jupyter only disallows :/\
 * but Terra disallows a larger list of characters.  Those characters are listed in
 * `const notebookNameValidator` in this file:
 * https://github.com/DataBiosphere/terra-ui/blob/dev/src/components/notebook-utils.js#L42
 *
 * Other AoU resource types do not have the same name restriction and only block slashes.
 */
export const nameValidationFormat = (
  existingNames: string[],
  resourceType: ResourceType
) =>
  resourceType === ResourceType.NOTEBOOK
    ? {
        presence: { allowEmpty: false },
        format: {
          pattern: /^[^@#$%*+=?,[\]:;/\\]*$/,
          message:
            "can't contain these characters: @ # $ % * + = ? , [ ] : ; / \\ ",
        },
        exclusion: {
          within: existingNames,
          message: 'already exists',
        },
      }
    : {
        presence: { allowEmpty: false },
        format: {
          pattern: /^[^\/]*$/,
          message: "can't contain a slash",
        },
        exclusion: {
          within: existingNames,
          message: 'already exists',
        },
      };

export const validateNewNotebookName = (
  name: string,
  existingNames: string[],
  fieldName: string = 'name' // error validators sometimes depend on the field name
) =>
  validate(
    // append the Jupyter suffix if missing, to both the user-chosen name and the list of existing names
    { [fieldName]: appendJupyterNotebookFileSuffix(name) },
    {
      [fieldName]: nameValidationFormat(
        existingNames?.map(appendJupyterNotebookFileSuffix),
        ResourceType.NOTEBOOK
      ),
    }
  );

export function canWrite(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER' || resource.permission === 'WRITER';
}

export function canDelete(resource: WorkspaceResource): boolean {
  return resource.permission === 'OWNER' || resource.permission === 'WRITER';
}
