import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  AppsApi,
  CdrVersionsApi,
  CohortAnnotationDefinitionApi,
  CohortReviewApi,
  DataSetApi,
  ErrorCode,
  NotebooksApi,
  RuntimeApi,
  RuntimeStatus,
  TerraJobStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { AppsPanel } from 'app/components/apps-panel';
import { ConfirmWorkspaceDeleteModal } from 'app/components/confirm-workspace-delete-modal';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  currentCohortCriteriaStore,
  currentCohortReviewStore,
  currentWorkspaceStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import {
  cdrVersionStore,
  clearCompoundRuntimeOperations,
  registerCompoundRuntimeOperation,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';
import { SWRConfig } from 'swr';

import defaultServerConfig from 'testing/default-server-config';
import {
  mountWithRouter,
  waitForFakeTimersAndUpdate,
} from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import {
  CdrVersionsApiStub,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { CohortAnnotationDefinitionServiceStub } from 'testing/stubs/cohort-annotation-definition-service-stub';
import {
  CohortReviewServiceStub,
  cohortReviewStubs,
} from 'testing/stubs/cohort-review-service-stub';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { HelpSidebar, LOCAL_STORAGE_KEY_SIDEBAR_STATE } from './help-sidebar';

const sidebarContent = require('assets/json/help-sidebar.json');

const criteria1 = {
  parameterId: '1',
  id: 1,
  parentId: 0,
  type: 'tree',
  name: 'Criteria 1',
  group: false,
  selectable: true,
  hasAttributes: false,
};

const criteria2 = {
  parameterId: '2',
  id: 2,
  parentId: 0,
  type: 'tree',
  name: 'Criteria 2',
  group: false,
  selectable: true,
  hasAttributes: false,
};

jest.mock('react-transition-group', () => {
  return {
    CSSTransition: (props) => props.children,
    TransitionGroup: (props) => props.children,
  };
});

class MockWorkspaceShare extends React.Component {
  render() {
    return <div>Mock Workspace Share</div>;
  }
}

jest.mock('app/pages/workspace/workspace-share', () => {
  return {
    WorkspaceShare: () => <MockWorkspaceShare />,
  };
});

const COMPUTE_SUSPENDED_RESPONSE_STUB = () =>
  Promise.reject(
    new Response(
      JSON.stringify({
        errorCode: ErrorCode.COMPUTESECURITYSUSPENDED,
        parameters: {
          suspendedUntil: new Date('2000-01-01 03:00:00').toISOString(),
        },
      }),
      {
        status: 412,
      }
    )
  );

describe('HelpSidebar', () => {
  let dataSetStub: DataSetApiStub;
  let runtimeStub: RuntimeApiStub;
  let props: {};

  const component = async () => {
    const c = mountWithRouter(
      <SWRConfig value={{ provider: () => new Map() }}>
        <HelpSidebar {...props} />
      </SWRConfig>,
      {
        attachTo: document.getElementById('root'),
      }
    );
    await waitForFakeTimersAndUpdate(c);
    return c;
  };

  const runtimeStatusIcon = (wrapper, exists = true) => {
    const icon = wrapper
      .find({ 'data-test-id': 'runtime-status-icon-container' })
      .find('svg');
    expect(icon.exists()).toEqual(exists);
    return icon;
  };

  const extractionStatusIcon = (wrapper, exists = true) => {
    const icon = wrapper
      .find({ 'data-test-id': 'extraction-status-icon-container' })
      .find('svg');
    expect(icon.exists()).toEqual(exists);
    return icon;
  };

  const setRuntimeStatus = (status) => {
    const runtime = { ...defaultRuntime(), status };
    runtimeStub.runtime = runtime;
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime,
      runtimeLoaded: true,
    });
  };

  const clearRuntime = () => {
    runtimeStub.runtime = null;
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: null,
      runtimeLoaded: false,
    });
  };

  const setActiveIcon = async (wrapper, activeIconKey) => {
    setSidebarActiveIconStore.next(activeIconKey);
    await waitForFakeTimersAndUpdate(wrapper);
  };

  beforeEach(() => {
    props = {};
    dataSetStub = new DataSetApiStub();
    runtimeStub = new RuntimeApiStub();
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(
      CohortAnnotationDefinitionApi,
      new CohortAnnotationDefinitionServiceStub()
    );
    registerApiClient(DataSetApi, dataSetStub);
    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(AppsApi, new AppsApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    currentCohortReviewStore.next(cohortReviewStubs[0]);
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableGenomicExtraction: true },
    });
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    cdrVersionStore.set(cdrVersionTiersResponse);

    // mock timers
    jest.useFakeTimers('modern');
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.useRealTimers();
  });

  it('should render', async () => {
    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show a different icon and title when pageKey is notebookStorage', async () => {
    props = { pageKey: 'notebookStorage' };
    const wrapper = await component();
    await setActiveIcon(wrapper, 'notebooksHelp');
    expect(wrapper.find('[data-test-id="section-title-0"]').text()).toBe(
      sidebarContent.notebookStorage[0].title
    );
    expect(
      wrapper.find('[data-test-id="help-sidebar-icon-notebooksHelp"]').get(0)
        .props.icon.iconName
    ).toBe('folder-open');
  });

  it('should update marginRight style when sidebarOpen prop changes', async () => {
    const wrapper = await component();
    await setActiveIcon(wrapper, 'help');
    expect(
      wrapper.find('[data-test-id="sidebar-content"]').parent().prop('style')
        .width
    ).toBe('calc(21rem + 70px)');

    await setActiveIcon(wrapper, null);
    expect(
      wrapper.find('[data-test-id="sidebar-content"]').parent().prop('style')
        .width
    ).toBe(0);
  });

  it('should show delete workspace modal on clicking delete workspace', async () => {
    const wrapper = await component();
    wrapper
      .find({ 'data-test-id': 'workspace-menu-button' })
      .first()
      .simulate('click');
    wrapper
      .find({ 'data-test-id': 'Delete-menu-item' })
      .first()
      .simulate('click');
    await waitForFakeTimersAndUpdate(wrapper);
    expect(wrapper.find(ConfirmWorkspaceDeleteModal).exists()).toBeTruthy();
  });

  it('should show workspace share modal on clicking share workspace', async () => {
    const wrapper = await component();
    wrapper
      .find({ 'data-test-id': 'workspace-menu-button' })
      .first()
      .simulate('click');
    wrapper
      .find({ 'data-test-id': 'Share-menu-item' })
      .first()
      .simulate('click');
    await waitForFakeTimersAndUpdate(wrapper);
    expect(wrapper.find(MockWorkspaceShare).exists()).toBeTruthy();
  });

  it('should hide workspace icon if on criteria search page', async () => {
    props = { pageKey: 'cohortBuilder' };
    const wrapper = await component();
    currentCohortCriteriaStore.next([]);
    await waitForFakeTimersAndUpdate(wrapper);

    expect(
      wrapper.find({ 'data-test-id': 'workspace-menu-button' }).length
    ).toBe(0);
    expect(wrapper.find({ 'data-test-id': 'criteria-count' }).length).toBe(0);
    currentCohortCriteriaStore.next([criteria1]);
    await waitForFakeTimersAndUpdate(wrapper);
    expect(wrapper.find({ 'data-test-id': 'criteria-count' }).length).toBe(1);
  });

  it('should update count if criteria is added', async () => {
    props = { pageKey: 'cohortBuilder' };
    const wrapper = await component();
    currentCohortCriteriaStore.next([criteria1, criteria2]);
    await waitForFakeTimersAndUpdate(wrapper);
    expect(
      wrapper.find({ 'data-test-id': 'criteria-count' }).first().props()
        .children
    ).toBe(2);
  });

  it('should not display runtime config icon for read-only workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.READER,
    });
    const wrapper = await component();
    expect(
      wrapper.find({ 'data-test-id': 'help-sidebar-icon-runtimeConfig' }).length
    ).toBe(0);
  });

  it('should display runtime config icon for writable workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    const wrapper = await component();
    expect(
      wrapper.find({ 'data-test-id': 'help-sidebar-icon-runtimeConfig' }).length
    ).toBe(1);
  });

  it('should not display apps icon for read-only workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.READER,
    });
    const wrapper = await component();
    expect(
      wrapper.find({ 'data-test-id': 'help-sidebar-icon-apps' }).length
    ).toBe(0);
  });

  it('should display apps icon for writable workspaces when showAppsPanel is true', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableCromwellGKEApp: true },
    });
    const wrapper = await component();
    expect(
      wrapper.find({ 'data-test-id': 'help-sidebar-icon-apps' }).length
    ).toBe(1);
  });

  it('should not display apps icon for writable workspaces when no apps are enabled', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableCromwellGKEApp: false,
        enableRStudioGKEApp: false,
      },
    });
    const wrapper = await component();
    expect(
      wrapper.find({ 'data-test-id': 'help-sidebar-icon-apps' }).length
    ).toBe(0);
  });

  it('should display dynamic runtime status icon', async () => {
    setRuntimeStatus(RuntimeStatus.Running);
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.running
    );

    act(() => setRuntimeStatus(RuntimeStatus.Deleting));
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.stopping
    );

    act(() => clearRuntime());
    await waitForFakeTimersAndUpdate(wrapper);
    runtimeStatusIcon(wrapper, /* exists */ false);

    act(() => setRuntimeStatus(RuntimeStatus.Creating));
    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.starting
    );
  });

  it('should display "starting" UX during compound runtime op with no runtime', async () => {
    setRuntimeStatus(RuntimeStatus.Deleting);
    registerCompoundRuntimeOperation(workspaceDataStub.namespace, {
      aborter: new AbortController(),
    });
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.stopping
    );

    act(() => clearRuntime());
    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.starting
    );
  });

  it('should display security suspended UX on compute suspended', async () => {
    runtimeStub.getRuntime = COMPUTE_SUSPENDED_RESPONSE_STUB;
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.stopped
    );
  });

  it('should display error on unknown error', async () => {
    runtimeStub.runtime = null;
    runtimeStub.getRuntime = () =>
      Promise.reject(
        new Response('', {
          status: 500,
        })
      );
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: undefined,
      runtimeLoaded: false,
      loadingError: new Error('???'),
    });
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.error
    );
  });

  it('should display "running" icon when extract currently running', async () => {
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.RUNNING,
      },
      {
        status: TerraJobStatus.ABORTING,
      },
      {
        status: TerraJobStatus.FAILED,
        completionTime: Date.now(),
      },
      {
        status: TerraJobStatus.SUCCEEDED,
        completionTime: Date.now(),
      },
    ];
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(extractionStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.starting
    );
  });

  it('should display "aborting" icon when extract currently aborting and nothing running', async () => {
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.ABORTING,
      },
      {
        status: TerraJobStatus.FAILED,
        completionTime: Date.now(),
      },
      {
        status: TerraJobStatus.SUCCEEDED,
        completionTime: Date.now(),
      },
    ];
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(extractionStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.stopping
    );
  });

  it('should display "FAILED" icon with recent failed jobs', async () => {
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.FAILED,
        completionTime: Date.now(),
      },
    ];
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(extractionStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.error
    );
  });

  it('should display icon corresponding to most recent completed job within 24h', async () => {
    const oneHourAgo = new Date();
    oneHourAgo.setHours(oneHourAgo.getHours() - 1);
    const twoHoursAgo = new Date();
    twoHoursAgo.setHours(twoHoursAgo.getHours() - 2);
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.SUCCEEDED,
        completionTime: oneHourAgo.getTime(),
      },
      {
        status: TerraJobStatus.FAILED,
        completionTime: twoHoursAgo.getTime(),
      },
    ];
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(extractionStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.succeeded
    );
  });

  it('should display no extract icons with old failed/succeeded jobs', async () => {
    const date = new Date();
    date.setMonth(date.getMonth() - 1);
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.FAILED,
        completionTime: date.getTime(),
      },
      {
        status: TerraJobStatus.SUCCEEDED,
        completionTime: date.getTime(),
      },
    ];
    const wrapper = await component();
    await waitForFakeTimersAndUpdate(wrapper);

    extractionStatusIcon(wrapper, false);
  });

  it('should automatically open previously open panel on load', async () => {
    runtimeStub.getRuntime = () => Promise.resolve(defaultRuntime());
    const activeIcon = 'apps';
    localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
    const wrapper = await component();
    expect(
      // @ts-ignore
      wrapper.find('[data-test-id="sidebar-content"]').contains(AppsPanel)
    ).toBeTruthy();
  });

  it('should not automatically open previously open panel on load if user is suspended', async () => {
    runtimeStub.getRuntime = COMPUTE_SUSPENDED_RESPONSE_STUB;
    const activeIcon = 'apps';
    localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
    const wrapper = await component();
    expect(
      // @ts-ignore
      wrapper.find('[data-test-id="sidebar-content"]').contains(AppsPanel)
    ).toBeFalsy();
  });
});
