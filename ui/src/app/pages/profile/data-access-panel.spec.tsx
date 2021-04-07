import {mount} from 'enzyme';
import * as React from 'react';
import * as TestUtils from 'app/utils/test-utils'
import {DataAccessPanel} from 'app/pages/profile/data-access-panel';
import {CheckCircle} from 'app/components/icons'


describe('Data Access Panel', () => {
  const component = (props = {}) => {
    return mount(<DataAccessPanel {...props}/>);
  };

  it('Should show controlled tier message while the user institution does not have an agreement', async() => {
    const wrapper = component({hasInstitutionalAgreement: false});
    expect(TestUtils.findNodesContainingText(wrapper, "your institution will need to sign").length).toBe(1)
  });

  it('Should show revoked status for controlled tier when the user has been revoked', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, userRevoked: true});
    expect(TestUtils.findNodesByExactText(wrapper, "Access to controlled tier data is revoked.").length).toBe(1)
  });

  it('Should show "get started" status for controlled tier when the user has not completed training', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: []});
    expect(TestUtils.findNodesByExactText(wrapper, "Get Started").length).toBe(1)
  });

  it('Should show success status for controlled tier when the user has completed training', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: ['controlled']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
  });

  it('Should show success status when the user is in the registered tier', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: ['registered']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
  });

  it('Should show success status when the user is in the registered tier and controlled tier', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: ['registered', 'controlled']});
    expect(wrapper.find(CheckCircle).length).toBe(2);
  });

  it('Should show success status when the user is in the registered tier and "get stated" for the controlled tier', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: ['registered']});
    expect(wrapper.find(CheckCircle).length).toBe(1);
    expect(TestUtils.findNodesByExactText(wrapper, "Get Started").length).toBe(1)
  });

  it('Should not show success status when the user is in not the registered tier and "get stated" for the controlled tier', async() => {
    const wrapper = component({hasInstitutionalAgreement: true, tiers: []});
    expect(wrapper.find(CheckCircle).length).toBe(0);
    expect(TestUtils.findNodesByExactText(wrapper, "Get Started").length).toBe(1)
  });

});
