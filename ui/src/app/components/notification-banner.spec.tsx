import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { NotificationBanner } from './notification-banner';

describe('NotificationBanner', () => {
  const component = () => {
    return mount(
      <MemoryRouter>
        <NotificationBanner
          dataTestId='banner'
          text='blah'
          buttonText='BLAH'
          buttonPath='yahoo.com'
          buttonDisabled={true}
          bannerTextWidth='177px'
        />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });
});
