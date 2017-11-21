import { Component, OnInit, Input, NgModule } from '@angular/core';
import { ChartModule } from 'angular2-highcharts';

// import highcharts and highmaps and add highmaps to it.
// Note, must import the js/modules/map so it can play nice. See highcharts docs
// https://www.highcharts.com/docs/getting-started/installation
//import * as highcharts from 'highcharts';
//import * as highmaps from 'highcharts/js/modules/map';

// Call this to add higcharts-more functionality
export declare var require: any;
const highcharts = require('highcharts');
ChartModule.forRoot(
  highcharts,
  require('highcharts/highcharts-more')
);
//highmaps(highcharts);  // Call this to add highmaps functionality to highcharts




import { Analysis, AnalysisResult } from '../analysisClasses';
import { IConcept } from '../ConceptClasses';

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
@NgModule({
  imports: [ChartModule]
})
export class ChartComponent  {
  @Input() redraw;
  @Input() analysis: Analysis;
  @Input() concepts: IConcept[];
  chartType
  chartOptions
  chart
  localAnalysis: Analysis;
  constructor() {
    highcharts.setOptions({
      lang: { thousandsSep: ',' },
      colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4']
    })
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
      let a  = this.makeConceptsAnalysis(this.concepts);
      this.chartOptions = a.hcChartOptions();
      this.chartType = a.chartType;
    }
    else if (this.analysis && this.analysis.results.length) {
      // HC automatically redraws when changing chart options
      this.localAnalysis  = this.analysis;
      this.chartOptions = this.localAnalysis.hcChartOptions();
      this.chartType = this.analysis.chartType;
    }
  }

  makeConceptsAnalysis(concepts: IConcept[]): Analysis {

    // Make an analysis object id = 3000 with results to chart these concepts results in histogram
    let obj = {
      analysis_id: 3000,
      analysis_name: "Number of Participants by Source Concepts",
      results: [],
      chartType: "column",
    }

    for (let i = 0; i < this.concepts.length; i++) {

      // //

      let resultObj = {
        analysis_id: 3000,
        stratum_1_name: this.concepts[i].concept_name,
        stratum_1: this.concepts[i].concept_id,
        count_value: this.concepts[i].count_value
      }
      let series = new AnalysisResult(resultObj)
      obj.results.push(series)
    }
    let a = new Analysis(obj);
    //
    return a;
  }
}
