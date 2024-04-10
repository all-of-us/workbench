import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';

import { IndividualParticipantsCharts } from './individual-participants-charts';

describe('IndividualParticipantsChartsComponent', () => {
  const testConditionTitle = 'Test Condition Title';
  it('should render IndividualParticipantsChartsComponent', () => {
    render(
      <IndividualParticipantsCharts
        chartData={{
          loading: false,
          conditionTitle: testConditionTitle,
          items: [
            {
              startDate: '2019-01-01',
              standardName: 'a',
              standardVocabulary: 'b',
              ageAtEvent: 1,
              rank: 1,
            },
          ],
        }}
      />
    );
    expect(screen.getByText(testConditionTitle)).toBeInTheDocument();
  });
});
