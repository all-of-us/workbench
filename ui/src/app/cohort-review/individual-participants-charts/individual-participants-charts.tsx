import {Component, DoCheck, Input, OnChanges, OnInit, ViewChild} from '@angular/core';
import {ParticipantChartData} from 'generated';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as moment from 'moment';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

interface DisplayParticipantChartData extends ParticipantChartData {
  yAxisValue?: number;
  newStartDate?: number;
}

interface ChartData {
  loading: boolean;
  conditionTitle: string;
  items: Array<DisplayParticipantChartData>;
}

export interface ChartReactProps {
  chartData: ChartData;
  chartKey: number;
}

export class IndividualParticipantsReactCharts extends React.Component<ChartReactProps> {
  props: ChartReactProps;
  chartOptions = {};
  trimmedData = [];
  duplicateItems = [];
  yAxisNames = [''];
  chart: any;

  setYaxisValue() {
    let yAxisValue = 1;
    this.props.chartData.items.reverse().map(items => { // find standardName in duplicate items
      const duplicateFound = this.duplicateItems.find(
        findName =>
          findName.name === items.standardName
      );

      // duplicate items found return true otherwise push the the item in duplicateItems array
      if (duplicateFound) {
        Object.assign(items, {
          yAxisValue: duplicateFound.yAxisValue,
          // format date to unix timestamp
          newStartDate: moment(items.startDate, 'YYYY-MM-DD').unix()
        });
        return true;
      }
      this.duplicateItems.push({name: items.standardName, yAxisValue});
      Object.assign(items, {
        yAxisValue,
        newStartDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
      });
      yAxisValue ++;
    });

    this.props.chartData.items.map(i => {
      const temp = {
        x: i.newStartDate,
        y: i.yAxisValue,
        ageAtEvent: i.ageAtEvent,
        standardName: i.standardName,
        rank: i.rank,
        startDate: moment.unix(i.newStartDate).format('MM-DD-YYYY'),
        standardVocabulary: i.standardVocabulary,
      };
      this.trimmedData.push(temp);
      this.trimmedData.reverse();
    });
    this.duplicateItems.map(d => {
      this.yAxisNames.push(d.name.substring(0, 13));
    });

    if (this.trimmedData.length) {
       this.getChartsData();
    }
  }

  getChartsData() {
    const names = this.yAxisNames;
    const header = this.props.chartData.conditionTitle;

    this.chartOptions = {
      chart: {
        type: 'scatter',
        zoomType: 'xy',
      },
      credits: {
        enabled: false
      },
      title: {
        text: 'Top' + ' ' + header + ' ' + 'over Time',
      },
      xAxis: {
        title: {
          enabled: true,
          text: 'Entry Date',
        },
        labels: {
          formatter: function () {
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
          text: header,
        },
        labels: {
          formatter: function () {
            return names[this.value];
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

        pointFormat: '<div>' +
          '<b>Date:</b>{point.startDate}<br/>' +
          '<b>Standard Vocab:</b>{point.standardVocabulary}<br/>' +
          '<b>Standard Name:</b>{point.standardName}<br/>' +
          '<b>Age at Event:</b>{point.ageAtEvent}<br/>' +
          '</div>',
        style: {
          color: '#565656',
          fontSize: 12
        },
        shared: true
      },
      series: [{
        type: 'scatter',
        name: 'Details',
        data: this.trimmedData,
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
     const chartData = this.props.chartData;
    if (!chartData.loading && chartData) {
      this.setYaxisValue();
      const ScatterChart = () => <div>
        <HighchartsReact
          highcharts={highCharts}
          options={this.chartOptions}
          callback={this.getChartObj}
        />
      </div>;
      return <ScatterChart/>;
    }
    return <div/>;
  }
}

@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsChartsComponent implements OnChanges, OnInit {
  @ViewChild('chartRef') chartRef;
  @Input() chartData: ChartData;
  @Input() chartKey = 0;
  @Input() shouldReRender: boolean;
  chart: any;
  componentClass = 'react-chart';
  constructor() {}

  ngOnChanges (): void {
    this.triggerReRender();
  }

  ngOnInit (): void {
    this.triggerReRender();
  }

  triggerReRender(): void {
    ReactDOM.render(React.createElement(IndividualParticipantsReactCharts,
      {chartData: this.chartData,
        chartKey: this.chartKey,
      }),
      document.getElementsByClassName(this.componentClass)[this.chartKey]);
  }

}


