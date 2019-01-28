import { mount } from 'enzyme';
import * as React from 'react';

import {ScrollReact} from './component';

interface Props {
  icon: string;
  shade: string;
  style: object;
}

describe('ScrollComponent', () => {

  let props: Props;

  const component = () => {
    return mount(<ScrollReact {...props}/>);
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
    expect(lightShadeSvgProp.style.color).toBe('#5FAEE0');
    expect(darkShadeSvgProp.style.color).toBe('#2691D0');
    expect(lightShadeSvgProp.style.opacity).toBe('0.54');
    expect(darkShadeSvgProp.style.opacity).toBe('1');
    expect(lightShadeSvgProp.width).toBe('40');
    expect(darkShadeSvgProp.width).toBe('47');
    expect(lightShadeSvgProp.height).toBe('41');
    expect(darkShadeSvgProp.height).toBe('48');
  });
});
