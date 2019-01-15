import { mount } from 'enzyme';
import * as React from 'react';

import {SettingsReact, SettingsState} from './component';

import {Cluster, ClusterStatus} from 'generated/fetch/api';

describe('SettingsComponent', () => {
  let props: {};

  const component = () => {
    return mount<SettingsReact, {}, SettingsState>
    (<SettingsReact {...props}/>);
  };

  beforeAll(() => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    document.body.appendChild(popupRoot);
  });

  afterAll(() => {
    document.removeChild(document.getElementById('popup-root'));
  });

  beforeEach(() => {
    props = {};
  });

  it('should not open the cluster reset modal when no cluster', () => {
    const wrapper = component();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });

  it('should open the cluster reset modal when there is cluster', () => {
    const wrapper = component();
    wrapper.setState({cluster: {clusterName: 'testing',
      clusterNamespace: 'testing',
      status: ClusterStatus.Running}});
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
  });

  it('should open the cluster reset modal when there is cluster', () => {
    const wrapper = component();
    wrapper.setState({cluster: {clusterName: 'testing',
      clusterNamespace: 'testing',
      status: ClusterStatus.Running}});
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
  });

  it('should open the cluster reset modal when there is cluster', () => {
    const wrapper = component();
    wrapper.setState({cluster: {clusterName: 'testing',
      clusterNamespace: 'testing',
      status: ClusterStatus.Running}});
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
  });
});
