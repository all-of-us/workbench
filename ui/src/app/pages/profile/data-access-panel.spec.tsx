import {DataAccessPanel, DataAccessPanelProps} from 'app/pages/profile/data-access-panel';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {cdrVersionStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {mount} from 'enzyme';
import * as React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';

const findRTGranted = (wrapper) => wrapper.find(`[data-test-id="registered-tier-access-granted"]`);
const findRTDenied = (wrapper) => wrapper.find(`[data-test-id="registered-tier-access-denied"]`);
const findCTGranted = (wrapper) => wrapper.find(`[data-test-id="controlled-tier-access-granted"]`);
const findCTDenied = (wrapper) => wrapper.find(`[data-test-id="controlled-tier-access-denied"]`);

const expectAccessStatus = (wrapper, rtStatus: boolean, ctStatus: boolean) => {
  expect(findRTGranted(wrapper).exists()).toEqual(rtStatus);
  expect(findRTDenied(wrapper).exists()).not.toEqual(rtStatus);
  expect(findCTGranted(wrapper).exists()).toEqual(ctStatus);
  expect(findCTDenied(wrapper).exists()).not.toEqual(ctStatus);
};

const expectAccessStatusRtOnly = (wrapper, rtStatus: boolean) => {
  expect(findRTGranted(wrapper).exists()).toEqual(rtStatus);
  expect(findRTDenied(wrapper).exists()).not.toEqual(rtStatus);

  // Controlled Tier does not render at all
  expect(findCTGranted(wrapper).exists()).toBeFalsy();
  expect(findCTDenied(wrapper).exists()).toBeFalsy();
};

describe('Data Access Panel', () => {
  const component = (props: DataAccessPanelProps) => {
    return mount(<MemoryRouter><DataAccessPanel {...props}/></MemoryRouter>);
  };

  beforeEach(() => {
    cdrVersionStore.set(cdrVersionTiersResponse);
    environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered, AccessTierShortNames.Controlled];
  });

  it('Should show success status for registered tier when the user has access', async () => {

    const wrapper = component({userAccessTiers: ['registered']});
    expectAccessStatus(wrapper, true, false);
  });

  it('Should show success status for controlled tier when the user has access', async () => {
    const wrapper = component({userAccessTiers: ['controlled']});
    expectAccessStatus(wrapper, false, true);
  });

  it('Should show success status when the user is in the registered tier and controlled tier', async () => {
    const wrapper = component({userAccessTiers: ['registered', 'controlled']});
    expectAccessStatus(wrapper, true, true);
  });

  it('Should not show success status when the user is not in the registered tier or controlled tier', async () => {
    const wrapper = component({userAccessTiers: []});
    expectAccessStatus(wrapper, false, false);
  });

  it('Should only show the registered tier in environments without a controlled tier (user has access)', async () => {
    environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered];

    const wrapper = component({userAccessTiers: ['registered']});
    expectAccessStatusRtOnly(wrapper, true);
  });

  it('Should only show the registered tier in environments without a controlled tier (user does not have access)', async () => {
    environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered];

    const wrapper = component({userAccessTiers: []});
    expectAccessStatusRtOnly(wrapper, false);
  });
});
