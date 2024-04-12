import '@testing-library/jest-dom';

import * as React from 'react';

import {
  CohortBuilderApi,
  CriteriaSubType,
  CriteriaType,
  Domain,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CriteriaTree } from './tree';

describe('CriteriaTree', () => {
  const criteriaStub = {
    childCount: 197614,
    code: '',
    conceptId: 903133,
    count: 197614,
    domainId: Domain.PHYSICAL_MEASUREMENT.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 328005,
    standard: false,
    name: 'Height',
    parentCount: 0,
    parentId: 0,
    path: '',
    selectable: true,
    subtype: CriteriaSubType.HEIGHT.toString(),
    type: CriteriaType.PPI.toString(),
    value: '',
  };
  const searchTermsStub = 'example search';
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });
  it('should create', () => {
    render(
      <CriteriaTree
        node={criteriaStub}
        domain={Domain.SURVEY}
        searchTerms={searchTermsStub}
      />
    );
    expect(screen.getByDisplayValue(searchTermsStub)).toBeInTheDocument();
  });
});
