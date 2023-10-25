import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';

import { Scroll } from './scroll';

describe('ScrollComponent', () => {
  it('should render', () => {
    const { container } = render(
      <Scroll dir='left' shade='light' className='scrollClass' />
    );
    expect(container.querySelector('.scrollClass')).toBeInTheDocument();
  });
});
