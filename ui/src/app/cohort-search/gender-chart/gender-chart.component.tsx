import {Component, Input} from '@angular/core';
import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

highCharts.setOptions({
  lang: {
    decimalPoint: '.',
    thousandsSep: ','
  },
});

interface Props {
  data: any;
}

interface State {
  options: any;
}

export class GenderChart extends React.Component<Props, State> {
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
        height: 200,
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
      legend: {
        enabled: false
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        }
      },
      series: [{
        data: this.getSeries(),
        tooltip: {
          pointFormat: '<span style="color:{point.color}">\u25CF </span><b> {point.y}</b>'
        }
      }]
    };
    this.setState({options});
  }

  getSeries() {
    const {data} = this.props;
    const genderCodes = {
      'M': 'Male',
      'F': 'Female',
      'No matching concept': 'Unknown'
    };
    const defaults = [
      {y: 0, name: 'Male', color: '#7aa3e5'},
      {y: 0, name: 'Female', color: '#a8385d'},
      {y: 0, name: 'Unknown', color: '#a27ea8'},
    ];
    return data.reduce((acc, datum) => {
      const index = acc.findIndex(d => d.name === genderCodes[datum.gender]);
      if (index > -1) {
        acc[index].y += datum.count;
      }
      return acc;
    }, defaults).sort((a, b) => a['name'] > b['name'] ? 1 : -1);
  }

  render() {
    const {options} = this.state;
    return <div style={{minHeight: 200}}>
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
