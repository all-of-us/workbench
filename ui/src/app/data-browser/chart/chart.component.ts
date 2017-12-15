import {Component, Input, NgModule, OnChanges} from '@angular/core';
import { ChartModule } from 'angular2-highcharts';
import { AchillesService} from '../services/achilles.service';

import { Analysis } from '../AnalysisClasses';
import { Concept } from '../ConceptClasses';

// This draws a highchart from an analysis object
// import highcharts and highmaps and add highmaps to it.
// Note, must import the js/modules/map so it can play nice. See highcharts docs
// https://www.highcharts.com/docs/getting-started/installation
// import * as highcharts from 'highcharts';
// import * as highmaps from 'highcharts/js/modules/map';

// Call this to add higcharts-more functionality
export declare var require: any;
const highcharts = require('highcharts');
ChartModule.forRoot(
  highcharts,
  require('highcharts/highcharts-more')
);
// highmaps(highcharts);  // Call this to add highmaps functionality to highcharts

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
@NgModule({
  imports: [ChartModule]
})
export class ChartComponent implements OnChanges {
  @Input() redraw;
  @Input() analysis: Analysis;
  @Input() concepts: Concept[];
  chartType;
  chartOptions;
  chart;
  localAnalysis: Analysis;
  constructor(private achillesService: AchillesService) {
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
    this.chartOptions = null;
    if (this.concepts && this.concepts.length) {
      const a  = this.achillesService.makeConceptsCountAnalysis(this.concepts);
      this.chartOptions = a.hcChartOptions();
      this.chartType = a.chartType;
    } else if (this.analysis && this.analysis.results.length) {
      // HC automatically redraws when changing chart options
      this.chartOptions = this.analysis.hcChartOptions();
      this.chartType = this.analysis.chartType;
    }
  }

}
