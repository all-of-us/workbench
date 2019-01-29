import {mount} from 'enzyme';
import {shallow} from 'enzyme';
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

  it('should not redirect without search argument', () => {
    pushHistory(endpoint);
    shallow(<NihCallback/>);
    expect(window.location.pathname).toBe(endpoint);
  });

  it('should redirect with a valid search argument', () => {
    // Trigger a call to the profile service for NIH token update
    pushHistory(endpoint + '?valid-search-arg');

    // window.location is not implemented in jest - mock here.
    window.location.assign = jest.fn();
    const wrapper = shallow(<NihCallback/>);
    expect(wrapper.exists()).toBeTruthy();
  });

});
