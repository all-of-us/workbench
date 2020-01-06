import {
  EnzymeAdapter,
  mount,
  ReactWrapper,
  shallow,
  ShallowWrapper,
} from 'enzyme';
import * as React from 'react';

import {CheckBox} from './inputs';

function findInput(wrapper: (ShallowWrapper|ReactWrapper)): (ShallowWrapper|ReactWrapper) {
  return wrapper.find('input[type="checkbox"]').first();
}

function clickCheckbox(wrapper: (ShallowWrapper|ReactWrapper)) {
  const currentChecked = findInput(wrapper).prop('checked');
  findInput(wrapper).simulate('change', {target: {checked: !currentChecked}});
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
    const wrapper = mount(<CheckBox checked={true} manageOwnState={false}/>);
    expect(findInput(wrapper).prop('checked')).toEqual(true);
    // A user click shouldn't directly affect the checked state.
    clickCheckbox(wrapper);
    expect(findInput(wrapper).prop('checked')).toEqual(true);
    // Changing the props will change the checked state.
    wrapper.setProps({checked: false} as any);
    expect(findInput(wrapper).prop('checked')).toEqual(false);
  });

  it('calls onChange with target checked state', () => {
    let checked = null;
    const wrapper = shallow(<CheckBox onChange={(value) => checked = value}/>);
    clickCheckbox(wrapper);
    expect(checked).toEqual(true);
  });

  it('renders with plain-text label', () => {
    const wrapper = mount(<CheckBox label='hello'/>);
    expect(wrapper).toBeTruthy();
  });

  it('renders with HTML label', () => {
    const wrapper = mount(<CheckBox label={(<span>Hello, <i>world</i></span>)}/>);
    expect(wrapper).toBeTruthy();
  });
});
