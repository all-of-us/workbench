import * as React from 'react';

import {
  FeaturedWorkspaceApi,
  FeaturedWorkspaceCategory,
  ProfileApi,
} from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { profileStore } from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { FeaturedWorkspacesApiStub } from 'testing/stubs/featured-workspaces-service-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

import { FeaturedWorkspaces } from './featured-workspaces';

describe('Featured Workspace List', () => {
  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const featuredCategoryTabs = [
    'Tutorial Workspaces',
    'Demonstration Projects',
    'Phenotype Library',
    'Community Workspaces',
  ];
  const featuredCategory = [
    FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES,
    FeaturedWorkspaceCategory.DEMO_PROJECTS,
    FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY,
    FeaturedWorkspaceCategory.COMMUNITY,
  ];
  let user;
  const component = () => {
    return renderWithRouter(<FeaturedWorkspaces {...props} />);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(FeaturedWorkspaceApi, new FeaturedWorkspacesApiStub());
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
    user = userEvent.setup();
  });

  it('renders', async () => {
    component();
    expect(
      await screen.findByText('Researcher Workbench Workspace Library')
    ).toBeInTheDocument();
    featuredCategoryTabs.map((tabName) => {
      expect(screen.getByRole('button', { name: tabName })).toBeInTheDocument();
    });
  });

  it('Shows all workspaces on clicking Tab name', async () => {
    component();
    expect(
      await screen.findByText('Researcher Workbench Workspace Library')
    ).toBeInTheDocument();
    props.hideSpinner();

    // Click on each tab and verify the workspaces
    featuredCategoryTabs.map(async (tabName, index) => {
      // Click Tab
      await user.click(
        await screen.getByRole('button', { name: featuredCategoryTabs[index] })
      );

      // There are two workspaces for each tab in the stub
      await waitFor(() => {
        expect(
          screen.getByRole('button', { name: featuredCategoryTabs[index] })
        ).toBeInTheDocument();
        expect(screen.queryAllByTestId('workspace-card').length).toBe(2);
      });
      expect(
        screen.queryByText(
          WorkspaceStubVariables.DEFAULT_WORKSPACE_DISPLAY_NAME +
            featuredCategory[index]
        )
      ).toBeInTheDocument();
    });
  });
});
