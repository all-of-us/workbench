import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {workspaceStubs} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceCard} from './workspace-card';

describe('WorkspaceCard', () => {
  const reload = jest.fn();

  const component = (accessLevel: WorkspaceAccessLevel) => {
    return mount(
      <WorkspaceCard
       accessLevel={accessLevel}
       reload={reload}
       workspace={workspaceStubs[0]}
      />,
      {attachTo: document.getElementById('root')}
    );
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    serverConfigStore.set({config: {gsuiteDomain: 'abc'}});
  });

  it('disables sharing for non-owners', async() => {
    const wrapper = component(WorkspaceAccessLevel.WRITER);
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.exists('[data-test-id="workspace-share-modal"]')).toBeFalsy();

    // Click the snowman menu.
    wrapper.find('[data-test-id="workspace-card-menu"]').first().simulate('click');
    const shareEl = wrapper.find('[data-test-id="Share-menu-item"]').first();

    // Hover should show the disabled tooltip.
    shareEl.simulate('mouseenter');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="workspace-share-disabled-tooltip"]')).toBeTruthy();

    // The share modal should not open on click.
    shareEl.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="workspace-share-modal"]')).toBeFalsy();
  });

});
