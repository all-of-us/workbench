import {shallow} from 'enzyme';
import * as React from 'react';

import {IndividualParticipantsCharts} from './individual-participants-charts';

describe('IndividualParticipantsChartsComponent', () => {
  it('should render IndividualParticipantsChartsComponent', () => {
    const wrapper = shallow(<IndividualParticipantsCharts
      chartData={{
        loading: false,
        conditionTitle: '',
        items: [{
          startDate: '2019-01-01',
          standardName: 'a',
          standardVocabulary: 'b',
          ageAtEvent: 1,
          rank: 1
        }]
      }}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
