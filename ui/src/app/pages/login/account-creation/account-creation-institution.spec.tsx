import {mount, ReactWrapper, ShallowWrapper} from 'enzyme';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {ConfigApi, InstitutionApi, Profile} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {AccountCreationInstitution, Props} from './account-creation-institution';
import {ConfigApiStub} from 'testing/stubs/config-api-stub';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import defaultServerConfig from 'testing/default-server-config';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import SpyInstance = jest.SpyInstance;
import {Dropdown} from 'primereact/dropdown';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {defaultInstitutions} from 'testing/stubs/institution-api-stub';
import {InstitutionalRole} from 'generated/fetch';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';

let mockGetPublicInstitutionDetails: SpyInstance;

type AnyWrapper = (ShallowWrapper|ReactWrapper);

let props: Props;
function component(): ReactWrapper {
  return mount(<AccountCreationInstitution {...props}/>);
}

function getInstance(wrapper: AnyWrapper): AccountCreationInstitution {
  return wrapper.find(AccountCreationInstitution).instance() as AccountCreationInstitution;
}

function getInstitutionDropdown(wrapper: AnyWrapper): Dropdown {
  return wrapper.find('Dropdown[data-test-id="institution-dropdown"]').instance() as Dropdown;
}

function getEmailInput(wrapper: AnyWrapper): AnyWrapper {
  return wrapper.find('[data-test-id="contact-email"]').hostNodes();
}

function getRoleDropdown(wrapper: AnyWrapper): Dropdown {
  return wrapper.find('Dropdown[data-test-id="role-dropdown"]').instance() as Dropdown;
}

function getSubmitButton(wrapper: AnyWrapper): AnyWrapper {
  return wrapper.find('[data-test-id="submit-button"]');
}

const academicSpecificRoleOption = AccountCreationOptions.institutionalRoleOptions.find(
  x => x.value === InstitutionalRole.UNDERGRADUATE
);

const industrySpecificRoleOption = AccountCreationOptions.institutionalRoleOptions.find(
  x => x.value === InstitutionalRole.SENIORRESEARCHER
);


beforeEach(() => {
  serverConfigStore.next(defaultServerConfig);
  registerApiClient(ConfigApi, new ConfigApiStub());
  registerApiClient(InstitutionApi, new InstitutionApiStub());

  props = {
    profile: createEmptyProfile(true),
    onComplete: (profile: Profile) => {},
    onPreviousClick: (profile: Profile) => {}
  };

  mockGetPublicInstitutionDetails = jest.spyOn(institutionApi(), 'getPublicInstitutionDetails');
});

it('should render', async() => {
  const wrapper = component();
  expect(wrapper.exists()).toBeTruthy();
});

it('should load institutions list', async() => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();

  const options = getInstitutionDropdown(wrapper).props.options as Array<Object>;
  expect(options.length).toEqual(defaultInstitutions.length);
});

it('should show user-facing error message on data load error', async() => {
  mockGetPublicInstitutionDetails.mockRejectedValueOnce(new Response(null, {status: 500}));
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  expect(wrapper.find('[data-test-id="data-load-error"]').exists).toBeTruthy();
});

it('should reset role value & options when institution is selected', async() => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Simulate choosing an institution from the dropdown.
  const institutionDropdown = getInstitutionDropdown(wrapper);
  institutionDropdown.props.onChange({originalEvent: undefined, value: 'Broad'});
  await waitOneTickAndUpdate(wrapper);

  const roleDropdown = getRoleDropdown(wrapper);
  // Broad is an academic institution, which should contain the undergrad role.
  expect(roleDropdown.props.options).toContain(academicSpecificRoleOption);

  // Simulate selecting a role value for Broad.
  roleDropdown.props.onChange({originalEvent: undefined, value: academicSpecificRoleOption.value});
  expect(roleDropdown.props.value).toEqual(academicSpecificRoleOption.value);

  // Simulate switching to Verily.
  institutionDropdown.props.onChange({originalEvent: undefined, value: 'Verily'});

  // Role value should be cleared when institution changes.
  expect(roleDropdown.props.value).toBeNull();
  // Role options should have been swapped out w/ industry options.
  expect(roleDropdown.props.options).toContain(industrySpecificRoleOption);
});


it('should show validation errors in an empty form', async() => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  const errors = getInstance(wrapper).validate();
  expect(errors['profile.verifiedInstitutionalAffiliation.institutionShortName'].length).toBeGreaterThan(0);
  expect(errors['profile.verifiedInstitutionalAffiliation.institutionalRoleEnum'].length).toBeGreaterThan(0);
  expect(errors['profile.contactEmail'].length).toBeGreaterThan(0);
});

it('should validate email affiliation when inst and email are specified', async() => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Choose 'Broad' and enter an email address.
  getInstitutionDropdown(wrapper).props.onChange({originalEvent: undefined, value: 'Broad'});
  getEmailInput(wrapper).simulate('change', {target: {value: 'asdf@asdf.com'}});

  // Email address is entered, but the input hasn't been blurred. The form should know that a
  // response is required, but the API request hasn't been sent and returned yet.
  expect(getInstance(wrapper).validate()['checkEmailResponse'])
    .toContain('Institutional membership check has not completed');

  // Once we blur the input, the API request is sent. Since asdf.com is not a member, it will
  // block form submission.
  getEmailInput(wrapper).simulate('blur');
  await waitOneTickAndUpdate(wrapper);
  expect(getInstance(wrapper).validate()['checkEmailResponse'])
    .toContain('Email address is not a member of the selected institution');
});

it('should clear email validation when institution is changed', async() => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  getInstitutionDropdown(wrapper).props.onChange({originalEvent: undefined, value: 'Broad'});
  getEmailInput(wrapper).simulate('change', {target: {value: 'asdf@broadinstitute.org'}});
  // Blur the email input field and wait for the API request to complete.
  getEmailInput(wrapper).simulate('blur');
  await waitOneTickAndUpdate(wrapper);
  getRoleDropdown(wrapper).props.onChange({originalEvent: undefined, value: InstitutionalRole.EARLYCAREER});

  // At this point, the form should be ready to submit.
  expect(getInstance(wrapper).validate()).toBeUndefined();

  // ... Mimic changing the institution & role, but leaving email as-is.
  getInstitutionDropdown(wrapper).props.onChange({originalEvent: undefined, value: 'Verily'});
  getRoleDropdown(wrapper).props.onChange({originalEvent: undefined, value: InstitutionalRole.PREDOCTORAL});

  // The form should be blocked now due to lack of email verification.
  expect(getInstance(wrapper).validate()['checkEmailResponse']).toBeTruthy();
});

it('should trigger email check when email is filled in before choosing institution', async() => {
  // This test ensures that a user can fill in their email address first, then choose an
  // institution, and still be able to complete the form. This ensures that the institution change
  // can trigger a checkEmail request.
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  getEmailInput(wrapper).simulate('change', {target: {value: 'asdf@broadinstitute.org'}});
  getEmailInput(wrapper).simulate('blur');
  // This shouldn't strictly be needed here, since we expect the API request not to be sent due to
  // no institution being chosen. But for consistency w/ other tests, it's included.
  await waitOneTickAndUpdate(wrapper);

  getInstitutionDropdown(wrapper).props.onChange({originalEvent: undefined, value: 'Broad'});
  getRoleDropdown(wrapper).props.onChange({originalEvent: undefined, value: InstitutionalRole.EARLYCAREER});
  await waitOneTickAndUpdate(wrapper);

  // At this point, the form should be ready to submit.
  expect(getInstance(wrapper).validate()).toBeUndefined();
});


it('should call callback with correct form data', async() => {
  let profile: Profile = null;
  props.onComplete = (formProfile: Profile) => {
    profile = formProfile;
  };
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Fill in all fields with reasonable data.
  getInstitutionDropdown(wrapper).props.onChange({originalEvent: undefined, value: 'VUMC'});
  getEmailInput(wrapper).simulate('change', {target: {value: 'asdf@vumc.org'}});
  getEmailInput(wrapper).simulate('blur');
  getRoleDropdown(wrapper).props.onChange({originalEvent: undefined, value: InstitutionalRole.UNDERGRADUATE});
  // Await one tick for the APi response to update state and allow form submission.
  await waitOneTickAndUpdate(wrapper);

  getSubmitButton(wrapper).simulate('click');

  expect(profile).toBeTruthy();
});
