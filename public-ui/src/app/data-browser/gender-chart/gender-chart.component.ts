import {Component, Input, OnChanges, OnInit} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';


@Component({
  selector: 'app-gender-chart',
  templateUrl: './gender-chart.component.html',
  styleUrls: ['./gender-chart.component.css']
})
export class GenderChartComponent implements OnChanges {
  @Input() analysis: Analysis;
  @Input() chartOptionsInput: any;
  @Input() selectedResult: any; // For ppi question answers analysis , we select an answer from the results we want to graph
  chartOptions: any;
  chartInstance: any;
  chartType = 'column';

  constructor() {
    highcharts.setOptions({
      lang: { thousandsSep: ',' },
      //colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4']
      colors: ['#6CAEE3']
    });
    //
  }

  saveInstance(chartInstance: any) {
    this.chartInstance = chartInstance;
  }

  // If analysis object results changed , update the chart
  ngOnChanges() {
    console.log('On changes', this.analysis);
    if (this.chartOptionsInput) {
      this.chartOptions = this.chartOptionsInput;
    } else if (this.analysis && this.analysis.results.length) {
        // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions(this.analysis);
    }
  }

  public chartClick(e) {
    console.log('Chart clicked ', e);
  }
  public getSelectedResults (selectedResult: any) {
    console.log("Selected result" , this.selectedResult);
    const results = [];
    for (const r of this.analysis.results) {
      if (r.stratum4 === selectedResult.stratum4) {
        results.push(r);
      }
    }

    return results;
  }

  public hcChartOptions(analysis: Analysis): any {
      const results = this.getSelectedResults(this.selectedResult);
    console.log("Results ", results);
      return {
          chart: {
              type: 'pie',
              backgroundColor: '#D9E4EA'
          },
          credits: {
              enabled: false
          },
          title: {
              text: analysis.analysisName
          },
          subtitle: {
          },
          plotOptions: {
              pie: {
                  // size: 260,
                  dataLabels: {
                      enabled: true,
                      distance: -50,
                      format: '{point.name} : {point.y}'
                  }
              },
          },
          legend: {
              enabled: false // this.seriesLegend()
          },
          series: this.makeCountSeries(results),
          colorByPoint: true
      };
  }

  public makeCountSeries(results) {
      const chartSeries = [{ name: this.analysis.analysisName, data: [] }];
      for (const a  of results) {
          chartSeries[0].data.push({name: a.stratum5Name, y: a.countValue});
      }
      chartSeries[0].data = chartSeries[0].data.sort((a, b) => a.name - b.name);
      console.log(chartSeries);
      return chartSeries;
  }

  public makeCountCategories(analysis: Analysis) {
      const cats = [];
      for (const a  of this.analysis.results) {
          cats.push(a.stratum4);
      }
      return cats;
  }
}
