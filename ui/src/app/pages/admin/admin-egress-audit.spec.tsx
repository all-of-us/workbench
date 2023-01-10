import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';
import { mount } from 'enzyme';

import {
  EgressEvent,
  EgressEventsAdminApi,
  EgressEventStatus,
} from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { waitForFakeTimersAndUpdate } from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';

import { AdminEgressAudit } from './admin-egress-audit';

describe('AdminEgressAudit', () => {
  let event: EgressEvent;
  let eventsStub: EgressEventsAdminApiStub;

  const component = () => {
    return mount(
      <MemoryRouter
        initialEntries={[`/admin/egress-events/${event.egressEventId}`]}
      >
        <Route exact path='/admin/egress-events/:eventId'>
          <AdminEgressAudit hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    eventsStub = new EgressEventsAdminApiStub();
    registerApiClient(EgressEventsAdminApi, eventsStub);

    event = eventsStub.simulateNewEvent();

    jest.useFakeTimers('modern');
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render basic', async () => {
    const wrapper = component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(AdminEgressAudit).exists()).toBeTruthy();
  });

  it('should allow event status update', async () => {
    const wrapper = component();
    await waitForFakeTimersAndUpdate(wrapper);

    wrapper.find('#egress-event-status-dropdown').first().simulate('click');

    wrapper
      .find('.p-dropdown-item')
      .find({ 'aria-label': EgressEventStatus.VERIFIEDFALSEPOSITIVE })
      .simulate('click');
    wrapper.find({ 'data-test-id': 'save-egress-event' }).simulate('click');

    await waitForFakeTimersAndUpdate(wrapper);
    expect(eventsStub.events[0].status).toBe(
      EgressEventStatus.VERIFIEDFALSEPOSITIVE
    );
  });
});
