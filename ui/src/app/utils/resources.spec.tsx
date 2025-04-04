import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  ResourceType,
  WorkspaceResource,
} from 'generated/fetch';

import { analysisTabPath, dataTabPath } from 'app/routing/utils';

import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

import { stringifyUrl } from './navigation';
import {
  getDescription,
  getDisplayName,
  getId,
  getResourceUrl,
  getType,
  getTypeString,
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
  toDisplay,
} from './resources';

const COHORT_NAME = exampleCohortStubs[0].name;
const COHORT_DESCRIPTION = exampleCohortStubs[0].description;
const COHORT_ID = exampleCohortStubs[0].id;

const COHORT_REVIEW_DESCRIPTION = 'this is a review';
const COHORT_REVIEW_ID = 100;

const COHORT_REVIEW_COHORT_NAME = 'the cohort to review';
const COHORT_REVIEW_COHORT_ID = 101;

const CONCEPT_SET_NAME = 'testConceptSet1';
const CONCEPT_SET_DESCRIPTION = 'this is a concept set';
const CONCEPT_SET_ID = 200;

const DATA_SET_NAME = 'testDataSet1';
const DATA_SET_DESCRIPTION = 'this is a data set';
const DATA_SET_ID = 300;

const NOTEBOOK_NAME = 'testNotebook1.ipynb';
const NOTEBOOK_DISPLAY_NAME = 'testNotebook1.ipynb';

const testCohort = {
  ...stubResource,
  cohort: {
    name: COHORT_NAME,
    description: COHORT_DESCRIPTION,
    id: COHORT_ID,
  } as Cohort,
} as WorkspaceResource;

const testCohortReview = {
  ...stubResource,
  cohortReview: {
    cohortId: COHORT_REVIEW_COHORT_ID,
    cohortName: COHORT_REVIEW_COHORT_NAME,
    description: COHORT_REVIEW_DESCRIPTION,
    cohortReviewId: COHORT_REVIEW_ID,
  } as CohortReview,
} as WorkspaceResource;

const testConceptSet = {
  ...stubResource,
  conceptSet: {
    name: CONCEPT_SET_NAME,
    description: CONCEPT_SET_DESCRIPTION,
    id: CONCEPT_SET_ID,
  } as ConceptSet,
} as WorkspaceResource;

const testDataSet = {
  ...stubResource,
  dataSet: {
    name: DATA_SET_NAME,
    description: DATA_SET_DESCRIPTION,
    id: DATA_SET_ID,
  } as DataSet,
} as WorkspaceResource;

const testNotebook = {
  ...stubResource,
  notebook: { name: NOTEBOOK_NAME } as FileDetail,
} as WorkspaceResource;

describe('resources.tsx', () => {
  it('should identify resource types', () => {
    expect(isCohort(testCohort)).toBeTruthy();
    expect(getType(testCohort)).toEqual(ResourceType.COHORT);

    expect(isCohortReview(testCohort)).toBeFalsy();
    expect(isConceptSet(testCohort)).toBeFalsy();
    expect(isDataSet(testCohort)).toBeFalsy();
    expect(isNotebook(testCohort)).toBeFalsy();

    expect(isCohortReview(testCohortReview)).toBeTruthy();
    expect(getType(testCohortReview)).toEqual(ResourceType.COHORT_REVIEW);

    expect(isCohort(testCohortReview)).toBeFalsy();
    expect(isConceptSet(testCohortReview)).toBeFalsy();
    expect(isDataSet(testCohortReview)).toBeFalsy();
    expect(isNotebook(testCohortReview)).toBeFalsy();

    expect(isConceptSet(testConceptSet)).toBeTruthy();
    expect(getType(testConceptSet)).toEqual(ResourceType.CONCEPT_SET);

    expect(isCohort(testConceptSet)).toBeFalsy();
    expect(isCohortReview(testConceptSet)).toBeFalsy();
    expect(isDataSet(testConceptSet)).toBeFalsy();
    expect(isNotebook(testConceptSet)).toBeFalsy();

    expect(isDataSet(testDataSet)).toBeTruthy();
    expect(getType(testDataSet)).toEqual(ResourceType.DATASET);

    expect(isCohort(testDataSet)).toBeFalsy();
    expect(isCohortReview(testDataSet)).toBeFalsy();
    expect(isConceptSet(testDataSet)).toBeFalsy();
    expect(isNotebook(testDataSet)).toBeFalsy();

    expect(isNotebook(testNotebook)).toBeTruthy();
    expect(getType(testNotebook)).toEqual(ResourceType.NOTEBOOK);

    expect(isCohort(testNotebook)).toBeFalsy();
    expect(isCohortReview(testNotebook)).toBeFalsy();
    expect(isConceptSet(testNotebook)).toBeFalsy();
    expect(isDataSet(testNotebook)).toBeFalsy();
  });

  it('should return resource type strings', () => {
    expect(toDisplay(ResourceType.COHORT)).toBe('Cohort');
    expect(toDisplay(ResourceType.COHORT_REVIEW)).toBe('Cohort Review');
    expect(toDisplay(ResourceType.CONCEPT_SET)).toBe('Concept Set');
    expect(toDisplay(ResourceType.DATASET)).toBe('Dataset');
    expect(toDisplay(ResourceType.NOTEBOOK)).toBe('Notebook');

    expect(toDisplay(ResourceType.COHORT_SEARCH_GROUP)).toBe('Group');
    expect(toDisplay(ResourceType.COHORT_SEARCH_ITEM)).toBe('Item');
    expect(toDisplay(ResourceType.WORKSPACE)).toBe('Workspace');

    expect(getTypeString(testCohort)).toBe('Cohort');
    expect(getTypeString(testCohortReview)).toBe('Cohort Review');
    expect(getTypeString(testConceptSet)).toBe('Concept Set');
    expect(getTypeString(testDataSet)).toBe('Dataset');
    expect(getTypeString(testNotebook)).toBe('Notebook');
  });

  it('should return resource display names', () => {
    expect(getDisplayName(testCohort)).toBe(COHORT_NAME);
    expect(getDisplayName(testCohortReview)).toBe(COHORT_REVIEW_COHORT_NAME);
    expect(getDisplayName(testConceptSet)).toBe(CONCEPT_SET_NAME);
    expect(getDisplayName(testDataSet)).toBe(DATA_SET_NAME);
    expect(getDisplayName(testNotebook)).toBe(NOTEBOOK_DISPLAY_NAME);
  });

  it('should return resource descriptions', () => {
    expect(getDescription(testCohort)).toBe(COHORT_DESCRIPTION);
    expect(getDescription(testCohortReview)).toBe(COHORT_REVIEW_DESCRIPTION);
    expect(getDescription(testConceptSet)).toBe(CONCEPT_SET_DESCRIPTION);
    expect(getDescription(testDataSet)).toBe(DATA_SET_DESCRIPTION);
    // Notebooks don't have Descriptions
    expect(getDescription(testNotebook).trim()).toBeFalsy();
  });

  it('should return resource IDs', () => {
    expect(getId(testCohort)).toBe(COHORT_ID);
    expect(getId(testCohortReview)).toBe(COHORT_REVIEW_ID);
    expect(getId(testConceptSet)).toBe(CONCEPT_SET_ID);
    expect(getId(testDataSet)).toBe(DATA_SET_ID);
    // Notebooks don't have IDs
    expect(getId(testNotebook)).toBeFalsy();
  });

  it('should return resource URLs', () => {
    const { DEFAULT_WORKSPACE_NS, DEFAULT_WORKSPACE_TERRA_NAME } =
      WorkspaceStubVariables;
    const dataTabPrefix = dataTabPath(
      DEFAULT_WORKSPACE_NS,
      DEFAULT_WORKSPACE_TERRA_NAME
    );
    const analysisTabPrefix = analysisTabPath(
      DEFAULT_WORKSPACE_NS,
      DEFAULT_WORKSPACE_TERRA_NAME
    );
    const EXPECTED_COHORT_URL = `${dataTabPrefix}/cohorts/build?cohortId=${COHORT_ID}`;
    const EXPECTED_COHORT_REVIEW_URL = `${dataTabPrefix}/cohorts/${COHORT_REVIEW_COHORT_ID}/reviews/${COHORT_REVIEW_ID}`;
    const EXPECTED_CONCEPT_SET_URL = `${dataTabPrefix}/concepts/sets/${CONCEPT_SET_ID}`;
    const EXPECTED_DATA_SET_URL = `${dataTabPrefix}/data-sets/${DATA_SET_ID}`;
    const EXPECTED_NOTEBOOK_URL = `${analysisTabPrefix}/preview/${NOTEBOOK_NAME}`;

    expect(stringifyUrl(getResourceUrl(testCohort))).toBe(EXPECTED_COHORT_URL);
    expect(stringifyUrl(getResourceUrl(testCohortReview))).toBe(
      EXPECTED_COHORT_REVIEW_URL
    );
    expect(stringifyUrl(getResourceUrl(testConceptSet))).toBe(
      EXPECTED_CONCEPT_SET_URL
    );
    expect(stringifyUrl(getResourceUrl(testDataSet))).toBe(
      EXPECTED_DATA_SET_URL
    );
    expect(stringifyUrl(getResourceUrl(testNotebook))).toBe(
      EXPECTED_NOTEBOOK_URL
    );
  });
});
