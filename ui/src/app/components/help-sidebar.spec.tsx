import * as React from 'react';
import { act } from 'react-dom/test-utils';

import { CdrVersionsApi, ProfileApi, WorkspacesApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import {
  currentWorkspaceStore,
  sidebarActiveIconStore,
} from 'app/utils/navigation';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';
import { SWRConfig } from 'swr';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouter } from 'testing/react-test-helpers';
import {
  CdrVersionsApiStub,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { HelpSidebar } from './help-sidebar';

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

describe('HelpSidebar', () => {
  let props: {};
  let user;

  const component = () => {
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

  const setActiveIcon = (activeIconKey) => {
    act(() => sidebarActiveIconStore.next(activeIconKey));
  };

  beforeEach(async () => {
    props = {};
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({ config: defaultServerConfig });
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

  it('should render', async () => {
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
  });

  it('should update marginRight style when sidebarOpen prop changes', async () => {
    component();
    expect(await screen.findByTestId('sidebar-content')).toBeInTheDocument();
    setActiveIcon('help');
    expect(
      (await screen.findByTestId('sidebar-content')).parentNode
    ).toHaveStyle({ width: 'calc(21rem + 70px)' });

    setActiveIcon(null);
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
});
