import * as React from 'react';
import * as highCharts from "highcharts";
import HighchartsReact from 'highcharts-react-official';
import * as fp from "lodash/fp";
import * as moment from "moment";

export class ReviewDomainChartsComponent extends React.Component<{orgData: Array<any>}, {data: Array<any> }> {
  constructor(props) {
    super(props);
    this.state = {
      data: this.props.orgData,
    };
  }

  getOptions() {
    // const {chartData: {conditionTitle, items}} = this.props;
    // const names = ['', ...fp.uniq(items.map(item => item.standardName))];
    // const nameIndexes = fp.mapValues(n => +n, fp.invert(names));
    // const data = items.map(({startDate, standardName, standardVocabulary, ageAtEvent}) => {
    //   return {
    //     x: moment(startDate, 'YYYY-MM-DD').unix(),
    //     y: nameIndexes[standardName],
    //     ageAtEvent,
    //     standardName,
    //     standardVocabulary,
    //   };
    // });
    return {
      chart: {
        zoomType: 'xy',
      },
      // credits: {
      //   enabled: false
      // },
      title: {
        text: 'Solar Employment Growth by Sector, 2010-2016'
      },
      // xAxis: {
      //       //   title: {
      //       //     enabled: true,
      //       //     text: 'Entry Date',
      //       //   },
      //       //   // labels: {
      //       //   //   formatter: function() {
      //       //   //     return moment.unix(this.value).format('YYYY');
      //       //   //   },
      //       //   // },
      //       //   startOnTick: true,
      //       //   endOnTick: true,
      //       //   tickInterval: 40 * 3600 * 1000,
      //       // },
      yAxis: {
        title: {
          text: 'Number of Employees'
        }
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
          pointStart: 2010
        }
      },
      // tooltip: {
      //   pointFormatter() {
      //     return `<div><b>Date: </b>${moment.unix(this.x).format('MM-DD-YYYY')}<br/>
      //       <b>Standard Vocab: </b>${this.standardVocabulary}<br/>
      //       <b>Standard Name: </b>${this.standardName}<br/>
      //       <b>Age at Event: </b>${this.ageAtEvent}<br/></div>`;
      //   },
      //   style: {
      //     color: '#565656',
      //     fontSize: 12
      //   },
      //   shared: true
      // },
      series: [{
        name: 'Installation',
        data: [43934, 52503, 57177, 69658, 97031, 119931, 137133, 154175]
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
    // console.log(this.state.data);
    return (
      <div>
        <HighchartsReact
          highcharts={highCharts}
          options={this.getOptions()}
        />;
      </div>
    );
  }
}
export default ReviewDomainChartsComponent;
