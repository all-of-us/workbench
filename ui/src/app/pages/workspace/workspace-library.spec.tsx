import {mount} from 'enzyme';
import * as React from 'react';

import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {FeaturedWorkspacesConfigApi, ProfileApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {FeaturedWorkspacesConfigApiStub} from 'testing/stubs/featured-workspaces-config-api-stub';
import {buildWorkspaceStubs} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceLibrary} from './workspace-library';
import {ProfileApiStub} from "testing/stubs/profile-api-stub";
import {profileStore} from "app/utils/stores";
import {AccessTierShortNames} from "app/utils/access-tiers";

describe('WorkspaceLibrary', () => {
  let publishedWorkspaceStubs = [];
  let PHENOTYPE_LIBRARY_WORKSPACES;
  let TUTORIAL_WORKSPACE;
  let PUBLISHED_WORKSPACE;

  const suffixes = [' Phenotype Library', ' Tutorial Workspace', ' Published Workspace'];

  const props = {
    enablePublishedWorkspaces: true,
    hideSpinner: () => {},
    showSpinner: () => {}
  };

  const component = () => {
    return mount(<WorkspaceLibrary {...props} />);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn()
    });

    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(FeaturedWorkspacesConfigApi, new FeaturedWorkspacesConfigApiStub());
    publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true
    }));
    PHENOTYPE_LIBRARY_WORKSPACES = publishedWorkspaceStubs[0];
    TUTORIAL_WORKSPACE = publishedWorkspaceStubs[1];
    PUBLISHED_WORKSPACE = publishedWorkspaceStubs[2];
  });

  it('renders', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display phenotype library workspaces', async () => {

    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([PHENOTYPE_LIBRARY_WORKSPACES.name]);
  });

  it('should display tutorial workspaces', async () => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Tutorial Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('should not display unpublished workspaces', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList.length).toBe(0);
  });

  it('should display published workspaces', async () => {

    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Published Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([PUBLISHED_WORKSPACE.name]);
  });

  it('should have tutorial workspaces as default tab', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
        .map(c => c.text());
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('controlled tier workspace is not clickable for non-ct user', async () => {
    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName = AccessTierShortNames.Controlled;
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList.length).toEqual(1);

    const styleCursor = wrapper.find('[data-test-id="workspace-card"]').first().find('[role="button"]')
      .map(c => c.prop('style').cursor);
    expect(styleCursor).toEqual(['not-allowed']);
  });

  it('controlled tier workspace is clickable for ct user', async () => {
    profileStore.set({...profileStore.get(), profile: {
        ...profileStore.get().profile,
        accessTierShortNames: [AccessTierShortNames.Registered, AccessTierShortNames.Controlled]
      }});

    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName = AccessTierShortNames.Controlled;

    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    const styleCursor = wrapper.find('[data-test-id="workspace-card"]').first().find('[role="button"]')
      .map(c => c.prop('style').cursor);
    expect(styleCursor).toEqual(['pointer']);
  });
});
