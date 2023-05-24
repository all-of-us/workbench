import '@testing-library/jest-dom';

import { Disk, DiskStatus, DiskType } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import * as swaggerClients from 'app/services/swagger-fetch-clients';

import { DisksTable } from './disks-table';

jest.mock('app/services/swagger-fetch-clients');

test('loads and displays greeting', async () => {
  const mockdisksAdminApi = jest.spyOn(swaggerClients, 'disksAdminApi');

  const mockDisk: Disk = {
    size: 1000,
    diskType: DiskType.Standard,
    isGceRuntime: true,
    name: 'mock-disk',
    blockSize: 1,
    status: DiskStatus.Ready,
    appType: null,
    creator: '"evrii@fake-research-aou.org"',
    createdDate: '2023-05-22T18:55:10.108838Z',
  };
  // @ts-ignore: Expects full implementation which includes a protected property(configuration) which is hard to mock
  mockdisksAdminApi.mockImplementation(() => ({
    listDisksInWorkspace: () => Promise.resolve([mockDisk]),
  }));
  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitFor(() => {
    expect(screen.queryByTestId('disks spinner')).not.toBeInTheDocument();
  });
  // expect(DataTable).toBeInTheDocument();
  // screen.getByText(/disks/i);
});
