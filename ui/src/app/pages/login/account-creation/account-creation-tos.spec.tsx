import {mount, ReactWrapper, shallow, ShallowWrapper} from 'enzyme';
import * as React from 'react';
import {AccountCreationTos, AccountCreationTosProps} from 'app/pages/login/account-creation/account-creation-tos';

type AnyWrapper = (ShallowWrapper|ReactWrapper);
const getPrivacyCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('input[data-test-id="privacy-statement-check"]');
};
const getTosCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('input[data-test-id="terms-of-service-check"]');
};
const getNextButton = (wrapper: AnyWrapper ): AnyWrapper => {
  return wrapper.find('[data-test-id="next-button"]');
};

let props: AccountCreationTosProps;
const onCompleteSpy = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  props = {
    onComplete: onCompleteSpy,
    pdfPath: '/assets/documents/fake-document-path.pdf'
  };
});

it('should render', async() => {
  const wrapper = mount(<AccountCreationTos {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});

it('should enable checkboxes and next button with user input', async() => {
  const wrapper = mount(<AccountCreationTos {...props} />);

  expect(getPrivacyCheckbox(wrapper).prop('disabled')).toBeTruthy();
  expect(getTosCheckbox(wrapper).prop('disabled')).toBeTruthy();
  expect(getNextButton(wrapper).prop('disabled')).toBeTruthy();

  // In the real world, the user can either click "Scroll to bottom" or scroll until the last page
  // element is visible, at which point this state variable will be set to true. This behavior
  // was extremely difficult to simulate in Jest, and will be better-suited to an end-to-end test.
  //
  // As a workaround, we manually set the state here to allow us to test the input enablement.
  wrapper.setState({hasReadEntireTos: true});

  expect(getPrivacyCheckbox(wrapper).prop('disabled')).toBeFalsy();
  expect(getTosCheckbox(wrapper).prop('disabled')).toBeFalsy();
  expect(getNextButton(wrapper).prop('disabled')).toBeTruthy();

  // Now, simulate checking both boxes, which should enable the "next" button.
  getPrivacyCheckbox(wrapper).simulate('change', {target: {checked: true}});
  getTosCheckbox(wrapper).simulate('change', {target: {checked: true}});

  expect(getNextButton(wrapper).prop('disabled')).toBeFalsy();
});

it('should call onComplete when next button is pressed', async() => {
  const wrapper = mount(<AccountCreationTos {...props} />);

  wrapper.setState({hasReadEntireTos: true});
  getPrivacyCheckbox(wrapper).simulate('change', {target: {checked: true}});
  getTosCheckbox(wrapper).simulate('change', {target: {checked: true}});

  getNextButton(wrapper).simulate('click');

  expect(onCompleteSpy).toHaveBeenCalled();
});
