import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {FeaturedWorkspacesConfigApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {FeaturedWorkspacesConfigApiStub} from 'testing/stubs/featured-workspaces-config-api-stub';
import {buildWorkspaceStubs} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceLibrary} from './workspace-library';

describe('WorkspaceLibrary', () => {
  const suffixes = [" Phenotype Library", " Tutorial Workspace", " Published Workspace"];

  const props = {
    enablePublishedWorkspaces: true,
    hideSpinner: () => {},
    showSpinner: () => {},
    spinnerVisible: false
  };

  const component = () => {
    return mount(<WorkspaceLibrary {...props} />);
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(FeaturedWorkspacesConfigApi, new FeaturedWorkspacesConfigApiStub());
  });

  it('renders', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display phenotype library workspaces', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Phenotype Library"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([publishedWorkspaceStubs[0].name]);
  });

  it('should display tutorial workspaces', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Tutorial Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([publishedWorkspaceStubs[1].name]);
  });

  it('should not display unpublished workspaces', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList.length).toBe(0);
  });

  it('should display published workspaces', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true,
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Published Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([publishedWorkspaceStubs[2].name]);
  });

});
