import * as React from 'react';

import {
  FeaturedWorkspacesConfigApi,
  ProfileApi,
  WorkspacesApi,
} from 'generated/fetch';

import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { FeaturedWorkspacesConfigApiStub } from 'testing/stubs/featured-workspaces-config-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { buildWorkspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceLibrary } from './workspace-library';

describe('WorkspaceLibrary', () => {
  let publishedWorkspaceStubs = [];
  let PHENOTYPE_LIBRARY_WORKSPACES;
  let TUTORIAL_WORKSPACE;

  const suffixes = [' Phenotype Library', ' Tutorial Workspace'];

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const component = () => {
    return mountWithRouter(<WorkspaceLibrary {...props} />);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(
      FeaturedWorkspacesConfigApi,
      new FeaturedWorkspacesConfigApiStub()
    );
    publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map((w) => ({
      ...w,
      published: true,
    }));

    serverConfigStore.set({
      config: defaultServerConfig,
    });

    PHENOTYPE_LIBRARY_WORKSPACES = publishedWorkspaceStubs[0];
    TUTORIAL_WORKSPACE = publishedWorkspaceStubs[1];
  });

  it('renders', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display phenotype library workspaces', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
    expect(cardNameList).toEqual([PHENOTYPE_LIBRARY_WORKSPACES.name]);
  });

  it('should display tutorial workspaces', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Tutorial Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('should not display unpublished workspaces', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
    expect(cardNameList.length).toBe(0);
  });

  it('should have tutorial workspaces as default tab', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('controlled tier workspace is not clickable for non-ct user', async () => {
    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName =
      AccessTierShortNames.Controlled;
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const cardNameList = wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
    expect(cardNameList.length).toEqual(1);

    const styleCursor = wrapper
      .find('[data-test-id="workspace-card"]')
      .first()
      .find('a')
      .map((c) => c.prop('style').cursor);
    expect(styleCursor).toEqual(['not-allowed']);
  });

  it('controlled tier workspace is clickable for ct user', async () => {
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profileStore.get().profile,
        accessTierShortNames: [
          AccessTierShortNames.Registered,
          AccessTierShortNames.Controlled,
        ],
      },
    });

    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName =
      AccessTierShortNames.Controlled;

    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const styleCursor = wrapper
      .find('[data-test-id="workspace-card"]')
      .first()
      .find('a')
      .map((c) => c.prop('style').color);
    expect(styleCursor).not.toEqual(colors.disabled);
  });
});
