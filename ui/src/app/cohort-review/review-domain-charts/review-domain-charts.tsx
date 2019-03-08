import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';
import * as moment from 'moment';

export class ReviewDomainChartsComponent extends React.Component<
  {orgData: any, unitName: any}, {}> {
  constructor(props) {
    super(props);
    // this.state = {
    //   data: this.props.orgData,
    // };
  }
  componentDidMount() {
    // console.log(this.props.orgData)
  }


  getOptions() {
     console.log(this.props.orgData)
    // const {chartData: {conditionTitle, items}} = this.props;
    // const names = ['', ...fp.uniq(items.map(item => item.standardName))];
    // const nameIndexes = fp.mapValues(n => +n, fp.invert(names));
    const values = this.props.orgData.map(val => {
      return  val.values
    });
    const date = this.props.orgData.map(val => {
      // return moment(val.date, 'YYYY-MM-DD').unix()
       return val.date
    });
     console.log(date);
    return {
      // chart: {
      //   zoomType: 'xy',
      // },
      // credits: {
      //   enabled: false
      // },
      title: {
        text: this.props.unitName
      },

      // xAxis: {
      //         title: {
      //           enabled: true,
      //           text: 'Entry Date',
      //         },
      //         labels: {
      //           formatter: function() {
      //             return moment.unix(this.value).format('YYYY');
      //           },
      //         },
      //         startOnTick: true,
      //         endOnTick: true,
      //         tickInterval: 40 * 3600 * 1000,
      //       },
      yAxis: {
        // title: {
        //   text: 'Number of Employees'
        // }
      },
      legend: {
        layout: 'vertical',
        align: 'right',
        verticalAlign: 'middle'
      },
      plotOptions: {
        series: {
          label: {
            connectorAllowed: false
          },
          // pointStart: 2010
        }
      },
      tooltip: {
        pointFormatter() {
          return `
            <b>Value: </b>${this.y}<br/>`;
        },
        style: {
          color: '#565656',
          fontSize: 12
        },
        shared: true
      },
      xAxis: {
        // title: {
        //   text: 'Number of Months'
        // },
        categories: date,
        // labels: {
        //   formatter: function() {
        //     return moment.unix(this.value).format('YYYY');
        //   },
        // },
        // tickInterval: 12 * 3600 * 1000,
      },
      series: [{
        name: 'Vitals',
        data: values,
        turboThreshold: 5000,
        showInLegend: false,
      }],
      responsive: {
        rules: [{
          condition: {
            maxWidth: 500
          },
          chartOptions: {
            legend: {
              layout: 'horizontal',
              align: 'center',
              verticalAlign: 'bottom'
            }
          }
        }]
      }
    };
  }


  render() {
    return (
      <div>
        <HighchartsReact
          highcharts={highCharts}
          options={this.getOptions()}
        />
      </div>
    );
  }
}
export default ReviewDomainChartsComponent;
