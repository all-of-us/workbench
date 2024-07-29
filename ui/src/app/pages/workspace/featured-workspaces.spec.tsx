import * as React from 'react';

import { FeaturedWorkspaceApi, ProfileApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { renderWithRouter } from 'testing/react-test-helpers';
import { FeaturedWorkspacesApiStub } from 'testing/stubs/featured-workspaces-service-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

import { FeaturedWorkspaces } from './featured-workspaces';

describe('Featured Workspace List', () => {
  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const component = () => {
    return renderWithRouter(<FeaturedWorkspaces {...props} />);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(FeaturedWorkspaceApi, new FeaturedWorkspacesApiStub());
  });

  it('renders', async () => {
    component();
    expect(
      await screen.findByText('Researcher Workbench Workspace Library')
    ).toBeInTheDocument();
  });
});
