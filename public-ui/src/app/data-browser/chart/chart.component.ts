import {Component, Input, OnChanges} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../AnalysisClasses';
import {Concept} from '../ConceptClasses';
import {AchillesService} from '../services/achilles.service';


@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
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
