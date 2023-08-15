import * as React from 'react';
import { act } from 'react-dom/test-utils';
import * as fp from 'lodash/fp';
import { ReactWrapper } from 'enzyme';

import { EgressEventsAdminApi, EgressEventStatus } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  mountWithRouter,
  waitForFakeTimersAndUpdate,
} from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';

import { EgressEventsTable } from './egress-events-table';

describe('EgressEventsTable', () => {
  let eventsStub: EgressEventsAdminApiStub;

  beforeEach(() => {
    eventsStub = new EgressEventsAdminApiStub();
    registerApiClient(EgressEventsAdminApi, eventsStub);

    jest.useFakeTimers('modern');
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const editRowToFalsePositive = async (
    wrapper: ReactWrapper,
    rowIndex: number
  ) => {
    wrapper.find('.p-row-editor-init').at(rowIndex).simulate('click');
    wrapper.find('.p-dropdown').simulate('click');
    wrapper
      .find('.p-dropdown-item')
      .find({ 'aria-label': EgressEventStatus.VERIFIEDFALSEPOSITIVE })
      .simulate('click');
    wrapper.find('[type="button"]').find('[name="row-save"]').simulate('click');
    await waitForFakeTimersAndUpdate(wrapper);
  };

  it('should render basic', async () => {
    const wrapper = mountWithRouter(<EgressEventsTable />);
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(EgressEventsTable).exists()).toBeTruthy();
  });

  it('should render paginated', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 20);

    const wrapper = mountWithRouter(<EgressEventsTable displayPageSize={5} />);
    await waitForFakeTimersAndUpdate(wrapper);

    expect(wrapper.find(EgressEventsTable).exists()).toBeTruthy();
    expect(wrapper.find({ 'data-test-id': 'egress-event-id' }).length).toBe(5);
  });

  it('should paginate', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 100);

    const wrapper = mountWithRouter(<EgressEventsTable displayPageSize={10} />);
    await waitForFakeTimersAndUpdate(wrapper);

    const ids = new Set<string>();
    for (let i = 0; i < 10; i++) {
      const idNodes = wrapper.find({ 'data-test-id': 'egress-event-id' });
      expect(idNodes.length).toBe(10);

      idNodes
        .map((w, _) => w.text())
        .forEach((id) => {
          ids.add(id);
        });

      act(() => {
        wrapper.find('.p-paginator-next').simulate('click');
      });
      await waitForFakeTimersAndUpdate(wrapper);
    }
    expect(wrapper.find('.p-paginator-next').prop('disabled')).toBe(true);
    expect(ids.size).toBe(100);
  });

  it('should allow event status update', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 5);

    const wrapper = mountWithRouter(<EgressEventsTable />);
    await waitForFakeTimersAndUpdate(wrapper);

    await editRowToFalsePositive(wrapper, 2);
    expect(eventsStub.events[2].status).toBe(
      EgressEventStatus.VERIFIEDFALSEPOSITIVE
    );
  });

  it('should allow multiple event status updates', async () => {
    eventsStub.events = fp.times(() => eventsStub.simulateNewEvent(), 5);

    const wrapper = mountWithRouter(<EgressEventsTable />);
    await waitForFakeTimersAndUpdate(wrapper);

    await editRowToFalsePositive(wrapper, 2);
    await editRowToFalsePositive(wrapper, 3);

    expect(eventsStub.events[2].status).toBe(
      EgressEventStatus.VERIFIEDFALSEPOSITIVE
    );
    expect(eventsStub.events[3].status).toBe(
      EgressEventStatus.VERIFIEDFALSEPOSITIVE
    );
  });
});
