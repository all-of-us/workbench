import {getChartObj} from 'app/cohort-search/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';


export class ReviewDomainChartsComponent extends React.Component<
  {unitData: any}> {
  chart: any;
  constructor(props) {
    super(props);
  }

  getOptions() {
    const values = this.props.unitData.map(val => {
      return  val.values;
    });
    const date = this.props.unitData.map(val => {
      return val.date;
    });

    return {
      chart: {
        type: 'spline',
        zoomType: 'xy',
        backgroundColor: 'transparent',
        height: 300,
      },
      credits: {
        enabled: false
      },
      title: {
        text: ''
      },
      yAxis: {
        lineWidth: 2,
        lineColor: '#979797',
      },
      plotOptions: {
        series: {
          label: {
            connectorAllowed: false
          },
        }
      },
      tooltip: {
        headerFormat: '',
        pointFormatter() {
          return `
            <b>Value: </b>${this.y}<br/>
             <b>Date: </b>${this.category}<br/>`;
        },
        style: {
          color: '#565656',
          fontSize: 12
        },
        shared: true
      },
      xAxis: {
        categories: date,
        lineWidth: 2,
        lineColor: '#979797',
      },
      series: [{
        data: values,
        turboThreshold: 5000,
        showInLegend: false,
      }],
    };
  }

  render() {
    return (
      <div>
        <HighchartsReact
          highcharts={highCharts}
          options={this.getOptions()}
          callback={getChartObj}
        />
      </div>
    );
  }
}
export default ReviewDomainChartsComponent;
