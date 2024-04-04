import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render, screen } from '@testing-library/react';

import { NotificationBanner } from './notification-banner';

describe('NotificationBanner', () => {
  const bannerText = 'blah';
  const component = () => {
    return render(
      <MemoryRouter>
        <NotificationBanner
          dataTestId='banner'
          text={bannerText}
          buttonText='BLAH'
          buttonPath='yahoo.com'
          buttonDisabled={true}
          bannerTextWidth='177px'
        />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    component();
    expect(screen.getByText(bannerText)).toBeInTheDocument();
  });
});
