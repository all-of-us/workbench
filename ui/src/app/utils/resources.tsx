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

import { cond, switchCase } from '@terra-ui-packages/core-utils';

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
  return switchCase(
    resourceType,
    [ResourceType.COHORT, () => 'Cohort'],
    [ResourceType.COHORTREVIEW, () => 'Cohort Review'],
    [ResourceType.CONCEPTSET, () => 'Concept Set'],
    [ResourceType.DATASET, () => 'Dataset'],
    [ResourceType.NOTEBOOK, () => 'Notebook'],

    [ResourceType.COHORTSEARCHGROUP, () => 'Group'],
    [ResourceType.COHORTSEARCHITEM, () => 'Item'],
    [ResourceType.WORKSPACE, () => 'Workspace']
  );
}

export function getDescription(resource: WorkspaceResource): string {
  return cond(
    [isCohort(resource), () => resource.cohort.description],
    [isCohortReview(resource), () => resource.cohortReview.description],
    [isConceptSet(resource), () => resource.conceptSet.description],
    [isDataSet(resource), () => resource.dataSet.description],
    [
      // notebooks don't have descriptions
      isNotebook(resource),
      () => '',
    ]
  );
}

export function getDisplayName(resource: WorkspaceResource): string {
  return cond(
    [isCohort(resource), () => resource.cohort.name],
    [isCohortReview(resource), () => resource.cohortReview.cohortName],
    [isConceptSet(resource), () => resource.conceptSet.name],
    [isDataSet(resource), () => resource.dataSet.name],
    [isNotebook(resource), () => resource.notebook.name]
  );
}

export function getId(resource: WorkspaceResource): number {
  // Notebooks do not have IDs
  return cond(
    [isCohort(resource), () => resource.cohort.id],
    [isCohortReview(resource), () => resource.cohortReview.cohortReviewId],
    [isConceptSet(resource), () => resource.conceptSet.id],
    [isDataSet(resource), () => resource.dataSet.id]
  );
}

export function getResourceUrl(resource: WorkspaceResource): UrlObj {
  const { workspaceNamespace, workspaceFirecloudName } = resource;
  const dataTabPrefix = dataTabPath(workspaceNamespace, workspaceFirecloudName);
  const analysisTabPrefix = analysisTabPath(
    workspaceNamespace,
    workspaceFirecloudName
  );

  return cond(
    [
      isCohort(resource),
      () => ({
        url: `${workspacePrefix}/data/cohorts/build`,
        queryParams: { cohortId: resource.cohort.id },
      }),
    ],
    [
      isCohortReview(resource),
      () => ({
        url: `${workspacePrefix}/data/cohorts/${resource.cohortReview.cohortId}/reviews/${resource.cohortReview.cohortReviewId}`,
      }),
    ],
    [
      isConceptSet(resource),
      () => ({
        url: `${workspacePrefix}/data/concepts/sets/${resource.conceptSet.id}`,
      }),
    ],
    [
      isDataSet(resource),
      () => ({
        url: `${workspacePrefix}/data/data-sets/${resource.dataSet.id}`,
      }),
    ],
    [
      isNotebook(resource),
      () => ({
        url: `${workspacePrefix}/${analysisTabName}/preview/${encodeURIComponentStrict(
          resource.notebook.name
        )}`,
      }),
    ]
  );
}

export function getType(resource: WorkspaceResource): ResourceType {
  return cond(
    [isCohort(resource), () => ResourceType.COHORT],
    [isCohortReview(resource), () => ResourceType.COHORTREVIEW],
    [isConceptSet(resource), () => ResourceType.CONCEPTSET],
    [isDataSet(resource), () => ResourceType.DATASET],
    [isNotebook(resource), () => ResourceType.NOTEBOOK]
  );
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
    id,
    accessLevel,
    accessTierShortName,
    adminLocked,
    cdrVersionId,
    billingStatus,
  } = workspace;
  return {
    workspaceNamespace: namespace,
    workspaceFirecloudName: id,
    permission: WorkspaceAccessLevel[accessLevel],
    accessTierShortName,
    cdrVersionId,
    workspaceBillingStatus: billingStatus,
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
