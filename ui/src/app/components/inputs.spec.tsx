import * as React from 'react';
import { mount, ReactWrapper, shallow, ShallowWrapper } from 'enzyme';

import { CheckBox, TextAreaWithLengthValidationMessage } from './inputs';

const initialText = 'Hey';

function findInput(
  wrapper: ShallowWrapper | ReactWrapper
): ShallowWrapper | ReactWrapper {
  return wrapper.find('input[type="checkbox"]').first();
}

function clickCheckbox(wrapper: ShallowWrapper | ReactWrapper) {
  const currentChecked = findInput(wrapper).prop('checked');
  findInput(wrapper).simulate('change', {
    target: { checked: !currentChecked },
  });
}

describe('inputs', () => {
  it('click causes DOM checked state to change', () => {
    // When the checkbox manages its own state, a click should cause the input
    // to change.
    const wrapper = shallow(<CheckBox label='asdf' />);
    expect(findInput(wrapper).prop('checked')).toEqual(false);
    clickCheckbox(wrapper);
    wrapper.update();
    expect(findInput(wrapper).prop('checked')).toEqual(true);
  });

  it('uses "checked" to set initial state', () => {
    const wrapper = shallow(<CheckBox checked={true} />);
    expect(findInput(wrapper).prop('checked')).toEqual(true);
  });

  it('uses props only when manageOwnState=false', () => {
    // This pattern is disfavored but still exists in the RW codebase, where
    // the caller of CheckBox provides "checked" as a prop and handles updates
    // by reacting to onChange callbacks.
    const wrapper = mount(<CheckBox checked={true} manageOwnState={false} />);
    expect(findInput(wrapper).prop('checked')).toEqual(true);
    // A user click shouldn't directly affect the checked state.
    clickCheckbox(wrapper);
    expect(findInput(wrapper).prop('checked')).toEqual(true);
    // Changing the props will change the checked state.
    wrapper.setProps({ checked: false } as any);
    expect(findInput(wrapper).prop('checked')).toEqual(false);
  });

  it('calls onChange with target checked state', () => {
    let checked = null;
    const wrapper = shallow(
      <CheckBox onChange={(value) => (checked = value)} />
    );
    clickCheckbox(wrapper);
    expect(checked).toEqual(true);
  });

  it('renders with plain-text label', () => {
    const wrapper = mount(<CheckBox label='hello' />);
    expect(wrapper).toBeTruthy();
  });

  it('renders with HTML label', () => {
    const wrapper = mount(
      <CheckBox
        label={
          <span>
            Hello, <i>world</i>
          </span>
        }
      />
    );
    expect(wrapper).toBeTruthy();
  });

  it('Shows characters remaining warning ', () => {
    let wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={5}
        onChange={() => {}}
      />
    );
    // Length of initialText (Hey): 3 characters remaining 5 - 3
    expect(
      wrapper.find('[data-test-id="characterLimit"]').first().text()
    ).toEqual(`2 characters remaining`);

    wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText + 'me'}
        maxCharacters={5}
        onChange={() => {}}
      />
    );

    expect(
      wrapper.find('[data-test-id="characterLimit"]').first().text()
    ).toEqual(`0 characters remaining`);
  });

  it('Shows characters over warning', () => {
    const wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText + ' lets test'}
        maxCharacters={5}
        onChange={() => {}}
      />
    );

    expect(
      wrapper.find('[data-test-id="characterLimit"]').first().text()
    ).toEqual('8 characters over');
  });

  it('Shows too short warning if text input is less the short characters', () => {
    // No props for tooShortWarning should not show any warning
    let wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
      />
    );

    wrapper.find('textarea').simulate('blur');
    expect(wrapper.find('[data-test-id="warning"]').length).toBe(0);

    // Props for tooShortWarning should show any warning if the text is less than tooShortWarningCharacters
    wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning={'Testing too short'}
        tooShortWarningCharacters={5}
      />
    );
    wrapper.find('textarea').simulate('blur');
    expect(wrapper.find('[data-test-id="warning"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="warning"]').first().text()).toBe(
      'Testing too short'
    );

    // Props for tooShortWarning should not show any warning if the text is more than tooShortWarningCharacters
    wrapper = mount(
      <TextAreaWithLengthValidationMessage
        id={'test'}
        initialText={initialText}
        maxCharacters={15}
        onChange={() => {}}
        tooShortWarning={'Testing too short'}
        tooShortWarningCharacters={2}
      />
    );
    wrapper.find('textarea').simulate('blur');
    expect(wrapper.find('[data-test-id="warning"]').length).toBe(0);
  });
});
