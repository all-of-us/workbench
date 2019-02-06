import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

import {WorkbenchAccessTasks, WorkbenchAccessTasksProps} from './component';

describe('WorkbenchAccessTasks', () => {
  let props: WorkbenchAccessTasksProps;

  const component = () => {
    return mount<WorkbenchAccessTasks, WorkbenchAccessTasksProps, {}>
    (<WorkbenchAccessTasks {...props}/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    props  = {
      eraCommonsLinked: false,
      eraCommonsError: '',
      trainingCompleted: false,
    }
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show an error if passed an error message', () => {
    const errorMessage = 'error message!';
    props.eraCommonsError = errorMessage;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="era-commons-error"]').first().text())
        .toContain(errorMessage);
  });

});
