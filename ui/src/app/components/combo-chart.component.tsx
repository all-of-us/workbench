import * as React from 'react';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import { DemoChartInfo } from 'generated/fetch';

import { getChartObj } from 'app/pages/data/cohort/utils';
import colors from 'app/styles/colors';

interface Props {
  data: DemoChartInfo[];
  legendTitle: string;
  mode: string;
}

interface State {
  options: any;
}

export class ComboChart extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { options: null };
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  componentDidUpdate(prevProps: any): void {
    if (
      prevProps.mode !== this.props.mode ||
      prevProps.data !== this.props.data
    ) {
      this.getChartOptions();
    }
  }

  getChartOptions() {
    const { legendTitle, mode } = this.props;
    const normalized = mode === 'normalized';
    const { categories, series } = this.getCategoriesAndSeries();
    const height = Math.max(categories.length * 30, 200);
    const options = {
      chart: {
        height,
        type: 'bar',
      },
      credits: {
        enabled: false,
      },
      title: {
        text: '',
      },
      xAxis: {
        categories,
        tickLength: 0,
        tickPixelInterval: 50,
      },
      yAxis: {
        labels: {
          format: '{value}' + (normalized ? '%' : ''),
        },
        min: 0,
        title: {
          text: '',
        },
      },
      colors: colors.chartColors,
      legend: {
        title: {
          style: { fontWeight: 'normal' },
          text: legendTitle,
        },
        reversed: true,
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
        series: {
          stacking: normalized ? 'percent' : 'normal',
        },
      },
      series,
    };
    this.setState({ options });
  }

  getCategoriesAndSeries() {
    const { data } = this.props;
    const categories = data
      .reduce((acc, { name }) => {
        if (!acc.includes(name)) {
          acc.push(name);
        }
        return acc;
      }, [])
      .sort((a, b) => (a > b ? 1 : -1));
    const series = data
      .reduce((acc, { ageRange, count, name }) => {
        const obj = { x: categories.indexOf(name), y: count };
        const index = acc.findIndex((d) => d.name === ageRange);
        if (index === -1) {
          acc.push({ name: ageRange, data: [obj] });
        } else {
          acc[index].data.push(obj);
        }
        return acc;
      }, [])
      .sort((a, b) => (a.name < b.name ? 1 : -1));
    return { categories, series };
  }

  render() {
    const { options } = this.state;
    return (
      <div style={{ minHeight: 200 }}>
        {options && (
          <HighchartsReact
            highcharts={highCharts}
            options={options}
            callback={getChartObj}
          />
        )}
      </div>
    );
  }
}
