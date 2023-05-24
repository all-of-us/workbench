import '@testing-library/jest-dom';

import * as fp from 'lodash/fp';

import {
  AppType,
  Disk,
  DiskStatus,
  DiskType,
  ListDisksResponse,
} from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor, within } from '@testing-library/react';
import * as swaggerClients from 'app/services/swagger-fetch-clients';
import moment from 'moment';

import { DisksTable } from './disks-table';

jest.mock('app/services/swagger-fetch-clients');

const convertDate = (originalDate: String) => {
  const date = originalDate.substring(0, 10);
  const hour =
    parseInt(originalDate.substring(11, 13), 10) + moment().utcOffset() / 60;
  const minutes = originalDate.substring(14, 16);
  return `${date} ${hour}:${minutes}`;
};

const getEnvironmentType = (isGceRuntime: boolean, appType: AppType) => {
  return isGceRuntime ? 'Jupyter' : fp.capitalize(appType.toString());
};

test('loads and displays table', async () => {
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
  const mockDisks: ListDisksResponse = [mockDisk];
  // @ts-ignore: Expects full implementation which includes a protected property(configuration) which is hard to mock
  mockdisksAdminApi.mockImplementation(() => ({
    listDisksInWorkspace: () => Promise.resolve(mockDisks),
  }));
  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitFor(() => {
    expect(screen.queryByTestId('disks spinner')).not.toBeInTheDocument();
  });

  mockDisks.forEach((disk) => {
    const row = screen.getByText(disk.name).closest('tr');
    const rowScope = within(row);
    expect(rowScope.getByText(disk.name)).toBeInTheDocument();
    expect(rowScope.getByText(disk.creator)).toBeInTheDocument();
    expect(
      rowScope.getByText(convertDate(disk.createdDate))
    ).toBeInTheDocument();
    expect(rowScope.getByText(disk.status)).toBeInTheDocument();
    expect(
      rowScope.getByText(getEnvironmentType(disk.isGceRuntime, disk.appType))
    ).toBeInTheDocument();
    expect(rowScope.getByText(disk.size)).toBeInTheDocument();
    expect(
      rowScope.getByText('Delete').closest('div[role="button"]')
    ).not.toBeDisabled();
  });
});
