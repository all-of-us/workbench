import {Component, Input, OnChanges, OnInit} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';


@Component({
  selector: 'app-age-chart',
  templateUrl: './age-chart.component.html',
  styleUrls: ['./age-chart.component.css']
})
export class AgeChartComponent implements OnChanges {
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


  public hcChartOptions(analysis: Analysis): any {
      const seriesData = this.makeAgeSeriesData();

      return {
          chart: {
              type: 'column',
              /* Note highcharts needs background here */
              backgroundColor: '#D9E4EA'
          },
          credits: {
              enabled: false
          },
          title: {
              text: analysis.analysisName
          },
          tooltip: {
            pointFormat: '<b>{point.y}</b>'
          },
          subtitle: {
          },
          plotOptions: {
              column: {
                shadow: false,
                colorByPoint: true,
                groupPadding: 0,
                dataLabels: {
                  enabled: false,
                  /*
                    rotation: -90,
                    align: 'right',
                    y: 10, // y: value offsets dataLabels in pixels.
                    style: {
                        'fontWeight': 'thinner',
                        'fontSize': '15px',
                        'textOutline': '1.75px black',
                        'color': 'white'
                    } */
                }
              },
              pie: {
                  // size: 260,
                  dataLabels: {
                      enabled: false,
                      distance: -50,
                      format: '{point.name} : {point.y}'
                  }
              },
          },
          legend: {
              enabled: false // this.seriesLegend()
          },
          series: [ seriesData.series ] ,
          xAxis: {
            categories: seriesData.categories,
            type: 'category',
            labels: {
              style: {
                whiteSpace: 'nowrap',
              }
            }
          },
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
