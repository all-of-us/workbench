import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi, Domain } from 'generated/fetch';

import { render } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortCriteriaStore,
  currentConceptStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';

import {
  CohortBuilderServiceStub,
  VersionedSurveyStubVariables,
} from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { NodeProp, TreeNode } from './tree-node';

const treeNodeStub = {
  children: [],
  code: '',
  conceptId: 903133,
  count: 0,
  domainId: 'Measurement',
  group: false,
  hasAttributes: true,
  id: 316305,
  name: 'Height Detail',
  parentId: 0,
  predefinedAttributes: null,
  selectable: true,
  subtype: 'HEIGHT',
  type: 'PM',
} as NodeProp;

const surveyCOPETreeNodeStub = {
  ...VersionedSurveyStubVariables[0],
  children: [],
} as NodeProp;
describe('TreeNode', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentCohortCriteriaStore.next([]);
    currentConceptStore.next([]);
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });
  it('should create', () => {
    const { container } = render(
      <TreeNode
        autocompleteSelection={undefined}
        groupSelections={[]}
        node={treeNodeStub}
        scrollToMatch={() => {}}
        searchTerms=''
        select={() => {}}
        selectedIds={[]}
        source='cohort'
        setAttributes={() => {}}
        domain={Domain.OBSERVATION}
      />
    );
    expect(container).not.toBeEmpty();
  });
  it('should display Versioned if SURVEY is COPE', async () => {
    const { container } = render(
      <TreeNode
        autocompleteSelection={undefined}
        groupSelections={[]}
        node={surveyCOPETreeNodeStub}
        scrollToMatch={() => {}}
        searchTerms=''
        select={() => {}}
        selectedIds={[]}
        setAttributes={() => {}}
        domain={Domain.SURVEY}
        versionedSurveyIds={VersionedSurveyStubVariables.map(({ id }) => id)}
      />
    );
    expect(container.textContent).toContain(
      'COVID-19 Participant Experience (COPE) Survey -  Versioned'
    );
  });
});
