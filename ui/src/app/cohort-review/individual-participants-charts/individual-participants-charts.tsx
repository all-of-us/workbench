import {Component, Input, OnChanges, OnInit, ViewChild} from '@angular/core';
import * as moment from 'moment';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official'

export interface ChartReactProps {
  chartData: Array<any>;
  chartHeader: string;
  chartKey: number;
}

export class IndividualParticipantsReactCharts extends React.Component<ChartReactProps> {
   props: ChartReactProps;
  // duplicateItems = [];
  // trimmedData =[];
  // yAxisNames=[''];
  // chartOptions = {};
  //
  constructor(props) {
    super(props);

    // this.setYaxisValue = this.setYaxisValue.bind(this);
    // this.getChartsData = this.getChartsData.bind(this);
  }
  //
  // componentDidMount() {
  //   if (this.props.chartData) {
  //       this.setYaxisValue();
  //   }
  // }
  //
  //
  //
  // setYaxisValue() {
  //   let yAxisValue = 1;
  //   this.props.chartData.map(items => { // find standardName in duplicate items
  //     const duplicateFound = this.duplicateItems.find(
  //       findName =>
  //         findName.name === items.standardName
  //     );
  //
  //     // duplicate items found return true otherwise push the the item in duplicateItems array
  //     if (duplicateFound) {
  //       Object.assign(items, {
  //         yAxisValue: duplicateFound.yAxisValue,
  //         startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
  //       });
  //       return true;
  //     }
  //     this.duplicateItems.push({name: items.standardName, yAxisValue});
  //     Object.assign(items, {
  //       yAxisValue,
  //       startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
  //     });
  //     yAxisValue ++;
  //   });
  //
  //   this.props.chartData.map(i => {
  //     const temp = {
  //       x: i.startDate,
  //       y: i.yAxisValue,
  //       standardName: i.standardName,
  //       ageAtEvent: i.ageAtEvent,
  //       rank: i.rank,
  //       startDate: moment.unix(i.startDate).format('MM-DD-YYYY'),
  //       standardVocabulary: i.standardVocabulary,
  //     };
  //     this.trimmedData.push(temp);
  //   });
  //   this.duplicateItems.map(d => {
  //     this.yAxisNames.push(d.name.substring(0, 13));
  //   });
  //
  //   if (this.trimmedData.length) {
  //      this.getChartsData();
  //   }
  // }
  //
  // getChartsData(){
  //   const names = this.yAxisNames;
  //   this.chartOptions = {
  //     chart: {
  //       type: 'scatter',
  //       zoomType: 'xy',
  //     },
  //     credits: {
  //       enabled: false
  //     },
  //     title: {
  //       text: 'Top' + ' ' + this.props.chartHeader + ' ' + 'over Time',
  //     },
  //     xAxis: {
  //       title: {
  //         enabled: true,
  //         text: 'Entry Date',
  //       },
  //       labels: {
  //         formatter: function () {
  //           return moment.unix(this.value).format('YYYY');
  //         },
  //       },
  //       startOnTick: true,
  //       endOnTick: true,
  //       tickInterval: 40 * 3600 * 1000,
  //     },
  //     yAxis: [{
  //       title: {
  //         enabled: true,
  //         text: this.props.chartHeader,
  //       },
  //       labels: {
  //         formatter: function () {
  //           return names[this.value];
  //         }
  //       },
  //       tickInterval: 1,
  //       lineWidth: 1,
  //     },
  //       {
  //         title: {
  //           enabled: false,
  //         },
  //         opposite: true,
  //         lineWidth: 1,
  //       }],
  //     plotOptions: {
  //       scatter: {
  //         marker: {
  //           radius: 5,
  //           states: {
  //             hover: {
  //               enabled: true,
  //               lineColor: 'rgb(100,100,100)'
  //             }
  //           }
  //         },
  //         states: {
  //           hover: {
  //             marker: {
  //               enabled: false
  //             }
  //           }
  //         },
  //       }
  //     },
  //     tooltip: {
  //       pointFormat: '<div>' +
  //         '<b>Date:</b>{point.startDate}<br/>' +
  //         '<b>Standard Vocab:</b>{point.standardVocabulary}<br/>' +
  //         '<b>Standard Name:</b>{point.standardName}<br/>' +
  //         '<b>Age at Event:</b>{point.ageAtEvent}<br/>' +
  //         '</div>',
  //       style: {
  //         color: '#565656',
  //         fontSize: 12
  //       },
  //       shared: true
  //     },
  //     series: [{
  //       type: 'scatter',
  //       name: 'Details',
  //       data: this.trimmedData,
  //       turboThreshold: 5000,
  //       showInLegend: false,
  //     }],
  //   };
  // }
  //
  //
  // // getChartObj(chartObj: any) {
  // //   this.props.chart = chartObj;
  // //   // check that ResizeObserver is supported
  // //   if (this.props.chart && typeof ResizeObserver === 'function') {
  // //     // Unbind window.onresize handler so we don't do double redraws
  // //     if (this.props.chart.unbindReflow) {
  // //       this.props.chart.unbindReflow();
  // //     }
  // //     // create observer to redraw charts on div resize
  // //     const ro = new ResizeObserver(() => this.props.chart.reflow());
  // //     ro.observe(this.props.chartRef.element.nativeElement);
  // //   }
  // // }
  //
  //
  options = {
    title: {
      text: 'My chart'
    },
    series: [{
      data: [1, 2, 3]
    }]
  }

  render() {

    const ScatterChart = () => <div key={this.props.chartKey}>
  <HighchartsReact
    highcharts={highCharts}
    options={this.options}
  /></div>
return <ScatterChart/>

  }
}
@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsChartsComponent implements OnChanges {
  // @ViewChild('chartRef') chartRef;
  @Input() chartData = [];
  @Input() chartHeader: string;
  @Input() chartKey: number;
  // chart: any;
  componentId = 'react-chart'
  constructor() {}
  ngOnChanges(): void {
      ReactDOM.render(React.createElement(IndividualParticipantsReactCharts,
        {chartData: this.chartData, chartHeader: this.chartHeader, chartKey: this.chartKey}),
        document.getElementById(this.componentId));
  }


}


