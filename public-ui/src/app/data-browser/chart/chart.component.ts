import {Component, Input, OnChanges, OnInit} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';

/* CONSTANTS */
const COUNT_ANALYSIS_ID = 3000;
const GENDER_ANALYSIS_ID = 3101;
const AGE_ANALYSIS_ID = 3102;
const SURVEY_COUNT_ANALYSIS_ID = 3110;
const SURVEY_GENDER_ANALYSIS_ID = 3111;
const SURVEY_AGE_ANALYSIS_ID = 3112;
const GENDER_COLORS = {
  '8507': '#8DC892',
  '8532': '#6CAEE3'
};
const AGE_COLORS = {
  '1': '#252660',
  '2': '#4259A5',
  '3': '#6CAEE3',
  '4': '#80C4EC',
  '5': '#F8C75B',
  '6': '#8DC892',
  '7': '#F48673',
  '8': '#BF85F6',
  '9': '#BAE78A',
  '10': '#8299A5',
  '11': '#000000',
  '12': '#DDDDDD'
};
const CHART_TITLE_STYLE = {
  'color': '#302C71', 'font-family': 'Gotham HTF',	'font-size': '14px', 'font-weight': '300'
};
const DATA_LABEL_STYLE = {
  'color': '#FFFFFF', 'font-family': 'Gotham HTF',	'font-size': '14px', 'font-weight': '300', 'textOutline': 'none'
};

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
      this.chartOptions = this.hcChartOptions();
    }
  }

  public chartClick(e) {
    console.log('Chart clicked ', e);
  }


  public hcChartOptions(): any {
      const options = this.makeChartOptions();
      return {
          chart: options.chart,
          credits: {

              enabled: false
          },
          title: options.title,
          subtitle: {
          },
          tooltip: {
            pointFormat: '<b>{point.y} </b><br>{series.name}'
          },
          plotOptions: {
              series: {
                  animation: {
                      duration: 100,
                  },
                  pointWidth: options.pointWidth,
                  minPointLength: 3
              },
              pie: {
                  borderColor: null,
                  slicedOffset: 4,
                  dataLabels: {
                      enabled: true,
                      style: DATA_LABEL_STYLE,
                      distance: -30,
                      format: '{point.name} {point.percentage:.0f}%'
                  }
              },
              column: {
                  shadow: false,
                  borderColor: null,
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
              categories: options.categories,
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
          series: [ options.series ],
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

  public makeChartOptions() {
    if (this.analysis.analysisId === COUNT_ANALYSIS_ID ||
        this.analysis.analysisId === SURVEY_COUNT_ANALYSIS_ID) {
      return this.makeCountChartOptions();
    }
    if (this.analysis.analysisId === GENDER_ANALYSIS_ID ||
        this.analysis.analysisId === SURVEY_GENDER_ANALYSIS_ID) {
      return this.makeGenderChartOptions();
    }
    if (this.analysis.analysisId === AGE_ANALYSIS_ID ||
      this.analysis.analysisId === SURVEY_AGE_ANALYSIS_ID) {
      return this.makeAgeChartOptions();
    }
  }

  public makeCountChartOptions() {
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

      // Override tooltip and colors and such
      const series = {
        name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#6CAEE3'],
        tooltip: {pointFormat: '<b>{point.y} </b>'}
      };
      return {
        chart: {type: 'column', backgroundColor: '#FFFFFF'},
        title: { text: null },
        series: series,
        categories: cats,
        pointWidth: 10
      };

  }

  public makeGenderChartOptions() {
    const results = this.getSelectedResults(this.selectedResult);
    let data = [];
    let cats = [];
    for (const a  of results) {
      data.push({name: a.stratum5Name, y: a.countValue, color: GENDER_COLORS[a.stratum5], sliced: true});
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
    const seriesName = this.selectedResult.stratum4;
    const series = { name: seriesName, colorByPoint: true, data: data };
    return {
      chart: {type: 'pie', backgroundColor: '#D9E4EA'},
      title: { text: this.analysis.analysisName, style: CHART_TITLE_STYLE },
      series: series,
      categories: cats,
      pointWidth: 10
    };

  }

  public makeAgeChartOptions() {
    const results = this.getSelectedResults(this.selectedResult);

    // Age results have two stratum-- 1 is concept, 2 is age decile
    let data = [];
    let cats = [];
    for (const a  of results) {
      data.push({name: a.stratum5Name, y: a.countValue, color: AGE_COLORS[a.stratum5]});
      cats.push(a.stratum5Name);
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

    // Series name for answers is the answer selected which is in stratum4
    const seriesName = this.selectedResult.stratum4;
    const series = { name: seriesName, colorByPoint: true, data: data};
    return {
      chart: {type: 'column', backgroundColor: '#D9E4EA'},
      title: { text: this.analysis.analysisName, style: CHART_TITLE_STYLE },
      series: series,
      categories: cats,
      pointWidth: 20,
    };
  }

}
