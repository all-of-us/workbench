import '@testing-library/jest-dom';

import { fireEvent, render, screen } from '@testing-library/react';

import {
  panels,
  QuickTourReact,
  QuickTourReactProps,
} from './quick-tour-modal';

describe('QuickTourModalComponent', () => {
  let props: QuickTourReactProps;

  const component = () => {
    return render(<QuickTourReact {...props} />);
  };

  beforeEach(() => {
    props = {
      closeFunction: () => {},
    };
  });

  it('should render, should have a next and close button', () => {
    component();
    expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
  });

  it('should not show the previous button when we are on the first slide', () => {
    component();
    expect(
      screen.queryByRole('button', { name: /previous/i })
    ).not.toBeInTheDocument();
  });

  it('should go to the next slide when we click next', () => {
    component();
    expect(screen.getByTestId('panel-title').textContent).toBe(panels[0].title);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByTestId('panel-title').textContent).toBe(panels[1].title);
  });

  it('should go to the panel we select from the breadcrumbs', () => {
    component();
    const panelNum = 2;
    fireEvent.click(screen.getByTestId(`breadcrumb${panelNum}`));
    expect(screen.getByTestId('panel-title').textContent).toBe(
      panels[panelNum].title
    );
  });

  it('should go to the previous slide when we click previous', () => {
    component();
    expect(screen.getByTestId('panel-title').textContent).toBe(panels[0].title);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByTestId('panel-title').textContent).toBe(panels[1].title);
    fireEvent.click(screen.getByRole('button', { name: /previous/i }));
    expect(screen.getByTestId('panel-title').textContent).toBe(panels[0].title);
  });

  it('should not show the next button when we are on the last slide', async () => {
    component();
    const lastPanel = panels.length - 1;
    fireEvent.click(screen.getByTestId(`breadcrumb${lastPanel}`));
    expect(
      await screen.getByRole('button', { name: /close/i })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /next/i })
    ).not.toBeInTheDocument();
  });

  it('should expand and retract the image when the resize icon is clicked', () => {
    component();
    // You cannot expand the image on the first page of the quick tour
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.queryByTestId('full-image-wrapper')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('expand-icon'));
    expect(screen.getByTestId('full-image-wrapper')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('shrink-icon'));
    expect(screen.queryByTestId('full-image-wrapper')).not.toBeInTheDocument();
  });
});
