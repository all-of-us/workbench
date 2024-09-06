import * as React from 'react';
import * as fp from 'lodash/fp';

import { ResourceType } from 'generated/fetch';

import { TanagraWorkspaceResource } from 'app/pages/data/tanagra-dev/data-component-tanagra';
import colors from 'app/styles/colors';

export const isCohort = (resource: TanagraWorkspaceResource): boolean =>
  !!resource.cohortTanagra;
export const isCohortReview = (resource: TanagraWorkspaceResource): boolean =>
  !!resource.reviewTanagra;
export const isFeatureSet = (resource: TanagraWorkspaceResource): boolean =>
  !!resource.featureSetTanagra;

export function toDisplay(resourceType: ResourceType): string {
  return fp.cond([
    [(rt) => rt === ResourceType.COHORT, () => 'Cohort'],
    [(rt) => rt === ResourceType.COHORT_REVIEW, () => 'Cohort Review'],
    [(rt) => rt === ResourceType.CONCEPT_SET, () => 'Feature Set'],
    [(rt) => rt === ResourceType.DATASET, () => 'Dataset'],
    [(rt) => rt === ResourceType.NOTEBOOK, () => 'Notebook'],

    [(rt) => rt === ResourceType.COHORT_SEARCH_GROUP, () => 'Group'],
    [(rt) => rt === ResourceType.COHORT_SEARCH_ITEM, () => 'Item'],
    [(rt) => rt === ResourceType.WORKSPACE, () => 'Workspace'],
  ])(resourceType);
}

export function getCreatedBy(resource: TanagraWorkspaceResource): string {
  return fp.cond([
    [isCohort, (r) => r.cohortTanagra.createdBy],
    [isCohortReview, (r) => r.reviewTanagra.createdBy],
    [isFeatureSet, (r) => r.featureSetTanagra.createdBy],
  ])(resource);
}

export function getDisplayName(resource: TanagraWorkspaceResource): string {
  return fp.cond([
    [isCohort, (r) => r.cohortTanagra.displayName],
    [isCohortReview, (r) => r.reviewTanagra.displayName],
    [isFeatureSet, (r) => r.featureSetTanagra.displayName],
  ])(resource);
}

export function getType(resource: TanagraWorkspaceResource): ResourceType {
  return fp.cond([
    [isCohort, () => ResourceType.COHORT],
    [isCohortReview, () => ResourceType.COHORT_REVIEW],
    [isFeatureSet, () => ResourceType.CONCEPT_SET],
  ])(resource);
}

export const getTypeString = (resource: TanagraWorkspaceResource): string =>
  toDisplay(getType(resource));

export const StyledResourceType = (props: {
  resource: TanagraWorkspaceResource;
}) => {
  const { resource } = props;

  function getColor(): string {
    return fp.cond([
      [isCohort, () => colors.resourceCardHighlights.cohort],
      [isCohortReview, () => colors.resourceCardHighlights.cohortReview],
      [isFeatureSet, () => colors.resourceCardHighlights.conceptSet],
    ])(resource);
  }
  return (
    <div
      data-test-id='card-type'
      style={{
        height: '22px',
        width: '9rem',
        paddingLeft: '10px',
        paddingRight: '10px',
        borderRadius: '2px',
        display: 'flex',
        justifyContent: 'left',
        color: colors.white,
        fontFamily: 'Montserrat, sans-serif',
        fontSize: '12px',
        fontWeight: 500,
        backgroundColor: getColor(),
      }}
    >
      {fp.startCase(fp.camelCase(getTypeString(resource)))}
    </div>
  );
};
