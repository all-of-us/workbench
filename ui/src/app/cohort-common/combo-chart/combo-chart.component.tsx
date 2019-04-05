import {Component, Input} from '@angular/core';
import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

export interface ComboChartProps {
  mode: string;
  data: any;
}

export class ComboChart extends React.Component<ComboChartProps, {options: any}>  {
  /*
   * TODO - this maps gender codes to human readable representations.  We
   * probably need to either grab that repro from the DB or generate a complete
   * list of possible values and include mappings for each one, not just the
   * binary basics.
   */
  readonly codeMap = {
    'M': 'Male',
    'F': 'Female',
    'No matching concept': 'Unknown'
  };

  constructor(props: ComboChartProps) {
    super(props);
    this.state = {options: null};
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  componentDidUpdate(prevProps: any): void {
    if (prevProps.mode !== this.props.mode) {
      this.getChartOptions();
    }
  }

  getChartOptions() {
    const {mode} = this.props;
    const normalized = mode === 'normalized';
    const options = {
      chart: {
        type: 'bar'
      },
      title: {
        text: ''
      },
      xAxis: {
        categories: this.getCategories()
      },
      yAxis: {
        labels: {
          format: '{value}' + (normalized ? '%' : '')
        },
        min: 0,
        title: {
          text: ''
        }
      },
      colors: ['#a8385d', '#7aa3e5', '#a27ea8', '#aae3f5', '#adcded'],
      legend: {
        enabled: false
      },
      plotOptions: {
        series: {
          stacking: (normalized ? 'percent' : 'normal')
        }
      },
      series: this.getSeries()
    };
    this.setState({options});
  }

  getCategories() {
    const {data} = this.props;
    const categories = data
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => `${datum.get('gender', 'Unknown')} ${datum.get('ageRange', 'Unknown')}`)
      .toJS();
    return Object.keys(categories);
  }

  getSeries() {
    const {data} = this.props;
    const series = data
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => datum.get('race', 'Unknown'))
      .map((group, race) => ({name: race, data: group.map(item => item.get('count'))}))
      .sort((a, b) => a.name < b.name ? 1 : -1)
      .toJS();
    console.log(series);
    return Object.values(series);
  }

  /**
   * Returns the minimum height of the container as a number representing
   * pixels.  The min height is calculated as 100 * the number of bars with a
   * positive value or else just 100.
   */
  // get minHeight() {
  //   return Math.max(this.data.length * 40, 200);
  // }

  render() {
    const {options} = this.state;
    return <div className='chart-container' style={{minHeight: 200}}>
      {options && <HighchartsReact
        highcharts={highCharts}
        options={options}
        callback={getChartObj}
      />}
    </div>;
  }
}

@Component ({
  selector: 'app-combo-chart',
  template: '<div #root></div>'
})
export class ComboChartComponent extends ReactWrapperBase {
  @Input('mode') mode: ComboChartProps['mode'];
  @Input('data') data: ComboChartProps['data'];
  constructor() {
    super(ComboChart, ['mode', 'data']);
  }
}

