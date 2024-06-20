import * as React from 'react';
import { act } from 'react-dom/test-utils';

import {
  AppsApi,
  AppStatus,
  CdrVersionsApi,
  CohortAnnotationDefinitionApi,
  CohortReviewApi,
  DataSetApi,
  DisksApi,
  ErrorCode,
  NotebooksApi,
  ProfileApi,
  RuntimeApi,
  RuntimeStatus,
  TerraJobStatus,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import {
  currentCohortCriteriaStore,
  currentCohortReviewStore,
  currentWorkspaceStore,
  sidebarActiveIconStore,
} from 'app/utils/navigation';
import {
  cdrVersionStore,
  clearCompoundRuntimeOperations,
  profileStore,
  registerCompoundRuntimeOperation,
  runtimeStore,
  serverConfigStore,
  userAppsStore,
} from 'app/utils/stores';
import { SWRConfig } from 'swr';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouter } from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsCromwellResponse,
} from 'testing/stubs/apps-api-stub';
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
import { DisksApiStub } from 'testing/stubs/disks-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { HelpSidebar, LOCAL_STORAGE_KEY_SIDEBAR_STATE } from './help-sidebar';

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
        errorCode: ErrorCode.COMPUTE_SECURITY_SUSPENDED,
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
  let appsStub: AppsApiStub;
  let props: {};
  let user;

  const component = async () => {
    /*
    The useSWR hook in useGenomicExtractionJobs is causing
    extractionJobs to carryover between tests. More details can be
    found here:
    https://github.com/vercel/swr/issues/781#issuecomment-952738214
     */
    const c = renderWithRouter(
      <SWRConfig value={{ provider: () => new Map(), dedupingInterval: 0 }}>
        <HelpSidebar {...props} />
      </SWRConfig>
    );
    return c;
  };

  const findRuntimeStatusIcon = async (status: RuntimeStatus) => {
    let label: RegExp;
    label = switchCase(
      status,
      [RuntimeStatus.RUNNING, () => /icon indicating environment is running/i],
      [
        RuntimeStatus.DELETING,
        () => /icon indicating environment is stopping/i,
      ],
      [
        RuntimeStatus.STOPPING,
        () => /icon indicating environment is stopping/i,
      ],
      [RuntimeStatus.CREATING, () => /icon indicating environment is updating/i]
    );

    return await within(
      await screen.findByTestId('runtime-status-icon-container')
    ).findByRole('img', {
      name: label,
    });
  };

  const findExtractionStatusIcon = async (status: TerraJobStatus) => {
    let label: RegExp;
    switch (status) {
      case TerraJobStatus.ABORTING:
        label = /Icon indicating extraction is stopping/i;
        break;
      case TerraJobStatus.FAILED:
        label = /Icon indicating extraction has failed/i;
        break;
      case TerraJobStatus.SUCCEEDED:
        label = /Icon indicating extraction has succeeded/i;
        break;
    }
    return await within(
      await screen.findByTestId('extraction-status-icon-container')
    ).findByRole('img', {
      name: label,
    });
  };

  const waitForRuntimeStatusIconAbsence = async () => {
    return await waitFor(() =>
      expect(
        within(
          screen.queryByTestId('runtime-status-icon-container')
        ).queryByRole('img')
      ).not.toBeInTheDocument()
    );
  };

  const waitForExtractionStatusIconAbsence = async () => {
    return await waitFor(() =>
      expect(
        within(
          screen.queryByTestId('extraction-status-icon-container')
        ).queryByRole('img')
      ).not.toBeInTheDocument()
    );
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

  const setActiveIcon = async (activeIconKey) => {
    sidebarActiveIconStore.next(activeIconKey);
  };

  const appsPanelTitle = /active applications/i;

  const expectAppsPanel = async () => {
    expect(
      await screen.findByRole('heading', {
        name: appsPanelTitle,
      })
    ).toBeInTheDocument;
  };

  const expectAppsPanelAbsence = () => {
    expect(
      screen.queryByRole('heading', {
        name: appsPanelTitle,
      })
    ).not.toBeInTheDocument;
  };

  const cromwellPanelTitle = /Cromwell Cloud Environment/i;

  const expectCromwellPanel = async () => {
    expect(
      await screen.findByRole('heading', {
        name: cromwellPanelTitle,
      })
    ).toBeInTheDocument;
  };

  const expectCromwellPanelAbsence = () => {
    expect(
      screen.queryByRole('heading', {
        name: cromwellPanelTitle,
      })
    ).not.toBeInTheDocument;
  };

  beforeEach(async () => {
    props = {};
    dataSetStub = new DataSetApiStub();
    runtimeStub = new RuntimeApiStub();
    appsStub = new AppsApiStub();
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(
      CohortAnnotationDefinitionApi,
      new CohortAnnotationDefinitionServiceStub()
    );
    registerApiClient(DataSetApi, dataSetStub);
    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(AppsApi, appsStub);
    registerApiClient(DisksApi, new DisksApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    currentCohortReviewStore.next(cohortReviewStubs[0]);
    serverConfigStore.set({ config: defaultServerConfig });
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    cdrVersionStore.set(cdrVersionTiersResponse);

    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    user = userEvent.setup();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
  });

  it('should render', async () => {
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
  });

  it('should show a different icon and title when pageKey is notebookStorage', async () => {
    props = { pageKey: 'notebookStorage' };
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();

    await setActiveIcon('notebooksHelp');

    expect(
      (
        await screen.findByTestId('help-sidebar-icon-notebooksHelp')
      ).classList.contains('fa-folder-open')
    ).toBeTruthy();
  });

  it('should update marginRight style when sidebarOpen prop changes', async () => {
    await component();
    await setActiveIcon('help');
    expect(
      (await screen.findByTestId('sidebar-content')).parentNode
    ).toHaveStyle({ width: 'calc(21rem + 70px)' });

    await setActiveIcon(null);
    expect(
      (await screen.findByTestId('sidebar-content')).parentNode
    ).toHaveStyle({ width: 0 });
  });

  it('should show delete workspace modal on clicking delete workspace', async () => {
    component();
    await user.click(await screen.findByLabelText('Open Actions Menu'));

    await user.click(
      screen.getByRole('button', {
        name: /delete/i,
      })
    );
    expect(
      screen.getByText(/warning — all work in this workspace will be lost\./i)
    ).toBeInTheDocument();
  });

  it('should show workspace share modal on clicking share workspace', async () => {
    component();
    await user.click(await screen.findByLabelText('Open Actions Menu'));

    await user.click(
      screen.getByRole('button', {
        name: /share/i,
      })
    );
    expect(screen.getByText(/mock workspace share/i)).toBeInTheDocument();
  });

  it('should hide workspace icon if on criteria search page', async () => {
    props = { pageKey: 'cohortBuilder' };
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
    currentCohortCriteriaStore.next([]);

    expect(
      screen.queryByLabelText('Open Actions Menu')
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId('criteria-count')).not.toBeInTheDocument();

    currentCohortCriteriaStore.next([criteria1]);
    expect(await screen.findByTestId('criteria-count')).toBeInTheDocument();
  });

  it('should update count if criteria is added', async () => {
    props = { pageKey: 'cohortBuilder' };
    component();
    currentCohortCriteriaStore.next([criteria1, criteria2]);
    expect(
      await within(await screen.findByTestId('criteria-count')).findByText('2')
    ).toBeInTheDocument();
  });

  it('should not display runtime config icon for read-only workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.READER,
    });
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
    expect(
      screen.queryByTestId('help-sidebar-icon-runtimeConfig')
    ).not.toBeInTheDocument();
  });

  it('should display runtime config icon for writable workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    component();
    expect(
      await screen.findByTestId('help-sidebar-icon-runtimeConfig')
    ).toBeInTheDocument();
  });

  it('should not display apps icon for read-only workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.READER,
    });

    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
    expect(
      screen.queryByTestId('help-sidebar-icon-apps')
    ).not.toBeInTheDocument();
  });

  it('should display apps icon for writable workspaces', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.value,
      accessLevel: WorkspaceAccessLevel.WRITER,
    });

    component();
    expect(
      await screen.findByTestId('help-sidebar-icon-apps')
    ).toBeInTheDocument();
  });

  it('should display dynamic runtime status icon', async () => {
    setRuntimeStatus(RuntimeStatus.RUNNING);
    component();

    expect(
      await findRuntimeStatusIcon(RuntimeStatus.RUNNING)
    ).toBeInTheDocument();
    act(() => setRuntimeStatus(RuntimeStatus.DELETING));
    expect(
      await findRuntimeStatusIcon(RuntimeStatus.DELETING)
    ).toBeInTheDocument();

    act(() => clearRuntime());

    await waitForRuntimeStatusIconAbsence();

    act(() => setRuntimeStatus(RuntimeStatus.CREATING));
    expect(
      await findRuntimeStatusIcon(RuntimeStatus.CREATING)
    ).toBeInTheDocument();
  });

  it('should display "starting" UX during compound runtime op with no runtime', async () => {
    setRuntimeStatus(RuntimeStatus.DELETING);
    registerCompoundRuntimeOperation(workspaceDataStub.namespace, {
      aborter: new AbortController(),
    });
    component();

    expect(
      await findRuntimeStatusIcon(RuntimeStatus.DELETING)
    ).toBeInTheDocument();

    act(() => clearRuntime());

    expect(
      await findRuntimeStatusIcon(RuntimeStatus.CREATING)
    ).toBeInTheDocument();
  });

  it('should display security suspended UX on compute suspended', async () => {
    runtimeStub.getRuntime = COMPUTE_SUSPENDED_RESPONSE_STUB;
    component();

    within(
      await screen.findByTestId('runtime-status-icon-container')
    ).getByTitle('Icon indicating environment is suspended');
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
    component();

    within(
      await screen.findByTestId('runtime-status-icon-container')
    ).getByTitle('Icon indicating environment has encountered an error');
  });
  //
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
    component();

    expect(
      await findRuntimeStatusIcon(RuntimeStatus.RUNNING)
    ).toBeInTheDocument();
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
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
    await findExtractionStatusIcon(TerraJobStatus.ABORTING);
  });

  it('should display "FAILED" icon with recent failed jobs', async () => {
    dataSetStub.extractionJobs = [
      {
        status: TerraJobStatus.FAILED,
        completionTime: Date.now(),
      },
    ];
    component();

    await findExtractionStatusIcon(TerraJobStatus.FAILED);
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
    component();

    await findExtractionStatusIcon(TerraJobStatus.SUCCEEDED);
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
    component();

    await waitForExtractionStatusIconAbsence();
  });

  it('should automatically open previously open panel on load', async () => {
    runtimeStub.getRuntime = () => Promise.resolve(defaultRuntime());
    const activeIcon = 'apps';
    localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
    component();
    await expectAppsPanel();
  });

  it('should not automatically open previously open panel on load if user is suspended', async () => {
    runtimeStub.getRuntime = COMPUTE_SUSPENDED_RESPONSE_STUB;
    const activeIcon = 'apps';
    localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, activeIcon);
    component();
    within(
      await screen.findByTestId('runtime-status-icon-container')
    ).getByTitle('Icon indicating environment is suspended');
    expectAppsPanelAbsence();
  });

  it('should open the Cromwell config panel after clicking the unexpanded app', async () => {
    component();
    await user.click(await screen.findByTestId('help-sidebar-icon-apps'));
    await expectAppsPanel();
    expectCromwellPanelAbsence();
    await user.click(await screen.findByTestId('Cromwell-unexpanded'));
    await expectCromwellPanel();
    expectAppsPanelAbsence();
  });

  it('should open the Cromwell config panel after clicking the Cromwell settings button', async () => {
    userAppsStore.set({
      userApps: [createListAppsCromwellResponse({ status: AppStatus.RUNNING })],
      updating: false,
    });

    component();
    await user.click(await screen.findByTestId('help-sidebar-icon-apps'));

    await expectAppsPanel();
    expectCromwellPanelAbsence();

    await user.click(
      screen.getByRole('button', {
        name: /cromwell/i,
      })
    );

    await expectCromwellPanel();
    expectAppsPanelAbsence();
  });

  it('should open the Cromwell config panel after clicking the Cromwell icon', async () => {
    component();

    await user.click(
      await screen.findByTestId('help-sidebar-icon-cromwellConfig')
    );

    await expectCromwellPanel();
  });

  it('should open the RStudio config panel after clicking the RStudio icon', async () => {
    const rStudioPanelTitle = /RStudio Cloud Environment/i;
    await component();

    expect(screen.queryByText(rStudioPanelTitle)).not.toBeInTheDocument();

    await user.click(
      screen.getByRole('img', {
        name: /rstudio icon/i,
      })
    );

    expect(
      await screen.findByRole('heading', {
        name: rStudioPanelTitle,
      })
    ).toBeInTheDocument;
  });

  it('should open the SAS config panel after clicking the SAS icon', async () => {
    const sasPanelTitle = /SAS Cloud Environment/i;
    await component();

    expect(screen.queryByText(sasPanelTitle)).not.toBeInTheDocument();

    await user.click(
      screen.getByRole('img', {
        name: /sas icon/i,
      })
    );

    expect(
      await screen.findByRole('heading', {
        name: sasPanelTitle,
      })
    ).toBeInTheDocument;
  });
});
