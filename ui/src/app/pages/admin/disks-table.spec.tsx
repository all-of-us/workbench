import '@testing-library/jest-dom';

import * as fp from 'lodash/fp';

import { AppType, ListDisksResponse } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import {
  fireEvent,
  render,
  waitFor,
  waitForElementToBeRemoved,
  within,
} from '@testing-library/react';
import * as swaggerClients from 'app/services/swagger-fetch-clients';
import moment from 'moment';

import {
  mockCromwellDisk,
  mockJupyterDisk,
  mockRStudioDisk,
} from 'testing/stubs/disks-api-stub';

import { DisksTable } from './disks-table';

const convertDate = (originalDate: string) => {
  let date = moment(originalDate).utc();
  date = date.local();

  return date.format('YYYY-MM-DD HH:mm');
};

const getEnvironmentType = (isGceRuntime: boolean, appType: AppType) => {
  return isGceRuntime ? 'Jupyter' : fp.capitalize(appType.toString());
};

let mockDisks: ListDisksResponse;

let mockdisksAdminApi;

beforeEach(() => {
  jest.mock('app/services/swagger-fetch-clients');
  mockdisksAdminApi = jest.spyOn(swaggerClients, 'disksAdminApi');
  mockDisks = [mockJupyterDisk(), mockCromwellDisk(), mockRStudioDisk()];
});

afterEach(() => {
  jest.clearAllMocks();
});

const setup = (mockOverrides) => {
  mockdisksAdminApi.mockImplementation(() => ({
    ...mockOverrides,
  }));
};

test('loads and displays table', async () => {
  setup({ listDisksInWorkspace: () => Promise.resolve(mockDisks) });
  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitForElementToBeRemoved(() =>
    screen.getByTitle('disks loading spinner')
  );

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
    ).not.toHaveStyle(`cursor: not-allowed`);
  });
});

test('loads and displays empty table', async () => {
  setup({ listDisksInWorkspace: () => Promise.resolve([]) });
  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitForElementToBeRemoved(() =>
    screen.getByTitle('disks loading spinner')
  );
  expect(screen.getByText('No disks found')).toBeInTheDocument();
});

test('delete disk', async () => {
  const mockDeleteFunction = jest.fn(() => Promise.resolve({}));
  const mockListFunction = jest.fn(() => Promise.resolve(mockDisks));

  setup({
    deleteDisk: mockDeleteFunction,
    listDisksInWorkspace: mockListFunction,
  });

  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitForElementToBeRemoved(() =>
    screen.getByTitle('disks loading spinner')
  );

  const row = screen.getByText(mockJupyterDisk().name).closest('tr');
  const rowScope = within(row);
  const jupyterDeleteButton = rowScope
    .getByText('Delete')
    .closest('div[role="button"]');

  expect(mockListFunction).toHaveBeenCalledTimes(1);
  fireEvent.click(jupyterDeleteButton);
  await waitFor(() => {
    expect(mockDeleteFunction).toHaveBeenCalledTimes(1);
    // The list function is called once for the initial component load and another as an update after a delete.
    expect(mockListFunction).toHaveBeenCalledTimes(2);
  });
});

test('disable delete buttons while deleting a disk', async () => {
  const mockDeleteFunction = jest.fn(() => new Promise(() => {}));
  const mockListFunction = jest.fn(() => Promise.resolve(mockDisks));

  setup({
    deleteDisk: mockDeleteFunction,
    listDisksInWorkspace: mockListFunction,
  });

  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitForElementToBeRemoved(() =>
    screen.getByTitle('disks loading spinner')
  );

  const jupyterRow = screen.getByText(mockJupyterDisk().name).closest('tr');
  const jupyterRowScope = within(jupyterRow);
  const jupyterDeleteButton = jupyterRowScope
    .getByText('Delete')
    .closest('div[role="button"]');

  fireEvent.click(jupyterDeleteButton);
  await waitFor(() => {
    expect(mockDeleteFunction).toHaveBeenCalledTimes(1);
  });
  mockDisks.forEach((disk) => {
    const row = screen.getByText(disk.name).closest('tr');
    const rowScope = within(row);
    // Deletion not allowed because a disk is being updated, so we do not want to allow for duplicate delete requests
    expect(
      rowScope.getByText('Delete').closest('div[role="button"]')
    ).toHaveStyle(`cursor: not-allowed`);
  });
});
