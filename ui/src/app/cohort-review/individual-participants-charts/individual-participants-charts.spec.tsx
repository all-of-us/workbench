import { shallow } from 'enzyme';
import * as React from 'react';
import {ChartReactProps, IndividualParticipantsChartsComponent, IndividualParticipantsReactCharts} from '../individual-participants-charts/individual-participants-charts';

describe('IndividualParticipantsChartsComponent', () => {

  let props: ChartReactProps;
  const component = () => {
    return shallow<IndividualParticipantsReactCharts, ChartReactProps>
    (<IndividualParticipantsReactCharts {...props}/>);
  };

  beforeEach(() => {
    props = {
      chartData: {
        loading: true,
    conditionTitle: '',
    items: [],
      },
      chartKey: 0,
    };
  });

  it('should render IndividualParticipantsChartsComponent', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});

