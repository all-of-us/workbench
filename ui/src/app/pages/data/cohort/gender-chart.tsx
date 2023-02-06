import * as React from 'react';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import { DemoChartInfo } from 'generated/fetch';

import { getChartObj } from 'app/pages/data/cohort/utils';
import colors from 'app/styles/colors';

highCharts.setOptions({
  lang: {
    decimalPoint: '.',
    thousandsSep: ',',
  },
});

interface Props {
  data: DemoChartInfo[];
}

interface State {
  options: any;
}

export class GenderChart extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { options: null };
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  getChartOptions() {
    const { categories, data } = this.getCategoriesAndData();
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
        min: 0,
        title: {
          text: '# Participants',
        },
      },
      legend: {
        enabled: false,
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
      },
      series: [
        {
          data,
          tooltip: {
            pointFormat:
              '<span style="color:{point.color}">\u25CF </span><b> {point.y}</b>',
          },
        },
      ],
    };
    this.setState({ options });
  }

  getCategoriesAndData() {
    const { data } = this.props;
    const chartData = data.reduce(
      (acc, { count, name }) => {
        if (!acc.categories.includes(name)) {
          acc.categories.push(name);
        }
        const index = acc.data.findIndex((d) => d.name === name);
        if (index > -1) {
          acc.data[index].y += count;
        } else {
          acc.data.push({
            y: count,
            name,
            color: colors.chartColors[acc.data.length],
          });
        }
        return acc;
      },
      { categories: [], data: [] }
    );
    return {
      categories: chartData.categories.sort((a, b) => (a > b ? 1 : -1)),
      data: chartData.data.sort((a, b) => (a.name > b.name ? 1 : -1)),
    };
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
