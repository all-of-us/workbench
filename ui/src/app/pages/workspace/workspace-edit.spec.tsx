import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {cdrVersionStore, currentWorkspaceStore, navigate, routeConfigDataStore, serverConfigStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {WorkspaceAccessLevel, UserApi, WorkspacesApi} from 'generated/fetch';
import * as React from 'react';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {WorkspaceEdit, WorkspaceEditMode, WorkspaceEditSection} from './workspace-edit';
import {workspaceStubs, WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';
import {UserApiStub} from 'testing/stubs/user-api-stub';

jest.mock('app/utils/navigation', () => ({
  ...(require.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

jest.mock('app/utils/workbench-gapi-client', () => ({
  getBillingAccountInfo: () => new Promise(resolve => resolve({billingAccountName: 'billing-account'}))
}));

describe('WorkspaceEdit', () => {
  let workspacesApi: WorkspacesApiStub;
  let userApi: UserApiStub;

  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };

  const component = () => {
    return mount(<WorkspaceEdit cancel={() => {}} />);
  };

  beforeEach(() => {
    workspace.researchPurpose = {
      ...workspace.researchPurpose,
      intendedStudy: 'greyscale',
      anticipatedFindings: 'everything',
      reasonForAllOfUs: 'science',
      drugDevelopment: true
    };

    userApi = new UserApiStub();
    registerApiClient(UserApi, userApi);

    workspacesApi = new WorkspacesApiStub([workspace]);
    registerApiClient(WorkspacesApi, workspacesApi);

    currentWorkspaceStore.next(workspace);
    cdrVersionStore.next(cdrVersionListResponse);
    routeConfigDataStore.next({mode: WorkspaceEditMode.Create});
    serverConfigStore.next({enableBillingLockout: true, defaultFreeCreditsDollarLimit: 100.0, gsuiteDomain: ''});
  });

  it('displays workspaces create page', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(WorkspaceEditSection).first().text()).toContain("Create a new Workspace");
  });

  it('displays workspaces duplicate page', async () => {
    routeConfigDataStore.next({mode: WorkspaceEditMode.Duplicate});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(WorkspaceEditSection).first().text()).toContain("Duplicate workspace");
  });

  it('displays workspaces edit page', async () => {
    routeConfigDataStore.next({mode: WorkspaceEditMode.Edit});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find(WorkspaceEditSection).first().text()).toContain("Edit workspace");
  });

  it('supports successful duplication', async () => {
    routeConfigDataStore.next({mode: WorkspaceEditMode.Duplicate});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const numBefore = workspacesApi.workspaces.length;
    wrapper.find('[data-test-id="workspace-save-btn"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(workspacesApi.workspaces.length).toEqual(numBefore + 1);
    expect(navigate).toHaveBeenCalledTimes(1);
  });

  it('supports waiting on access delays', async () => {
    routeConfigDataStore.next({mode: WorkspaceEditMode.Duplicate});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    workspacesApi.getWorkspace = (..._) => {
      return Promise.resolve({workspace, accessLevel: WorkspaceAccessLevel.NOACCESS});
    };

    jest.useFakeTimers();
    wrapper.find('[data-test-id="workspace-save-btn"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(navigate).not.toHaveBeenCalled();

    jest.advanceTimersByTime(15e3);
    await waitOneTickAndUpdate(wrapper);
    expect(navigate).not.toHaveBeenCalled();

    workspacesApi.getWorkspace = (..._) => {
      return Promise.resolve({workspace, accessLevel: WorkspaceAccessLevel.OWNER});
    };
    jest.advanceTimersByTime(10e3);
    await waitOneTickAndUpdate(wrapper);
    expect(navigate).toHaveBeenCalled();

    jest.useRealTimers();
  });

  it('shows confirmation on extended access delays', async () => {
    routeConfigDataStore.next({mode: WorkspaceEditMode.Duplicate});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    workspacesApi.getWorkspace = (..._) => {
      return Promise.resolve({workspace, accessLevel: WorkspaceAccessLevel.NOACCESS});
    };

    jest.useFakeTimers();
    wrapper.find('[data-test-id="workspace-save-btn"]').first().simulate('click');
    let aclDelayBtn;
    for (let i = 0; i < 10; i++) {
      jest.advanceTimersByTime(20e3);
      await waitOneTickAndUpdate(wrapper);
      aclDelayBtn = wrapper.find('[data-test-id="workspace-acl-delay-btn"]').first();
      if (aclDelayBtn.exists()) {
        break;
      }
    }

    if (!aclDelayBtn.exists()) {
      fail('failed to find a rendered acl delay modal button after many timer increments');
    }
    expect(navigate).not.toHaveBeenCalled();

    aclDelayBtn.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(navigate).toHaveBeenCalled();

    jest.useRealTimers();
  });
});
