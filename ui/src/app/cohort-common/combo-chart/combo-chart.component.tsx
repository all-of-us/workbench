import {Component, Input} from '@angular/core';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';

interface Props {
  mode: string;
  data: any;
}

interface State {
  options: any;
}

export class ComboChart extends React.Component<Props, State> {
  constructor(props: Props) {
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
        categories: this.getCategories(),
        tickLength: 0,
        tickPixelInterval: 50
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
      colors: ['#adcded', '#aae3f5', '#a27ea8', '#7aa3e5', '#a8385d'],
      legend: {
        enabled: false
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
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
    const codeMap = {
      'M': 'Male',
      'F': 'Female',
      'No matching concept': 'Unknown'
    };
    return data.reduce((acc, datum) => {
      const gender = !!codeMap[datum.gender] ? codeMap[datum.gender] : datum.gender;
      const key = `${gender} ${datum.ageRange || 'Unknown'}`;
      if (!acc.includes(key)) {
        acc.push(key);
      }
      return acc;
    }, []).sort((a, b) => a > b ? 1 : -1);;
  }

  getSeries() {
    const {data} = this.props;
    const categories = this.getCategories();
    const size = categories.length;
    let i = 0;
    return data.reduce((acc, datum) => {
      while (i < size) {
        const index = acc.findIndex(d => d.name === datum.race);
        if (index === -1 && acc.length) {
          if (acc[acc.length - 1].data.length < categories.length) {
            const zeroes = categories.length - acc[acc.length - 1].data.length;
            for (let z = 0; z < zeroes; z++) {
              acc[acc.length - 1].data.push(0);
            }
            i = 0;
          }
        }
        const key = categories[i];
        const missing = !key.includes(datum.ageRange);
        if (!missing) {
          if (index === -1) {
            acc.push({name: datum.race, data: [datum.count]});
          } else {
            acc[index].data.push(datum.count);
          }
          i++;
          break;
        } else {
          if (index === -1) {
            acc.push({name: datum.race, data: [0]});
          } else {
            acc[index].data.push(0);
          }
          i++;
        }
      }
      if (i === size) {
        i = 0;
      }
      return acc;
    }, []).sort((a, b) => a['name'] < b['name'] ? 1 : -1);
  }

  render() {
    const {options} = this.state;
    return <div style={{minHeight: 200}}>
      {options && <HighchartsReact highcharts={highCharts} options={options} callback={getChartObj} />}
    </div>;
  }
}

@Component ({
  selector: 'app-combo-chart',
  template: '<div #root></div>'
})
export class ComboChartComponent extends ReactWrapperBase {
  @Input('mode') mode: Props['mode'];
  @Input('data') data: Props['data'];
  constructor() {
    super(ComboChart, ['mode', 'data']);
  }
}

