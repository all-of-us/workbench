import * as React from 'react';
import { act } from 'react-dom/test-utils';
import * as fp from 'lodash/fp';
import { ReactWrapper } from 'enzyme';

import { EgressEventsAdminApi, EgressEventStatus } from 'generated/fetch';

import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  mountWithRouter,
  renderWithRouter,
  waitForFakeTimersAndUpdate,
} from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';

import { EgressEventsTable } from './egress-events-table';

describe('EgressEventsTable', () => {
  let eventsStub: EgressEventsAdminApiStub;
  let user: UserEvent;

  beforeEach(() => {
    user = userEvent.setup();
    eventsStub = new EgressEventsAdminApiStub();
    registerApiClient(EgressEventsAdminApi, eventsStub);
  });

  // The getAll query is used here, because this query would fail if you are looking for a single
  // digit number, but there is a multi-digit number that starts with the digit that you are
  // looking for. For example, if you are looking for row 1, and there is a row 10. In this case,
  // it is assumed that the single digit result will always be the first result.
  const getRowWithMatchingEventId = async (
    eventId: number
  ): Promise<HTMLElement> =>
    screen.getAllByRole('row', {
      name: new RegExp(`^${eventId}`),
    })[0];

  const editRowToFalsePositive = async (eventId: number) => {
    // Find the row with the matching event ID and click the edit button.
    const row = await getRowWithMatchingEventId(eventId);
    const editButton = within(row).getByRole('button');
    user.click(editButton);

    // Open the status dropdown
    const statusDropdown = await screen.findByRole('button', {
      name: /select a status/i,
    });
    user.click(statusDropdown);

    // Select the verified false positive option. This should close the dropdown.
    const falsePositiveOption = await screen.findByLabelText(
      /verified_false_positive/i
    );
    user.click(falsePositiveOption);
    await waitFor(() => {
      expect(
        within(row).queryByText(EgressEventStatus.REMEDIATED)
      ).not.toBeInTheDocument();
    });

    // Not ideal, but since PrimeReact does not offer an accessible way to get the save button,
    // we have to rely on the order of the buttons.
    const saveButton = within(row).queryAllByRole('button')[1];
    user.click(saveButton);

    // Once the save button is clicked, the row should no longer have the status dropdown.
    await waitFor(() => {
      expect(
        within(row).queryByRole('button', {
          name: /select a status/i,
        })
      ).not.toBeInTheDocument();
    });
  };

  it('should render basic', async () => {
    renderWithRouter(<EgressEventsTable />);

    await waitFor(() =>
      expect(screen.queryByText('No results found')).toBeInTheDocument()
    );
  });

  it('should render paginated', async () => {
    const headerRows = 1;
    const pageSize = 5;
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), pageSize);

    renderWithRouter(<EgressEventsTable displayPageSize={pageSize} />);
    await screen.findAllByText(EgressEventStatus.REMEDIATED);
    expect(screen.getAllByRole('row').length).toBe(headerRows + pageSize);
  });

  it('should paginate', async () => {
    const totalRecords = 30;
    const pageSize = 10;
    eventsStub.events = fp.times(
      () => eventsStub.simulateNewEvent(),
      totalRecords
    );

    renderWithRouter(<EgressEventsTable displayPageSize={pageSize} />);
    await screen.findAllByText(EgressEventStatus.REMEDIATED);

    const nextButton = screen.getByRole('button', { name: /next page/i });

    const ids = new Set<string>();
    let latestId;
    const numPages = totalRecords / pageSize;
    for (let i = 0; i < numPages; i++) {
      // Wait for the next page to load
      if (i != 0) {
        await waitFor(() => {
          expect(
            screen.queryByRole('cell', {
              name: latestId,
            })
          ).not.toBeInTheDocument();
        });
      }

      const rows = screen.getAllByRole('row');
      rows.shift(); // Remove header row
      expect(rows.length).toBe(pageSize);

      rows
        .map((row, _) => within(row).getAllByRole('cell')[0].textContent)
        .forEach((id) => {
          ids.add(id);
          latestId = id;
        });

      // Navigate to the next page unless test is on the last page
      if (i != numPages - 1) {
        user.click(nextButton);
      }
    }

    expect(nextButton.hasAttribute('disabled')).toBe(true);
  });

  it('should allow event status update', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 5);
    const eventId = 2;
    const eventIdRowIndexDifference = 1;
    renderWithRouter(<EgressEventsTable />);
    await screen.findAllByText(EgressEventStatus.REMEDIATED);

    await editRowToFalsePositive(eventId);
    expect(eventsStub.events[eventId - eventIdRowIndexDifference].status).toBe(
      EgressEventStatus.VERIFIED_FALSE_POSITIVE
    );
  });

  it('should allow multiple event status updates', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 5);
    const firstEventId = 3;
    const secondEventId = 4;
    const eventIdRowIndexDifference = 1;

    renderWithRouter(<EgressEventsTable />);
    await screen.findAllByText(EgressEventStatus.REMEDIATED);

    await editRowToFalsePositive(firstEventId);
    await editRowToFalsePositive(secondEventId);

    expect(
      eventsStub.events[firstEventId - eventIdRowIndexDifference].status
    ).toBe(EgressEventStatus.VERIFIED_FALSE_POSITIVE);
    expect(
      eventsStub.events[secondEventId - eventIdRowIndexDifference].status
    ).toBe(EgressEventStatus.VERIFIED_FALSE_POSITIVE);
  });
});
