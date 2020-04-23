import {mount} from 'enzyme';
import * as React from 'react';

import {wizardStore} from 'app/cohort-search/search-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {NodeProp, TreeNode} from './tree-node.component';

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
  type: 'PM'
} as NodeProp;
describe('TreeNode', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
    wizardStore.next({});
  });
  it('should create', () => {
    const wrapper = mount(<TreeNode autocompleteSelection={undefined}
                                      groupSelections={[]}
                                      node={treeNodeStub}
                                      scrollToMatch={() => {}}
                                      searchTerms={''}
                                      select={() => {}}
                                      selectedIds={[]}
                                      setAttributes={() => {}}/>);
    expect(wrapper).toBeTruthy();
  });
});
