import {mount, ReactWrapper, shallow, ShallowWrapper} from 'enzyme';
import * as React from 'react';
import {Document, Page} from 'react-pdf';
import AccountCreationTos, {AccountCreationTosProps} from './account-creation-tos';

type AnyWrapper = (ShallowWrapper|ReactWrapper);
const getPrivacyCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('CheckBox[data-test-id="privacy-statement-check"]');
};
const getTosCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('CheckBox[data-test-id="terms-of-service-check"]');
};
const getNextButton = (wrapper: AnyWrapper ): AnyWrapper => {
  return wrapper.find('[data-test-id="next-button"]');
};

let props: AccountCreationTosProps;
const onCompleteSpy = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  props = {
    windowSize: {width: 1700, height: 0},
    onComplete: onCompleteSpy,
    pdfPath: '/assets/documents/fake-document-path.pdf'
  };
});

it('should render', async() => {
  const wrapper = mount(<AccountCreationTos {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});

// TODO: this test is really about the PDF rendering, which should be bumped out when we encapsulate
// PDF rendering into its own component.
it('should load PDF pages', async() => {
  const wrapper = shallow(<AccountCreationTos {...props} />).shallow();

  // Initially we should have a document and no pages.
  expect(wrapper.find(Document).length).toEqual(1);
  expect(wrapper.find(Page).length).toEqual(0);

  // Simulate the PDF document loading and calling the 'onLoadSuccess' prop to indicate we have
  // 10 pages in the PDF.
  const pdfDocument = wrapper.find(Document);
  const onSuccess = pdfDocument.prop('onLoadSuccess') as (data: object) => {};
  onSuccess({numPages: 10});

  // We should now be rendering a <Page> component for each of the pages.
  expect(wrapper.find(Page).length).toEqual(10);
});

it('should enable checkboxes and next button with user input', async() => {
  // Note: since AccountCreationTos is exported as withWindowSize(...), we need to shallow-render
  // an extra time to get to the real AccountCreationTos component.
  const wrapper = shallow(<AccountCreationTos {...props} />).shallow();

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
  const wrapper = shallow(<AccountCreationTos {...props} />).shallow();

  wrapper.setState({hasReadEntireTos: true});
  getPrivacyCheckbox(wrapper).simulate('change', {target: {checked: true}});
  getTosCheckbox(wrapper).simulate('change', {target: {checked: true}});

  getNextButton(wrapper).simulate('click');

  expect(onCompleteSpy).toHaveBeenCalled();
});
