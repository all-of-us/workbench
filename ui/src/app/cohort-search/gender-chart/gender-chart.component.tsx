import {Component, Input} from '@angular/core';
import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

interface Props {
  data: any;
}

interface State {
  options: any;
}

export class GenderChart extends React.Component<Props, State> {
  readonly codeMap = {
    'M': 'Male',
    'F': 'Female',
    'No matching concept': 'Unknown'
  };
  readonly defaults = {
    Male: 0,
    Female: 0,
    Unknown: 0
  };
  readonly axis = {
    x: {
      show: true,
      label: '# Participants',
      showLabel: true,
    },
    y: {
      show: true,
      label: 'Gender',
      showLabel: true,
    }
  };
  readonly colors = {
    Male: '#a8385d',
    Female: '#7aa3e5',
    Unknown: '#a27ea8'
  }

  constructor(props: Props) {
    super(props);
    this.state = {options: null};
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  getChartOptions() {
    const options = {
      chart: {
        height: 250,
        type: 'bar'
      },
      credits: {
        enabled: false
      },
      title: {
        text: ''
      },
      xAxis: {
        categories: ['Female', 'Male', 'Unknown'],
        tickLength: 0,
        tickPixelInterval: 50,
        title: {
          text: 'Gender'
        }
      },
      yAxis: {
        min: 0,
        title: {
          text: '# Participants'
        }
      },
      colors: ['#a8385d', '#7aa3e5', '#a27ea8'],
      legend: {
        enabled: false
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
        series: {
          stacking: 'normal'
        }
      },
      series: this.getSeries()
    };
    this.setState({options});
  }

  getSeries() {
    const {data} = this.props;
    const series = data
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => datum.get('gender', 'Unknown'))
      .map((group, gender) => ({
        name: gender,
        data: [{
          y: group.reduce((acc, item) => acc + item.get('count'), 0),
          name: gender,
          color: this.colors[gender]
        }]
      }))
      .sort((a, b) => a.name < b.name ? 1 : -1)
      .toJS();
    console.log(series);
    const test = Object.keys(series).map(key => series[key]);
    console.log(test);
    return test;
  }

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

@Component({
  selector: 'app-gender-chart',
  template: '<div #root></div>'
})
export class GenderChartComponent extends ReactWrapperBase {
  @Input('data') data: Props['data'];

  constructor() {
    super(GenderChart, ['data']);
  }
}
