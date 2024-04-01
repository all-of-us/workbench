import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render, screen } from '@testing-library/react';

import {
  panels,
  QuickTourReact,
  QuickTourReactProps,
  QuickTourReactState,
} from './quick-tour-modal';

describe('QuickTourModalComponent', () => {
  let props: QuickTourReactProps;
  const lastPanel = panels.length - 1;

  const component = () => {
    return render(<QuickTourReact {...props} />);
  };

  beforeEach(() => {
    props = {
      closeFunction: () => {},
    };
  });

  it('should render, should have a next and close button', () => {
    const { getByTestId } = component();
    expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
  });

  it('should not show the previous button when we are on the first slide', () => {
    const { queryByTestId } = component();

    expect(
      screen.queryByRole('button', { name: /previous/i })
    ).not.toBeInTheDocument();
  });

  it('should go to the next slide when we click next', () => {
    const { getByTestId } = component();
    expect(getByTestId('panel-title').textContent).toBe(panels[0].title);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(getByTestId('panel-title').textContent).toBe(panels[1].title);
  });

  it('should go to the panel we select from the breadcrumbs', () => {
    const { getByTestId } = component();
    const panelNum = 2;
    fireEvent.click(getByTestId(`breadcrumb${panelNum}`));
    expect(getByTestId('panel-title').textContent).toBe(panels[panelNum].title);
  });

  it('should go to the previous slide when we click previous', () => {
    const { getByTestId } = component();
    expect(getByTestId('panel-title').textContent).toBe(panels[0].title);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(getByTestId('panel-title').textContent).toBe(panels[1].title);
    fireEvent.click(screen.getByRole('button', { name: /previous/i }));
    expect(getByTestId('panel-title').textContent).toBe(panels[0].title);
  });

  it('should not show the next button when we are on the last slide', () => {
    const { getByTestId } = component();
    fireEvent.click(getByTestId(`breadcrumb${lastPanel}`));
    expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
  });

  it('should expand and retract the image when the resize icon is clicked', () => {
    const { queryByTestId } = component();
    // You cannot expand the image on the first page of the quick tour
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(queryByTestId('full-image-wrapper')).not.toBeInTheDocument();
    fireEvent.click(queryByTestId('expand-icon'));
    expect(queryByTestId('full-image-wrapper')).toBeInTheDocument();
    fireEvent.click(queryByTestId('shrink-icon'));
    expect(queryByTestId('full-image-wrapper')).not.toBeInTheDocument();
  });
});
