import {shallow} from 'enzyme';
import * as React from 'react';

import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {CohortSearch} from './cohort-search.component';

describe('CohortSearch', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    queryParamsStore.next({
      cohortId: 'test-id'
    });
  });
  it('should render', () => {
    const wrapper = shallow(<CohortSearch setCohortChanged={() => {}} setShowWarningModal={() => {}} setUpdatingCohort={() => {}}/>);
    expect(wrapper).toBeTruthy();
  });
});
