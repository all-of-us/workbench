import * as React from 'react';
import { mount, ReactWrapper, ShallowWrapper } from 'enzyme';

import {
  ConfigApi,
  InstitutionalRole,
  InstitutionApi,
  Profile,
} from 'generated/fetch';

import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import {
  institutionApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { ConfigApiStub } from 'testing/stubs/config-api-stub';
import {
  defaultInstitutions,
  InstitutionApiStub,
} from 'testing/stubs/institution-api-stub';

import {
  AccountCreationInstitution,
  AccountCreationInstitutionProps,
} from './account-creation-institution';
import SpyInstance = jest.SpyInstance;

let mockGetPublicInstitutionDetails: SpyInstance;

type AnyWrapper = ShallowWrapper | ReactWrapper;

let props: AccountCreationInstitutionProps;
function component(): ReactWrapper {
  return mount(<AccountCreationInstitution {...props} />);
}

function getInstance(wrapper: AnyWrapper): AccountCreationInstitution {
  return wrapper
    .find(AccountCreationInstitution)
    .instance() as AccountCreationInstitution;
}

function getInstitutionDropdown(wrapper: AnyWrapper) {
  const matchedElements = wrapper
    .find('Dropdown[data-test-id="institution-dropdown"]')
    ?.getElements();

  return matchedElements?.[0];
}

function getEmailInput(wrapper: AnyWrapper): AnyWrapper {
  return wrapper.find('[data-test-id="contact-email"]').hostNodes();
}

function getEmailErrorMessage(wrapper: AnyWrapper): AnyWrapper {
  return wrapper.find('[data-test-id="email-error-message"]');
}

function getRoleDropdown(wrapper: AnyWrapper) {
  const matchedElements = wrapper
    .find('Dropdown[data-test-id="role-dropdown"]')
    ?.getElements();

  return matchedElements ? matchedElements[0] : undefined;
}

function getSubmitButton(wrapper: AnyWrapper): AnyWrapper {
  return wrapper.find('[data-test-id="submit-button"]');
}

const academicSpecificRoleOption =
  AccountCreationOptions.institutionalRoleOptions.find(
    (x) => x.value === InstitutionalRole.UNDERGRADUATE
  );

const industrySpecificRoleOption =
  AccountCreationOptions.institutionalRoleOptions.find(
    (x) => x.value === InstitutionalRole.SENIOR_RESEARCHER
  );

const eventDefaults = {
  stopPropagation: () => undefined,
  preventDefault: () => undefined,
};

beforeEach(() => {
  serverConfigStore.set({ config: defaultServerConfig });
  registerApiClient(ConfigApi, new ConfigApiStub());
  registerApiClient(InstitutionApi, new InstitutionApiStub());

  props = {
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {},
  };

  mockGetPublicInstitutionDetails = jest.spyOn(
    institutionApi(),
    'getPublicInstitutionDetails'
  );
});

it('should render', async () => {
  const wrapper = component();
  expect(wrapper.exists()).toBeTruthy();
});

it('should load institutions list', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();

  const options = getInstitutionDropdown(wrapper).props.options as Array<{
    value;
    label;
  }>;
  expect(options.length).toEqual(defaultInstitutions.length);
  // Drop down list should be sorted in ASC order
  expect(options[0].label).not.toEqual(defaultInstitutions[0].displayName);
  expect(options[0].label).toEqual('Broad Institute');
  console.log(options[0]);
  console.log(defaultInstitutions[0]);
});

it('should show user-facing error message on data load error', async () => {
  mockGetPublicInstitutionDetails.mockRejectedValueOnce(
    new Response(null, { status: 500 })
  );
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  expect(wrapper.find('[data-test-id="data-load-error"]').exists).toBeTruthy();
});

it('should reset role value & options when institution is selected', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Simulate choosing an institution from the dropdown.
  const institutionDropdown = getInstitutionDropdown(wrapper);
  institutionDropdown.props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'Broad',
    target: { name: '', id: '', value: 'Broad' },
  });
  await waitOneTickAndUpdate(wrapper);

  let roleDropdown = getRoleDropdown(wrapper);
  // Broad is an academic institution, which should contain the undergrad role.
  expect(roleDropdown.props.options).toContain(academicSpecificRoleOption);

  // Simulate selecting a role value for Broad.
  roleDropdown.props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: academicSpecificRoleOption.value,
    target: { name: '', id: '', value: academicSpecificRoleOption.value },
  });
  await waitOneTickAndUpdate(wrapper);
  roleDropdown = getRoleDropdown(wrapper);
  expect(roleDropdown.props.value).toEqual(academicSpecificRoleOption.value);

  // Simulate switching to Verily.
  institutionDropdown.props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'Verily',
    target: { name: '', id: '', value: 'Verily' },
  });

  await waitOneTickAndUpdate(wrapper);
  roleDropdown = getRoleDropdown(wrapper);

  // Role value should be cleared when institution changes.
  expect(roleDropdown.props.value).toBeUndefined();
  // Role options should have been swapped out w/ industry options.
  expect(roleDropdown.props.options).toContain(industrySpecificRoleOption);
});

it('should show validation errors in an empty form', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  const errors = getInstance(wrapper).validate();
  expect(
    errors['profile.verifiedInstitutionalAffiliation.institutionShortName']
      .length
  ).toBeGreaterThan(0);
  expect(
    errors['profile.verifiedInstitutionalAffiliation.institutionalRoleEnum']
      .length
  ).toBeGreaterThan(0);
  expect(errors['profile.contactEmail'].length).toBeGreaterThan(0);
});

it('should validate email affiliation when inst and email address are specified', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Choose 'Broad' and enter an email address.
  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'Broad',
    target: { name: '', id: '', value: 'Broad' },
  });
  getEmailInput(wrapper).simulate('change', {
    target: { value: 'asdf@asdf.com' },
  });

  // Email address is entered, but the input hasn't been blurred. The form should know that a
  // response is required, but the API request hasn't been sent and returned yet.
  expect(getInstance(wrapper).validate().checkEmailResponse).toContain(
    'Institutional membership check has not completed'
  );

  // Once we blur the input, the API request is sent. Since asdf.com is not a member, it will
  // block form submission.
  getEmailInput(wrapper).simulate('blur');

  await waitOneTickAndUpdate(wrapper);
  expect(getInstance(wrapper).validate().checkEmailResponse).toContain(
    'Email address is not a member of the selected institution'
  );

  expect(getEmailErrorMessage(wrapper).getDOMNode().textContent).toBe(
    'The institution has authorized access only to select members.' +
      'Please click here to request to be added to the institution'
  );
});

it('should validate email affiliation when inst and email domain are specified', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Choose 'VUMC' and enter an email address.
  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'VUMC',
    target: { name: '', id: '', value: 'VUMC' },
  });
  getEmailInput(wrapper).simulate('change', {
    target: { value: 'asdf@asdf.com' },
  });

  // Email address is entered, but the input hasn't been blurred. The form should know that a
  // response is required, but the API request hasn't been sent and returned yet.
  expect(getInstance(wrapper).validate().checkEmailResponse).toContain(
    'Institutional membership check has not completed'
  );

  // Once we blur the input, the API request is sent. Since asdf.com is not a member, it will
  // block form submission.
  getEmailInput(wrapper).simulate('blur');

  await waitOneTickAndUpdate(wrapper);
  expect(getInstance(wrapper).validate().checkEmailResponse).toContain(
    'Email address is not a member of the selected institution'
  );

  expect(getEmailErrorMessage(wrapper).getDOMNode().textContent).toBe(
    'Your email does not match your institution'
  );
});

it('should display validation icon only after email verification', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Choose 'VUMC' and enter an email address.
  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'VUMC',
    target: { name: '', id: '', value: 'VUMC' },
  });
  getEmailInput(wrapper).simulate('change', {
    target: { value: 'asdf@wrongDomain.com' },
  });

  // Email address is entered, but the input hasn't been blurred. The form should know that a
  // response is required, but the API request hasn't been sent and returned yet.
  expect(getInstance(wrapper).validate().checkEmailResponse).toContain(
    'Institutional membership check has not completed'
  );

  // At this point, the validation icon should not be displayed as email has not been verified
  await waitOneTickAndUpdate(wrapper);
  expect(
    wrapper.find('[data-test-id="email-validation-icon"]').children().length
  ).toBe(0);
  getEmailInput(wrapper).simulate('blur');

  // Email has beeb verified, the validation icon should now be displayed
  await waitOneTickAndUpdate(wrapper);
  expect(
    wrapper.find('[data-test-id="email-validation-icon"]').children().length
  ).toBe(1);
});

it('should clear email validation when institution is changed', async () => {
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'VUMC',
    target: { name: '', id: '', value: 'VUMC' },
  });
  getEmailInput(wrapper).simulate('change', {
    target: { value: 'asdf@vumc.org' },
  });
  // Blur the email input field and wait for the API request to complete.
  getEmailInput(wrapper).simulate('blur');
  await waitOneTickAndUpdate(wrapper);
  getRoleDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: InstitutionalRole.EARLY_CAREER,
    target: { name: '', id: '', value: InstitutionalRole.EARLY_CAREER },
  });

  // At this point, the form should be ready to submit.
  expect(getInstance(wrapper).validate()).toBeUndefined();

  // ... Mimic changing the institution & role, but leaving email as-is.
  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'Verily',
    target: { name: '', id: '', value: 'Verily' },
  });
  getRoleDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: InstitutionalRole.PRE_DOCTORAL,
    target: { name: '', id: '', value: InstitutionalRole.PRE_DOCTORAL },
  });

  // The form should be blocked now due to lack of email verification.
  expect(getInstance(wrapper).validate().checkEmailResponse).toBeTruthy();
});

it('should trigger email check when email is filled in before choosing institution', async () => {
  // This test ensures that a user can fill in their email address first, then choose an
  // institution, and still be able to complete the form. This ensures that the institution change
  // can trigger a checkEmail request.
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  getEmailInput(wrapper).simulate('change', {
    target: { value: 'contactEmail@broadinstitute.org' },
  });
  getEmailInput(wrapper).simulate('blur');
  // This shouldn't strictly be needed here, since we expect the API request not to be sent due to
  // no institution being chosen. But for consistency w/ other tests, it's included.
  await waitOneTickAndUpdate(wrapper);

  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'Broad',
    target: { name: '', id: '', value: 'Broad' },
  });
  getRoleDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: InstitutionalRole.EARLY_CAREER,
    target: { name: '', id: '', value: InstitutionalRole.EARLY_CAREER },
  });
  await waitOneTickAndUpdate(wrapper);

  // At this point, the form should be ready to submit.
  expect(getInstance(wrapper).validate()).toBeUndefined();
});

it('should call callback with correct form data', async () => {
  let profile: Profile = null;
  props.onComplete = (formProfile: Profile) => {
    profile = formProfile;
  };
  const wrapper = component();
  await waitOneTickAndUpdate(wrapper);

  // Fill in all fields with reasonable data.
  getInstitutionDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: 'VUMC',
    target: { name: '', id: '', value: 'VUMC' },
  });
  getEmailInput(wrapper).simulate('change', {
    target: { value: 'asdf@vumc.org' },
  });
  getEmailInput(wrapper).simulate('blur');
  getRoleDropdown(wrapper).props.onChange({
    ...eventDefaults,
    originalEvent: undefined,
    value: InstitutionalRole.UNDERGRADUATE,
    target: { name: '', id: '', value: InstitutionalRole.UNDERGRADUATE },
  });
  // Await one tick for the APi response to update state and allow form submission.
  await waitOneTickAndUpdate(wrapper);

  getSubmitButton(wrapper).simulate('click');

  expect(profile).toBeTruthy();
});
