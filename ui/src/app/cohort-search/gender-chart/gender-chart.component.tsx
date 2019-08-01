import {Component, Input} from '@angular/core';
import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as fp from 'lodash/fp';
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
  readonly codeMap = {
    'M': 'Male',
    'F': 'Female',
    'No matching concept': 'Unknown'
  };

  readonly defaults = {
    Male: {y: 0, name: 'Male', color: '#7aa3e5'},
    Female: {y: 0, name: 'Female', color: '#a8385d'},
    Unknown: {y: 0, name: 'Unknown', color: '#a27ea8'},
  };

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
    // TODO remove toJS() and commented code once the combo chart has been updated for vanilla js
    // TODO check that adding ES2017 to tsconfig is okay or find a workaround for Object.values
    return Object.values(data.toJS().map(datum => {
      datum.gender = this.codeMap[datum.gender];
      return datum;
    }).reduce((acc, it) => {
      acc[it.gender].y += it.count;
      return acc;
    }, this.defaults)).sort((a, b) => a['name'] > b['name'] ? 1 : -1);
    // return data
    //   .map(datum => datum.update('gender', code => this.codeMap[code]))
    //   .groupBy(datum => datum.get('gender', 'Unknown'))
    //   .map((group, gender) => ({
    //     y: group.reduce((acc, item) => acc + item.get('count'), 0),
    //     name: gender,
    //     color: this.defaults[gender].color
    //   }))
    //   .mergeWith((old, _) => old, this.defaults)
    //   .sort((a, b) => a.name > b.name ? 1 : -1)
    //   .valueSeq()
    //   .toArray();
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
