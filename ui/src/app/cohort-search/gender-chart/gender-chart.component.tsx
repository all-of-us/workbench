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
      categories: +cdrVersionId < 3
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
    const genderCodes = {
      'M': 'Male',
      'F': 'Female',
      'No matching concept': 'Unknown'
    };
    const colors = ['#7aa3e5', '#a8385d', '#a27ea8', '#aae3f5'];
    return data.reduce((acc, datum) => {
      const gender = !!genderCodes[datum.gender] ? genderCodes[datum.gender] : datum.gender;
      const index = acc.findIndex(d => d.name === gender);
      if (index > -1) {
        acc[index].y += datum.count;
      } else {
        acc.push({y: datum.count, name: gender, color: colors[acc.length]});
      }
      return acc;
    }, []).filter(it => it.y > 0).sort((a, b) => a['name'] > b['name'] ? 1 : -1);
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
