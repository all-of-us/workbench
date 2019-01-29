import { mount } from 'enzyme';
import * as React from 'react';

import {Scroll} from './component';

interface Props {
  icon: string;
  shade: string;
  style: object;
}

describe('ScrollComponent', () => {

  let props: Props;

  const component = () => {
    return mount(<Scroll {...props}/>);
  };

  beforeEach(() => {
    props = {
      icon:  '',
      shade: 'dark',
      style: {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should flip icon if it is RightIcon', () => {
    props.icon = 'RightIcon';
    const wrapper = component();
    const transformStyle = wrapper.find('svg').props().style;
    expect(transformStyle.transform).toBe('scaleX(-1)');
  });

  it('should update style if shade is light', () => {
    const darkShadeWrapper = component();
    const darkShadeSvgProp = darkShadeWrapper.find('svg').props();
    props.shade = 'Light';

    const lightShadeWrapper = component();
    const lightShadeSvgProp = lightShadeWrapper.find('svg').props();

    expect(darkShadeSvgProp.style).not.toBe(lightShadeSvgProp.style);
    expect(lightShadeSvgProp.style.color).not.toBe(darkShadeSvgProp.style.color);
    expect(lightShadeSvgProp.style.opacity).not.toBe(darkShadeSvgProp.style.opacity);
    expect(darkShadeSvgProp.width).not.toBe(lightShadeSvgProp.width);
  });
});
