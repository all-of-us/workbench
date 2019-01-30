import {mount, shallow} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

import {NihCallback} from './component';

const endpoint = '/nih-callback';

function pushHistory(path: string): void {
  window.history.pushState({}, '', path);
}

beforeEach(() => {
  registerApiClient(ProfileApi, new ProfileApiStub());
});

describe('NihCallback', () => {

  it('should render', () => {
    const wrapper = shallow(<NihCallback/>);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show an error without a search argument', () => {
    pushHistory(endpoint);
    const wrapper = mount(<NihCallback/>);
    expect(window.location.pathname).toBe(endpoint);
    expect(wrapper.text()).toContain('Error');
  });

  it('should show an error with an empty search argument', () => {
    pushHistory(endpoint + '?token=');
    const wrapper = mount(<NihCallback/>);
    expect(window.location.pathname).toBe(endpoint);
    expect(wrapper.text()).toContain('Error');
  });

  it('should redirect with a valid search argument', () => {
    // Trigger a call to the profile service for NIH token update
    pushHistory(endpoint + '?token=valid-search-arg');
    const wrapper = mount(<NihCallback/>);
    expect(wrapper.text()).not.toContain('Error');
  });

});
