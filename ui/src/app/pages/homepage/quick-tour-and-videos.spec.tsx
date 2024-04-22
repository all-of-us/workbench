import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { QuickTourAndVideos } from './quick-tour-and-videos';

describe('Quick Tour and Videos', () => {
  const component = (initialQuickTour) => {
    return render(
      <QuickTourAndVideos showQuickTourInitially={initialQuickTour} />
    );
  };

  it('should auto-display quick tour when requested', () => {
    component(true);
    expect(screen.getByTestId('quick-tour-react')).toBeInTheDocument();
  });

  it('should not auto-display quick tour when not requested', () => {
    component(false);
    expect(screen.queryByTestId('quick-tour-react')).not.toBeInTheDocument();
  });

  it('should display quick tour when clicked', () => {
    component(false);
    screen.getByAltText('show quick tour').click();
    expect(screen.getByTestId('quick-tour-react')).toBeInTheDocument();
  });

  // Note: The scroll tests might need to be adjusted as RTL doesn't directly support testing of UI scrolling behavior.
});
