import {Component, Input, OnChanges} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';


@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
export class ChartComponent implements OnChanges {
  @Input() analysis: Analysis;
  @Input() chartOptions;
  chart;
  chartType = 'column';

  constructor() {
    highcharts.setOptions({
      lang: { thousandsSep: ',' },
      colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4']
    });
    //
  }

  saveInstance(chartInstance) {
    this.chart = chartInstance;
  }

  chartClick(e) {
  }

  // If analysis object results changed , update the chart
  ngOnChanges() {
  console.log("On changes", this.chartOptions);
   if (this.analysis && this.analysis.results.length) {
      // HC automatically redraws when changing chart options

    }
  }

}
