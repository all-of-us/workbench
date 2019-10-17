import {getChartObj} from 'app/cohort-search/utils';
import {ParticipantChartData} from 'generated/fetch';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';


export class IndividualParticipantsCharts extends React.Component<{
  chartData: {
    loading: boolean,
    conditionTitle: string,
    items: ParticipantChartData[]
  }
}> {
  chart: any;

  getOptions() {
    const {chartData: {conditionTitle, items}} = this.props;
    const names = ['', ...fp.uniq(items.map(item => item.standardName))];
    const nameIndexes = fp.mapValues(n => +n, fp.invert(names));
    const data = items.map(({startDate, standardName, standardVocabulary, ageAtEvent}) => {
      return {
        x: moment(startDate, 'YYYY-MM-DD').unix(),
        y: nameIndexes[standardName],
        ageAtEvent,
        standardName,
        standardVocabulary,
      };
    });
    return {
      chart: {
        type: 'scatter',
        zoomType: 'xy',
      },
      credits: {
        enabled: false
      },
      title: {
        text: `Top ${conditionTitle} over Time`,
      },
      xAxis: {
        title: {
          enabled: true,
          text: 'Entry Date',
        },
        labels: {
          formatter: function() {
            return moment.unix(this.value).format('YYYY');
          },
        },
        startOnTick: true,
        endOnTick: true,
        tickInterval: 40 * 3600 * 1000,
      },
      yAxis: [{
        title: {
          enabled: true,
          text: conditionTitle,
        },
        labels: {
          formatter: function() {
            return names[this.value] && names[this.value].substring(0, 13);
          }
        },
        tickInterval: 1,
        lineWidth: 1,
      },
      {
        title: {
          enabled: false,
        },
        opposite: true,
        lineWidth: 1,
      }],
      plotOptions: {
        scatter: {
          marker: {
            radius: 5,
            states: {
              hover: {
                enabled: true,
                lineColor: 'rgb(100,100,100)'
              }
            }
          },
          states: {
            hover: {
              marker: {
                enabled: false
              }
            }
          },
        }
      },
      tooltip: {
        pointFormatter() {
          return `<div><b>Date: </b>${moment.unix(this.x).format('MM-DD-YYYY')}<br/>
            <b>Standard Vocab: </b>${this.standardVocabulary}<br/>
            <b>Standard Name: </b>${this.standardName}<br/>
            <b>Age at Event: </b>${this.ageAtEvent}<br/></div>`;
        },
        style: {
          color: '#565656',
          fontSize: 12
        },
        shared: true
      },
      series: [{
        type: 'scatter',
        name: 'Details',
        data,
        turboThreshold: 5000,
        showInLegend: false,
      }],
    };
  }


  render() {
    const {chartData: {loading, items}} = this.props;
    if (!loading && items.length) {
      return <HighchartsReact
        highcharts={highCharts}
        options={this.getOptions()}
        callback={getChartObj}
      />;
    }
    return null;
  }
}
