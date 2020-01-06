import {EnzymeAdapter, mount, shallow, ShallowWrapper} from 'enzyme';
import * as React from 'react';

import {CheckBox} from './inputs';

function findInput(wrapper: ShallowWrapper): ShallowWrapper {
  return wrapper.find('input[type="checkbox"]').first();
}

function clickCheckbox(wrapper: ShallowWrapper) {
  const currentChecked = findInput(wrapper).prop('checked');
  findInput(wrapper).simulate('change', {target: {checked: !currentChecked}});
}

describe('inputs', () => {
  it('handles click with manageOwnState', () => {
    // When the checkbox manages its own state, a click should cause the input
    // to change.
    const wrapper = shallow(<CheckBox label='asdf' manageOwnState={true} />);
    expect(findInput(wrapper).prop('checked')).toEqual(false);
    clickCheckbox(wrapper);
    wrapper.update();
    expect(findInput(wrapper).prop('checked')).toEqual(true);
  });

  it('uses props only when manageOwnState=false', () => {
    // This is the more common pattern used throughout the RW codebase, where
    // the caller of CheckBox provides "checked" as a prop and handles updates
    // by reacting to onChange callbacks.
    const wrapper = shallow(<CheckBox checked={true}/>);
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
