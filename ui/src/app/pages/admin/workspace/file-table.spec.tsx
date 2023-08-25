import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as swaggerClients from 'app/services/swagger-fetch-clients';

import { FileTable } from './file-table';

let workspaceNamespace, storageBucketPath, mockWorkspaceAdminApi, listFiles;

beforeEach(() => {
  workspaceNamespace = 'some fake value';
  storageBucketPath = 'another fake value';

  listFiles = jest.fn(() => Promise.resolve([]));
  mockWorkspaceAdminApi = jest.spyOn(swaggerClients, 'workspaceAdminApi');
  mockWorkspaceAdminApi.mockImplementation(() => ({ listFiles }));
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('FileTable', () => {
  it('calls the admin listAllFiles endpoint with appFilesOnly set appropriately', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <FileTable {...{ workspaceNamespace, storageBucketPath }} />
    );
    expect(container).toBeInTheDocument();

    let appFilesOnly = true; // initial load behavior
    expect(listFiles).toHaveBeenCalledWith(workspaceNamespace, appFilesOnly);

    const filesOnlySwitch = screen.getByRole('switch');
    await waitFor(async () => {
      await user.click(filesOnlySwitch);
      appFilesOnly = !appFilesOnly;
      expect(listFiles).toHaveBeenCalledWith(workspaceNamespace, appFilesOnly);
    });
    await waitFor(async () => {
      await user.click(filesOnlySwitch);
      appFilesOnly = !appFilesOnly;
      expect(listFiles).toHaveBeenCalledWith(workspaceNamespace, appFilesOnly);
    });
  });
});
