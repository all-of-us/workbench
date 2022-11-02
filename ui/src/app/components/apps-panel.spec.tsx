import * as React from 'react';
import { mount } from 'enzyme';

import { currentWorkspaceStore } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { workspaceDataStub, workspaceStubs } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

describe('AppsPanel', () => {
  const component = async () => {
    return mount(
      <AppsPanel
        workspace={workspaceStubs[0]}
        onClickRuntimeConf={() => ({})}
        onClickDeleteRuntime={() => ({})}
      />
    );
  };

  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({ config: defaultServerConfig });
  });

  it('should render', async () => {
    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
  });
});
