import {Component, Input, OnInit, ElementRef, ViewChild, OnChanges} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStatus} from 'generated';
import {ReviewStateService} from '../review-state.service';
import {Set} from "immutable";
import {CohortSearchActions, CohortSearchState, getParticipantData} from "../../cohort-search/redux";
import {NgRedux} from "@angular-redux/store";
// declare var jQuery:any;
const Highcharts = require('highcharts/highcharts.src');
import 'highcharts/adapters/standalone-framework.src';
import {DomainType} from "../../../generated";
import * as moment from 'moment';
@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsCharts implements OnInit, OnChanges{
  chartOptions = {};
  @Input() chartData;
  private _chart: any;
  trimmedData = [];
  duplicateItems = [];
  yAxisNames = [''];
  constructor(
  ) {}
  ngOnChanges(){
    this.yAxisNames = [''];
    if (this.chartData) {
      this.setYaxisValue();
    }
  }
  ngOnInit() {
    this.trimmedData = [];
    this.chartOptions = {};
    this.duplicateItems = [];
    // this.yAxisNames = [''];


  }

  setYaxisValue() {
    console.log(this.chartData)
    this.yAxisNames = [''];
    let yAxisValue = 1
    this.chartData.map(items=> { // find standardName in duplicate items
     const duplicateFound = this.duplicateItems.find(findName => findName.name === items.standardName);
      // duplicate items found return true otherwise push the the item in duplicateItems array
      if (duplicateFound) {
        Object.assign(items,{
          yAxisValue: duplicateFound.yAxisValue,
          startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
        });
        return true;
      }
      this.duplicateItems.push({name:items.standardName, yAxisValue});
      Object.assign(items,{
        yAxisValue,
        startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
      });
      yAxisValue ++;
    });

    this.chartData.map(i => {
      const temp = {
        x: i.startDate,
        y: i.yAxisValue,
        standardName: i.standardName
      }
      this.trimmedData.push(temp)
    })
    this.duplicateItems.map(d => {
      this.yAxisNames.push(d.name.substring(0, 13));
    });

    if(this.trimmedData.length){
      this.getchartsData();
    }

    console.log(this.yAxisNames)
  }


  getchartsData() {
    // console.log(this.duplicateItems.length);
    const test = this.yAxisNames;
    this.chartOptions = {
      chart: {
        type: 'scatter',
        zoomType: 'xy'
      },
      title: {
        text: 'Height Versus Weight of 507 Individuals by Gender'
      },
      subtitle: {
        text: 'Source: Heinz  2003'
      },
      xAxis: {
        // categories: xcategories,
        title: {
          enabled: true,
          text: 'Years(YYYY)'
        },

        labels: {
          formatter: function () {
            return moment.unix(this.value).format('YYYY');
          }
        },
        startOnTick: true,
        endOnTick: true,
        showLastLabel: true,

      },
      yAxis: {
        // categories: this.yAxisNames,
        title: {
          enabled: true,
          text: 'foo'
        },
        labels: {
          formatter: function () {
            return test[this.value];
          }
        },
        // startOnTick: true,
        // endOnTick: true,
        // showLastLabel: true,
        // showFirstLabel: true,
        // min: 0,
        tickInterval: 1
        // max: 5
      },
      legend: {
        layout: 'vertical',
        align: 'left',
        verticalAlign: 'top',
        x: 100,
        y: 70,
        floating: true,
        backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF',
        borderWidth: 1
      },
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
        formatter: function () {
          return this.point.standardName
        },
        shared: true
      },
      series: [{
        type: 'scatter',
        name:'Details',
        data: this.trimmedData
      }],

    };

  }

  }

