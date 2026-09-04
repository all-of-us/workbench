import '@testing-library/jest-dom';

import * as React from 'react';

import {
  StatusAlertApi,
  StatusAlertLocation,
  VwbSystemNotificationAdminApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  registerApiClient,
  statusAlertApi,
  vwbSystemNotificationAdminApi,
} from 'app/services/swagger-fetch-clients';

import { StatusAlertApiStub } from 'testing/stubs/status-alert-api-stub';
import {
  stubVwbSystemNotification,
  VwbSystemNotificationAdminApiStub,
} from 'testing/stubs/vwb-system-notification-admin-api-stub';

import { AdminBannerTable } from './admin-banner-table';

const findRowByTitle = async (title: string) => {
  const cell = await screen.findByText(title);
  return cell.closest('tr');
};

// Column order: Title, Message, Link, Verily Workbench, Location, Start, End, Actions.
const VWB_COLUMN = 3;
const LOCATION_COLUMN = 4;

const cellText = (row: HTMLElement, columnIndex: number) =>
  row.querySelectorAll('td')[columnIndex].textContent;

// IconButton renders a div rather than a button, so there is no button role to query. The delete
// control is the icon in the last column.
const clickDelete = async (
  row: HTMLElement,
  user: ReturnType<typeof userEvent.setup>
) => {
  const cells = row.querySelectorAll('td');
  const actionCell = cells[cells.length - 1];
  await user.click(actionCell.querySelector('svg') ?? actionCell);
};

describe(AdminBannerTable.name, () => {
  const component = () =>
    render(<AdminBannerTable hideSpinner={() => {}} showSpinner={() => {}} />);

  beforeEach(() => {
    registerApiClient(
      StatusAlertApi,
      new StatusAlertApiStub(StatusAlertLocation.AFTER_LOGIN)
    );
    registerApiClient(
      VwbSystemNotificationAdminApi,
      new VwbSystemNotificationAdminApiStub([
        stubVwbSystemNotification({ title: 'VWB Notification' }),
      ])
    );
  });

  it('should list All of Us banners and Verily Workbench notifications together', async () => {
    component();

    const aouRow = await findRowByTitle('Stub Title');
    const vwbRow = await findRowByTitle('VWB Notification');

    // The Verily Workbench column distinguishes the two kinds of row.
    expect(cellText(aouRow, VWB_COLUMN)).toEqual('No');
    expect(cellText(vwbRow, VWB_COLUMN)).toEqual('Yes');
  });

  it('should show no location for a Verily Workbench notification', async () => {
    component();

    // Location is an All of Us banner concept only.
    const vwbRow = await findRowByTitle('VWB Notification');
    expect(cellText(vwbRow, LOCATION_COLUMN)).toEqual('-');

    const aouRow = await findRowByTitle('Stub Title');
    expect(cellText(aouRow, LOCATION_COLUMN)).toEqual('After Login');
  });

  it('should delete a Verily Workbench notification through the VWB API', async () => {
    const user = userEvent.setup();
    const vwbDeleteSpy = jest.spyOn(
      vwbSystemNotificationAdminApi(),
      'deleteVwbSystemNotification'
    );
    const aouDeleteSpy = jest.spyOn(statusAlertApi(), 'deleteStatusAlert');

    component();

    const vwbRow = await findRowByTitle('VWB Notification');
    await clickDelete(vwbRow, user);

    expect(vwbDeleteSpy).toHaveBeenCalledWith('vwb-notification-uuid-1');
    expect(aouDeleteSpy).not.toHaveBeenCalled();

    await waitFor(() =>
      expect(screen.queryByText('VWB Notification')).not.toBeInTheDocument()
    );
  });

  it('should delete an All of Us banner through the status alert API', async () => {
    const user = userEvent.setup();
    const vwbDeleteSpy = jest.spyOn(
      vwbSystemNotificationAdminApi(),
      'deleteVwbSystemNotification'
    );
    const aouDeleteSpy = jest.spyOn(statusAlertApi(), 'deleteStatusAlert');

    component();

    const aouRow = await findRowByTitle('Stub Title');
    await clickDelete(aouRow, user);

    expect(aouDeleteSpy).toHaveBeenCalledWith(1);
    expect(vwbDeleteSpy).not.toHaveBeenCalled();
  });

  it('should still show All of Us banners when Verily Workbench is unreachable', async () => {
    jest
      .spyOn(vwbSystemNotificationAdminApi(), 'listVwbSystemNotifications')
      .mockRejectedValue(new Error('user-manager unavailable'));

    component();

    // Listing Verily Workbench notifications depends on user-manager, but All of Us banners do
    // not, so an outage there must not blank the page.
    expect(await screen.findByText('Stub Title')).toBeInTheDocument();
    expect(
      await screen.findByText(/Could not load Verily Workbench notifications/i)
    ).toBeInTheDocument();
  });

  it('should offer a button to create a Verily Workbench banner', async () => {
    const user = userEvent.setup();
    component();

    await user.click(
      await screen.findByRole('button', {
        name: 'Create Verily Workbench Banner',
      })
    );

    expect(
      screen.getByText(/Audience: All of Us customers only/i)
    ).toBeInTheDocument();
  });
});
