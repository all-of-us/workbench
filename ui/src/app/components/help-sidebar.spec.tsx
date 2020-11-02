import {mount} from 'enzyme';
import * as React from 'react';

import {cohortReviewStore} from 'app/services/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentCohortCriteriaStore, currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {CohortAnnotationDefinitionApi, CohortReviewApi} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortAnnotationDefinitionServiceStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {HelpSidebar} from './help-sidebar';
import {WorkspaceAccessLevel} from "generated/fetch";
import {WorkspacesApi} from "../../generated/fetch";

const sidebarContent = require('assets/json/help-sidebar.json');

const criteria1 = {parameterId: '1', id: 1, parentId: 0,
  type: 'tree', name: 'Criteria 1', group: false, selectable: true, hasAttributes: false};

const criteria2 = {parameterId: '2', id: 2, parentId: 0,
  type: 'tree', name: 'Criteria 2', group: false, selectable: true, hasAttributes: false};

describe('HelpSidebar', () => {
  let props: {};
  const component = () => {
    return mount(<HelpSidebar {...props} />, {attachTo: document.getElementById('root')});
  };

  beforeEach(() => {
    props = {};
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortAnnotationDefinitionApi, new CohortAnnotationDefinitionServiceStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    cohortReviewStore.next(cohortReviewStubs[0]);
    serverConfigStore.next({
      ...defaultServerConfig,
      enableCohortBuilderV2: false,
      enableCustomRuntimes: true
    });
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should update content when helpContentKey prop changes', () => {
    props = {helpContentKey: 'data', sidebarOpen: true};
    const wrapper = component();
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(sidebarContent.data[0].title);
    wrapper.setProps({helpContentKey: 'cohortBuilder'});
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(sidebarContent.cohortBuilder[0].title);
  });

  it('should show a different icon and title when helpContentKey is notebookStorage', () => {
    props = {helpContentKey: 'notebookStorage', sidebarOpen: true};
    const wrapper = component();
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(sidebarContent.notebookStorage[0].title);
    expect(wrapper.find('[data-test-id="help-sidebar-icon-notebooksHelp"]').get(0).props.icon.iconName).toBe('folder-open');
  });

  it('should update marginRight style when sidebarOpen prop changes', () => {
    props = {helpContentKey: 'data', sidebarOpen: true};
    const wrapper = component();
    expect(wrapper.find('[data-test-id="sidebar-content"]').prop('style').marginRight).toBe(0);
    wrapper.setProps({sidebarOpen: false});
    expect(wrapper.find('[data-test-id="sidebar-content"]').prop('style').marginRight).toBe('calc(-14rem - 40px)');
  });

  it('should call delete method when clicked', () => {
    const deleteSpy = jest.fn();
    props = {deleteFunction: deleteSpy};
    const wrapper = component();
    wrapper.find({'data-test-id': 'workspace-menu-button'}).first().simulate('click');
    wrapper.find({'data-test-id': 'Delete-menu-item'}).first().simulate('click');
    expect(deleteSpy).toHaveBeenCalled();
  });

  it('should call share method when clicked', () => {
    const shareSpy = jest.fn();
    props = {shareFunction: shareSpy};
    const wrapper = component();
    wrapper.find({'data-test-id': 'workspace-menu-button'}).first().simulate('click');
    wrapper.find({'data-test-id': 'Share-menu-item'}).first().simulate('click');
    expect(shareSpy).toHaveBeenCalled();
  });

  it('should hide workspace icon if on critera search page', async() => {
    const wrapper = component();
    currentCohortCriteriaStore.next([]);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find({'data-test-id': 'workspace-menu-button'}).length).toBe(0);
    expect(wrapper.find({'data-test-id': 'criteria-count'}).length).toBe(0);
    currentCohortCriteriaStore.next([criteria1]);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find({'data-test-id': 'criteria-count'}).length).toBe(1);
  });

  it('should update count if criteria is added', async() => {
    const wrapper = component();
    currentCohortCriteriaStore.next([criteria1, criteria2]);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find({'data-test-id': 'criteria-count'}).first().props().children).toBe(2);
  });

  it('should not display runtime control icon for read-only workspaces', () => {
    props = {workspace: {accessLevel: WorkspaceAccessLevel.READER}}
    const wrapper = component();
    expect(wrapper.find({'data-test-id': 'help-sidebar-icon-runtime'}).length).toBe(0);
  });

  it('should display runtime control icon for writable workspaces', () => {
    props = {workspace: {accessLevel: WorkspaceAccessLevel.WRITER}}
    const wrapper = component();
    expect(wrapper.find({'data-test-id': 'help-sidebar-icon-runtime'}).length).toBe(1);
  });
});
