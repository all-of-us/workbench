import {Component, Input, OnChanges, OnInit} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';


@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
export class ChartComponent implements OnChanges {
  @Input() analysis: Analysis;
  @Input() chartOptionsInput: any;
  chartOptions : any;
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

  saveInstance(chartInstance:any) {
    this.chartInstance = chartInstance;
  }

  // If analysis object results changed , update the chart
  ngOnChanges() {
    console.log('On changes');
    if (this.chartOptionsInput) {
      this.chartOptions = this.chartOptionsInput;
    } else if (this.analysis && this.analysis.results.length) {
        // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions(this.analysis);
    }
  }

  public chartClick(e) {
    console.log("Chart clicked ", e);
  }
  public hcChartOptions(analysis: Analysis): any {
      return {
          chart: {
              type: analysis.chartType ? analysis.chartType : 'column',
          },
          credits: {
              enabled: false
          },
          title: {
              text: null
          },
          subtitle: {
          },
          plotOptions: {
              series: {
                  animation: {
                      duration: 150,
                  },
                  pointWidth: 10,
                  minPointLength: 3
              },
              pie: {
                  // size: 260,
                  dataLabels: {
                      enabled: true,
                      distance: -50,
                      format: '{point.name} <br> Count: {point.y}'
                  }
              },
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
              }
          },
          yAxis: {
          },
          xAxis: {
              categories: this.makeCountCategories(analysis),
              type: 'category',
              labels: {
                  style: {
                      whiteSpace: 'nowrap',
                  }
              }
          },
          zAxis: {
          },
          legend: {
              enabled: false // this.seriesLegend()
          },
          series: this.makeCountSeries(analysis),
          colorByPoint: true
      };
  }

  public makeCountSeries(analysis: Analysis) {
      const chartSeries = [{ data: [] }];
      for (const a  of analysis.results) {
          chartSeries[0].data.push({name: a.stratum4, y: a.countValue});
      }
      chartSeries[0].data = chartSeries[0].data.sort((a, b) => a.name - b.name);

      return chartSeries;
  }

  public makeCountCategories(analysis: Analysis) {
      const cats = [];
      for (const a  of analysis.results) {
          cats.push(a.stratum4);
      }
      return cats;
  }
}
