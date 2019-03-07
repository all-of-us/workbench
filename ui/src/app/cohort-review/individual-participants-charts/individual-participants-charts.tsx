import {Component, Input} from '@angular/core';
import {ParticipantChartData} from 'generated/fetch';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';

import {ReactWrapperBase} from 'app/utils/index';

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

  getChartObj(chartObj: any) {
    this.chart = chartObj;
    const chartRef = this.chart.container.parentElement;
    if (this.chart && typeof ResizeObserver === 'function') {
      // Unbind window.onresize handler so we don't do double redraws
      if (this.chart.unbindReflow) {
        this.chart.unbindReflow();
      }
      // create observer to redraw charts on div resize
      const ro = new ResizeObserver(() => {
        if (this.chart) {
          this.chart.reflow();
        }
      });
      ro.observe(chartRef);
    }
  }

  render() {
    const {chartData: {loading, items}} = this.props;
    if (!loading && items.length) {
      return <HighchartsReact
        highcharts={highCharts}
        options={this.getOptions()}
        callback={this.getChartObj}
      />;
    }
    return null;
  }
}

@Component({
  selector: 'app-individual-participants-charts',
  template: '<div #root></div>',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsChartsComponent extends ReactWrapperBase {
  @Input() chartData;

  constructor() {
    super(IndividualParticipantsCharts, ['chartData']);
  }
}
