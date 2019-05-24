import {mount} from 'enzyme';
import * as React from 'react';

import {RenameModal} from './rename-modal';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {WorkspacesApi} from 'generated/fetch';

describe('RenameModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
  });

  it('should render', () => {
    const wrapper = mount(<RenameModal
      notebookName='a'
      onCancel={() => {}}
      onRename={() => {}}
      workspace={{namespace: 'a', name: 'b'}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
