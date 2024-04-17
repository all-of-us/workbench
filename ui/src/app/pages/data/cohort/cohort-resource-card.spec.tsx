import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';
import { CohortResourceCard } from 'app/pages/data/cohort/cohort-resource-card';

import { renderWithRouter } from 'testing/react-test-helpers';

describe('CohortResourceCard', () => {
  const component = () => {
    const props = {
      cohort: {},
    };

    return renderWithRouter(
      <CohortResourceCard resource={props} onUpdate={() => {}} />
    );
  };

  it('should render', () => {
    component();
    expect(screen.getByText(/cohort/i)).toBeInTheDocument();
  });
});
