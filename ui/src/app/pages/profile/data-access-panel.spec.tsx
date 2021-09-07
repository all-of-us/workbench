import {mount} from 'enzyme';
import * as React from 'react';

import * as TestHelpers from 'testing/react-test-helpers';
import {DataAccessPanel, DataAccessPanelProps} from 'app/pages/profile/data-access-panel';
import {CheckCircle} from 'app/components/icons'
import {cdrVersionStore} from 'app/utils/stores';
import {cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';
import {CdrVersionTier} from 'generated/fetch';
import {AccessTierShortNames} from 'app/utils/access-tiers';


describe('Data Access Panel', () => {
  const component = (props: DataAccessPanelProps) => {
    return mount(<DataAccessPanel {...props}/>);
  };

  beforeEach(() => {
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('Should show success status for registered tier when the user has access', async() => {
    const wrapper = component({accessTierShortNames: ['registered']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
    // one section of "Please complete the data access requirements to gain access", for CT
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(1);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access to controlled tier data').length).toBe(1);
  });

  it('Should show success status for controlled tier when the user has access', async() => {
    const wrapper = component({accessTierShortNames: ['controlled']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
    // one section of "Please complete the data access requirements to gain access", for RT
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(1);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access to registered tier data').length).toBe(1);
  });

  it('Should show success status when the user is in the registered tier and controlled tier', async() => {
    const wrapper = component({accessTierShortNames: ['registered', 'controlled']});
    expect(wrapper.find(CheckCircle).length).toBe(2);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(0);
  });

  it('Should not show success status when the user is in not the registered tier or controlled tier', async() => {
    const wrapper = component({accessTierShortNames: []});
    expect(wrapper.find(CheckCircle).length).toBe(0);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(2);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access to registered tier data').length).toBe(1);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access to controlled tier data').length).toBe(1);
  });

  it('Should only show the registered tier in environments without a controlled tier (user has access)', async() => {
    const rtOnly: CdrVersionTier = cdrVersionTiersResponse.tiers.find(tier => tier.accessTierShortName === AccessTierShortNames.Registered);
    // sanity check
    expect(rtOnly).toBeTruthy();
    
    cdrVersionStore.set({
      ...cdrVersionTiersResponse,
      tiers: [rtOnly]
    });

    const wrapper = component({accessTierShortNames: ['registered']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(0);
  });
  
  it('Should only show the registered tier in environments without a controlled tier (user does not have access)', async() => {
    const rtOnly: CdrVersionTier = cdrVersionTiersResponse.tiers.find(tier => tier.accessTierShortName === AccessTierShortNames.Registered);
    // sanity check
    expect(rtOnly).toBeTruthy();

    cdrVersionStore.set({
      ...cdrVersionTiersResponse,
      tiers: [rtOnly]
    });

    const wrapper = component({accessTierShortNames: []});
    expect(wrapper.find(CheckCircle).length).toBe(0);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access').length).toBe(1);
    expect(TestHelpers.findNodesContainingText(wrapper, 'gain access to registered tier data').length).toBe(1);
  });
});
