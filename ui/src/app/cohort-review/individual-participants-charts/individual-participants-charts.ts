import {Component, Input, OnChanges, OnInit} from '@angular/core';
import * as moment from 'moment';
@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsChartsComponent implements OnInit, OnChanges {
  chartOptions = {};
  @Input() chartData;
  @Input() chartHeader: string;
  private _chart: any;
  trimmedData = [];
  duplicateItems = [];
  yAxisNames = [''];
  constructor(
  ) {}
  ngOnChanges() {
    this.yAxisNames = [''];
    if (this.chartData) {
      this.setYaxisValue();
    }
  }
  ngOnInit() {
    this.trimmedData = [];
    this.chartOptions = {};
    this.duplicateItems = [];
  }

  setYaxisValue() {
    this.yAxisNames = [''];
    let yAxisValue = 1;
    this.chartData.reverse().map(items => { // find standardName in duplicate items
     const duplicateFound = this.duplicateItems.find(
       findName =>
         findName.name === items.standardName
       );
      // duplicate items found return true otherwise push the the item in duplicateItems array
      if (duplicateFound) {
        Object.assign(items, {
          yAxisValue: duplicateFound.yAxisValue,
          startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
        });
        return true;
      }
      this.duplicateItems.push({name: items.standardName, yAxisValue});
      Object.assign(items, {
        yAxisValue,
        startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
      });
      yAxisValue ++;
    });
    this.chartData.map(i => {
      const temp = {
        x: i.startDate,
        y: i.yAxisValue,
        standardName: i.standardName,
        ageAtEvent: i.ageAtEvent,
        rank: i.rank,
        startDate: moment.unix(i.startDate).format('MM-DD-YYYY'),
        standardVocabulary: i.standardVocabulary,
      };
      this.trimmedData.push(temp);
      this.trimmedData.reverse();
    });
    this.duplicateItems.map(d => {
      this.yAxisNames.push(d.name.substring(0, 13));
    });

    if (this.trimmedData.length) {
      this.getchartsData();
    }
  }


  getchartsData() {
    const test = this.yAxisNames;
    this.chartOptions = {
      chart: {
        type: 'scatter',
        zoomType: 'xy',
        width: 420
      },
      credits: {
        enabled: false
      },
      title: {
        text: 'Top' + ' ' + this.chartHeader + ' ' + 'over Time'
      },
      xAxis: {
        title: {
          enabled: true,
          text: 'Entry Date'
        },

        labels: {
          formatter: function () {
            return moment.unix(this.value).format('YYYY');
          }
        },
        startOnTick: true,
        endOnTick: true,
        // max: 2020,
        // min:2004,
        // tickPositions: [],
        // showLastLabel: true,
        // tickInterval: 1,
      },
      yAxis: [{
        title: {
          enabled: true,
          text: this.chartHeader,
        },
        labels: {
          formatter: function () {
            return test[this.value];
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
          'Date:<b>{point.startDate}</b><br/>' +
          'Standard Vocab:<b>{point.standardVocabulary}</b><br/>' +
          'Standard Name: <b>{point.standardName}</b><br/>' +
          'Age at Event:<b>{point.ageAtEvent}</b><br/>' +
          'rank:<b>{point.rank}</b><br/>' +
          '</div>',
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

  }

