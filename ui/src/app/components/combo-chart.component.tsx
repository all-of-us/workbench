import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

import {getChartObj} from 'app/cohort-search/utils';
import colors from 'app/styles/colors';

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
    const {categories, series} = this.getCategoriesAndSeries();
    const height = Math.max(categories.length * 30, 200);
    const options = {
      chart: {
        height,
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
      colors: colors.chartColors,
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
      series
    };
    this.setState({options});
  }

  getCategoriesAndSeries() {
    const {data} = this.props;
    const codeMap = {
      'M': 'Male',
      'F': 'Female',
      'No matching concept': 'Unknown'
    };
    const getKey = (dat) => {
      const gender = !!codeMap[dat.name] ? codeMap[dat.name] : dat.name;
      return `${gender} ${dat.ageRange || 'Unknown'}`;
    };
    const categories = data.reduce((acc, datum) => {
      const key = getKey(datum);
      if (!acc.includes(key)) {
        acc.push(key);
      }
      return acc;
    }, []).sort((a, b) => a > b ? 1 : -1);
    const series = data.reduce((acc, datum) => {
      const key = getKey(datum);
      const obj = {x: categories.indexOf(key), y: datum.count};
      const index = acc.findIndex(d => d.name === datum.race);
      if (index === -1) {
        acc.push({name: datum.race, data: [obj]});
      } else {
        acc[index].data.push(obj);
      }
      return acc;
    }, []).sort((a, b) => a.name < b.name ? 1 : -1);
    return {categories, series};
  }

  render() {
    const {options} = this.state;
    return <div style={{minHeight: 200}}>
      {options && <HighchartsReact highcharts={highCharts} options={options} callback={getChartObj} />}
    </div>;
  }
}

