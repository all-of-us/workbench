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
  @Input() selectedResult: any; // For ppi question answers analysis , we select an answer from the results we want to graph
  chartOptions: any;
  chartInstance: any;
  chartType = 'column';

  constructor() {
    highcharts.setOptions({
      lang: { thousandsSep: ',' },
    });
    //
  }

  saveInstance(chartInstance: any) {
    this.chartInstance = chartInstance;
  }

  // If analysis object results changed , update the chart
  ngOnChanges() {
    console.log('On changes');
    if (this.analysis && this.analysis.results.length) {
        // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions(this.analysis);
    }
  }

  public chartClick(e) {
    console.log('Chart clicked ', e);
  }
  public getChartType() {
    if (this.analysis.chartType) {
      return this.analysis.chartType;
    }
    if (this.analysis.analysisId === 3000 || this.analysis.analysisId === 3110 || this.analysis.analysisId === 3102 || this.analysis.analysisId === 3112) {
      return 'column';
    }
    if (this.analysis.analysisId === 3101 || this.analysis.analysisId === 3111) {
      return 'pie';
    }
  }
  public hcChartOptions(): any {
      const seriesData = this.makeSeriesData();
      return {
          chart: {
              type: this.getChartType(),
          },
          credits: {
              enabled: false
          },
          title: {
              text: this.analysis.analysisName
          },
          subtitle: {
          },
          tooltip: {
            pointFormat: '<b>{point.y}</b>'
          },
          plotOptions: {
              series: {
                  animation: {
                      duration: 100,
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
              categories: seriesData.categories,
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
          series: [ seriesData.series ],
          //colorByPoint: true
      };
  }

  /* For ppi answers we have to filter the results to that answer because all answers
   * for each question come in the analyses results
  */
  public getSelectedResults (selectedResult: any) {
    console.log('Selected result' , this.selectedResult);
    const results = [];
    for (const r of this.analysis.results) {
      if (r.stratum4 === selectedResult.stratum4) {
        results.push(r);
      }
    }

    return results;
  }

  public makeSeriesData() {
    if (this.analysis.analysisId === 3000 || this.analysis.analysisId === 3110) {
      return this.makeCountSeriesData();
    }
    if (this.analysis.analysisId === 3101 || this.analysis.analysisId === 3111) {
      return this.makeGenderSeriesData();
    }
    if (this.analysis.analysisId === 3102 || this.analysis.analysisId === 3112) {
      return this.makeAgeSeriesData();
    }
  }

  public makeCountSeriesData() {
      let data = [];
      let cats = [];
      for (const a  of this.analysis.results) {
          data.push({name: a.stratum4, y: a.countValue});
          cats.push(a.stratum4);
      }
      data = data.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0; }
      );
      cats = cats.sort((a, b) => {
        if (a > b) { return 1; }
        if (a < b) { return -1; }
        return 0;
      });
      const series = { name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#6CAEE3'] };
      return {series: series, categories: cats};

  }

  public makeGenderSeriesData() {
    const results = this.getSelectedResults(this.selectedResult);
    let data = [];
    let cats = [];
    for (const a  of results) {
      data.push({name: a.stratum5Name, y: a.countValue});
      cats.push(a.stratum4);
    }
    data = data.sort((a, b) => {
      if (a.name > b.name) {
        return 1;
      }
      if (a.name < b.name) {
        return -1;
      }
      return 0; }
    );
    cats = cats.sort((a, b) => {
      if (a > b) { return 1; }
      if (a < b) { return -1; }
      return 0;
    });
    const series = { name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#6CAEE3', '#000000'] };
    return {series: series, categories: cats};

  }

  public makeAgeSeriesData() {
    const results = this.getSelectedResults(this.selectedResult);

    // Age results have two stratum-- 1 is concept, 2 is age decile
    let data = [];
    let cats = [];
    for (const a  of results) {
      data.push({name: a.stratum5Name, y: a.countValue});
      cats.push(a.stratum5Name);
    }

    data = data.sort((a, b) => {
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      return 0; }
    );
    cats = cats.sort((a, b) => {
      if (a > b) { return 1; }
      if (a < b) { return -1; }
      return 0;
    });
    const series = { name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4', '#000000'] };
    return {series: series, categories: cats};
  }

}
