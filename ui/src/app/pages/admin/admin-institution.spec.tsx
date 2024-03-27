import '@testing-library/jest-dom';

import * as React from 'react';

import { InstitutionApi } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { InstitutionApiStub } from 'testing/stubs/institution-api-stub';

import { AdminInstitution } from './admin-institution';

describe('AdminInstitutionSpec', () => {
  const renderComponent = () => {
    render(<AdminInstitution hideSpinner={() => {}} showSpinner={() => {}} />);
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });

    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('Institution admin table')).toBeInTheDocument();
    });
  });

  it('should display all institutions', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getAllByRole('row')).toHaveLength(5); // 4 rows + 1 header row
    });
  });
});
