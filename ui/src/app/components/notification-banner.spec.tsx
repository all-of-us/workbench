import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render, screen } from '@testing-library/react';

import { NotificationBanner } from './notification-banner';

describe('NotificationBanner', () => {
  const component = () => {
    return render(
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
    const { container } = component();
    expect(container).toBeInTheDocument();
  });
});
