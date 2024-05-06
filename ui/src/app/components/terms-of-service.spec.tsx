import * as React from 'react';
import { mount, ReactWrapper, ShallowWrapper } from 'enzyme';

import { expectButtonElementDisabled } from '../../testing/react-test-helpers';
import {
  getDefaultNormalizer,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import { TermsOfService, TosProps } from 'app/components/terms-of-service';

type AnyWrapper = ShallowWrapper | ReactWrapper;
const getAgreementCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('input[aria-label="Acknowledge Terms"]');
};
const getAgreementCheckboxAlternate = (): HTMLInputElement => {
  return screen.getByRole('checkbox', { name: /acknowledge terms/i });
};
const getNextButton = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('[data-test-id="next-button"]');
};

const getNextButtonAlternate = (): HTMLElement => {
  return screen.getByRole('button', {
    name: /next/i,
  });
};

let props: TosProps;
const onCompleteSpy = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  props = {
    onComplete: onCompleteSpy,
    filePath: '/assets/documents/fake-document-path.html',
    afterPrev: false,
    showReAcceptNotification: false,
  };
});

it('should render', async () => {
  const wrapper = render(<TermsOfService {...props} />);
  await screen.findByText(
    'Please read through the entire agreement to continue.'
  );
});

it('should enable checkbox and next button with user input', async () => {
  const wrapper = mount(<TermsOfService {...props} />);

  expect(getAgreementCheckbox(wrapper).prop('disabled')).toBeTruthy();
  expect(getNextButton(wrapper).prop('disabled')).toBeTruthy();

  // In the real world, the user can either click "Scroll to bottom" or scroll until the last page
  // element is visible, at which point this state variable will be set to true. This behavior
  // was extremely difficult to simulate in Jest, and will be better-suited to an end-to-end test.
  //
  // As a workaround, we manually set the state here to allow us to test the input enablement.
  wrapper.setState({ hasReadEntireAgreement: true });

  expect(getAgreementCheckbox(wrapper).prop('disabled')).toBeFalsy();
  expect(getNextButton(wrapper).prop('disabled')).toBeTruthy();

  // Now, simulate checking the box, which should enable the "next" button.
  getAgreementCheckbox(wrapper).simulate('change', {
    target: { checked: true },
  });

  expect(getNextButton(wrapper).prop('disabled')).toBeFalsy();
});

it('should call onComplete when next button is pressed', async () => {
  const wrapper = mount(<TermsOfService {...props} />);

  wrapper.setState({ hasReadEntireAgreement: true });
  getAgreementCheckbox(wrapper).simulate('change', {
    target: { checked: true },
  });

  getNextButton(wrapper).simulate('click');

  expect(onCompleteSpy).toHaveBeenCalled();
});

it('should disable next button when next button is pressed', async () => {
  const wrapper = mount(<TermsOfService {...props} />);

  wrapper.setState({ hasReadEntireAgreement: true });
  getAgreementCheckbox(wrapper).simulate('change', {
    target: { checked: true },
  });

  // Next Button should be Enabled after checking Privacy and TOS checkbox
  expect(getNextButton(wrapper).prop('disabled')).toBeFalsy();

  getNextButton(wrapper).simulate('click');

  // After clicking Next, the button should be disabled
  expect(getNextButton(wrapper).prop('disabled')).toBeTruthy();
});

it('should enable NEXT button and checkbox should be selected if page is re-visited after Institution Page', async () => {
  props.afterPrev = true;
  const wrapper = mount(<TermsOfService {...props} />);

  expect(getAgreementCheckbox(wrapper).prop('checked')).toBeTruthy();
  expect(getNextButton(wrapper).prop('disabled')).toBeFalsy();
});
