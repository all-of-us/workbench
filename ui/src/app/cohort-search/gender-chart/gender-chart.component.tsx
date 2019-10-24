import {Component, Input} from '@angular/core';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';

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
  categories: Array<string>;
  options: any;
}

export class GenderChart extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.state = {
      options: null,
      categories: parseInt(cdrVersionId, 10) < 2 // TODO change 2 to 3 before merging
        ? ['Female', 'Male', 'Unknown']
        : ['Female', 'Male', 'Not man only, not woman only, prefer not to answer, or skipped', 'Unknown']
    };
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  getChartOptions() {
    const {categories} = this.state;
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
        categories,
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
    const {cdrVersionId} = currentWorkspaceStore.getValue();
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
    // TODO change 2 to 3 in the below condition that checks cdrVersionId before merging
    if (+cdrVersionId >= 2) {
      defaults.push({y: 0, name: 'Not man only, not woman only, prefer not to answer, or skipped', color: '#aae3f5'});
    }
    return data.reduce((acc, datum) => {
      // TODO change 2 to 3 in the below condition that checks cdrVersionId before merging
      const gender = +cdrVersionId < 2 ? genderCodes[datum.gender] : datum.gender;
      const index = acc.findIndex(d => d.name === gender);
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
