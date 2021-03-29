import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore, urlParamsStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {DuaType, InstitutionApi} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {AdminInstitutionEdit} from './admin-institution-edit';

describe('AdminInstitutionEditSpec', () => {

  const component = () => {
    return mount(<AdminInstitutionEdit/>);
  };

  beforeEach(() => {
    serverConfigStore.next(defaultServerConfig);

    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });

  it('should throw error for existing Institution if display name is more than 80 characters', async() => {
    urlParamsStore.next({
      institutionId: 'Verily'
    });
    const wrapper = mount(<AdminInstitutionEdit/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const testInput = fp.repeat(83, 'a');
    const displayNameText = wrapper.find('[id="displayName"]').first();
    displayNameText.simulate('change', {target: {value: testInput}});
    displayNameText.simulate('blur');
    expect(wrapper.find('[data-test-id="displayNameError"]').first().prop('children'))
      .toContain('Display name must be 80 characters or less');
  });

  it('should default DUA to Master', async() => {
    urlParamsStore.next({
      institutionId: 'Verily'
    });
    const wrapper = mount(<AdminInstitutionEdit/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]');
    expect(agreementTypeDropDown.first().props().value).toBe(DuaType.MASTER);
  });

  it('should show appropriate section after changing agreement type', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const testInput = fp.repeat(81, 'a');

    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange({
      originalEvent: undefined,
      value: DuaType.MASTER,
      target: {name: 'name', id: '', value: DuaType.MASTER}
    });
    await waitOneTickAndUpdate(wrapper);

    let emailAddressDiv = wrapper.find('[data-test-id="emailAddress"]');
    let emailDomainDiv = wrapper.find('[data-test-id="emailDomain"]');
    expect(emailAddressDiv.length).toBe(0);
    expect(emailDomainDiv.length).toBe(2);

    const emailDomainLabel = emailDomainDiv.first().props().children[0];
    expect(emailDomainLabel.props.children).toBe('Accepted Email Domains');

    agreementTypeDropDown.props.onChange(
      {
        originalEvent: undefined, value: DuaType.RESTRICTED,
        target: {name: 'name', id: '', value: DuaType.RESTRICTED}
      });
    await waitOneTickAndUpdate(wrapper);

    emailAddressDiv = wrapper.find('[data-test-id="emailAddress"]');
    emailDomainDiv = wrapper.find('[data-test-id="emailDomain"]');
    expect(emailAddressDiv.length).toBe(2);
    expect(emailDomainDiv.length).toBe(0);

    const emailAddressLabel = emailAddressDiv.first().props().children[0];

    expect(emailAddressLabel.props.children).toBe('Accepted Email Addresses');

  });

  it('Should display error in case of invalid email Address Format', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    let emailAddressError = wrapper.find('[data-test-id="emailAddressError"]');
    expect(emailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: DuaType.RESTRICTED, target: {name: 'name', id: '', value: DuaType.RESTRICTED}});
    await waitOneTickAndUpdate(wrapper);

    // In case of a single entry which is not in the correct format
    wrapper.find('[data-test-id="emailAddressInput"]').first().simulate('change', {target: {value: 'invalidEmail@domain'}});
    wrapper.find('[data-test-id="emailAddressInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailAddressError"]');
    expect(emailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : invalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    wrapper.find('[data-test-id="emailAddressInput"]').first()
      .simulate('change', {
        target: {
          value:
            'invalidEmail@domain@org,\n' +
            'correctEmail@someDomain.org,\n' +
            ' correctEmail.123.hello@someDomain567.org.com   \n' +
            ' invalidEmail   ,\n' +
            ' justDomain.org,\n' +
            'someEmail@broadinstitute.org\n' +
            'nope@just#plain#wrong'
        }
      });
    wrapper.find('[data-test-id="emailAddressInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailAddressError"]');
    expect(emailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    wrapper.find('[data-test-id="emailAddressInput"]').first()
      .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    wrapper.find('[data-test-id="emailAddressInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailAddressError"]');
    expect(emailAddressError.length).toBe(0);

  });

  it('Should display error in case of invalid email Domain Format', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    let emailAddressError = wrapper.find('[data-test-id="emailDomainError"]');
    expect(emailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: DuaType.MASTER, target: {name: 'name', id: '', value: DuaType.MASTER}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="emailDomainInput"]').first()
      .simulate('change', {target: {value: 'invalidEmail@domain'}});
    wrapper.find('[data-test-id="emailDomainInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailDomainError"]');
    expect(emailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    wrapper.find('[data-test-id="emailDomainInput"]').first()
      .simulate('change', {
        target: {
          value:
            'someEmailAddress@domain@org,\n' +
            'someDomain123.org.com        ,\n' +
            ' justSomeText,\n' +
            ' justDomain.org,\n' +
            'broadinstitute.org#wrongTest'
        }
      });
    wrapper.find('[data-test-id="emailDomainInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailDomainError"]');
    expect(emailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');


    wrapper.find('[data-test-id="emailDomainInput"]').first()
      .simulate('change', {target: {value: 'domain.com'}});
    wrapper.find('[data-test-id="emailDomainInput"]').first().simulate('blur');
    emailAddressError = wrapper.find('[data-test-id="emailDomainError"]');
    expect(emailAddressError.length).toBe(0);

  });

  it('Should ignore empty string in email Domain', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: DuaType.MASTER, target: {name: 'name', id: '', value: DuaType.MASTER}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="emailDomainInput"]').first()
      .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    wrapper.find('[data-test-id="emailDomainInput"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="emailDomainInput"]').first().prop('value'))
      .toBe('validEmail.com,\njustSomeRandom.domain');
  });

  it('Should ignore whitespaces in email address', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const agreementTypeDropDown = wrapper.find('[data-test-id="agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: DuaType.MASTER, target: {name: 'name', id: '', value: DuaType.MASTER}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="emailDomainInput"]').first()
      .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    wrapper.find('[data-test-id="emailDomainInput"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="emailDomainInput"]').first().prop('value'))
      .toBe('someDomain.com,\njustSomeRandom.domain');
  });
});
