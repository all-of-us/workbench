import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { CompatRoute, CompatRouter } from 'react-router-dom-v5-compat';

import {
  EgressEvent,
  EgressEventsAdminApi,
  EgressEventStatus,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { expectButtonElementEnabled } from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';

import { AdminEgressAudit } from './admin-egress-audit';

describe('AdminEgressAudit', () => {
  let event: EgressEvent;
  let eventsStub: EgressEventsAdminApiStub;

  const component = () => {
    return render(
      <MemoryRouter
        initialEntries={[`/admin/egress-events/${event.egressEventId}`]}
      >
        <CompatRouter>
          <CompatRoute exact path='/admin/egress-events/:eventId'>
            <AdminEgressAudit hideSpinner={() => {}} showSpinner={() => {}} />
          </CompatRoute>
        </CompatRouter>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    eventsStub = new EgressEventsAdminApiStub();
    registerApiClient(EgressEventsAdminApi, eventsStub);

    event = eventsStub.simulateNewEvent();

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render basic', async () => {
    const { getByText } = component();
    await waitFor(() => getByText(`Egress event ${event.egressEventId}`));
  });

  it('should allow event status update', async () => {
    const { getByText } = component();

    await waitFor(() => getByText(`Egress event ${event.egressEventId}`));
    screen.logTestingPlaygroundURL();
    userEvent.click(screen.getByRole('button', { name: /select a status/i }));
    let falsePositiveOption;
    await waitFor(() => {
      falsePositiveOption = getByText(
        EgressEventStatus.VERIFIED_FALSE_POSITIVE
      );
    });

    userEvent.click(falsePositiveOption);

    const saveButton = screen.getByRole('button', { name: /save/i });

    await waitFor(() => {
      expectButtonElementEnabled(saveButton);
    });

    userEvent.click(saveButton);

    await waitFor(() => {
      expect(eventsStub.events[0].status).toBe(
        EgressEventStatus.VERIFIED_FALSE_POSITIVE
      );
    });
  });
});
