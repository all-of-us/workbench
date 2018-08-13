import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import * as highcharts from 'highcharts';
import 'highcharts/adapters/standalone-framework.src';


import {Analysis} from '../../../publicGenerated/model/analysis';
import {Concept} from '../../../publicGenerated/model/concept';

/* CONSTANTS */
const COUNT_ANALYSIS_ID = 3000;
const GENDER_ANALYSIS_ID = 3101;
const AGE_ANALYSIS_ID = 3102;
const SURVEY_COUNT_ANALYSIS_ID = 3110;
const SURVEY_GENDER_ANALYSIS_ID = 3111;
const SURVEY_AGE_ANALYSIS_ID = 3112;
const MEASUREMENT_AGE_ANALYSIS_ID = 3112;
const MEASUREMENT_VALUE_ANALYSIS_ID = 1900;
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
  'color': '#FFFFFF', 'font-family': 'Gotham HTF',	'font-size': '14px',
  'font-weight': '300', 'textOutline': 'none'
};

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
export class ChartComponent implements OnChanges {
  @Input() analysis: Analysis;
  @Input() concepts: Concept[] = []; // Can put in analysis or concepts to chart. Don't put both
  @Input() selectedResult: any;
  // For ppi question answers analysis , we select an answer from the results
  @Input() pointWidth = 10;   // Optional width of bar or point or box plot
  @Input() backgroundColor = '#FFFFFF'; // Optional background color
  @Input() chartTitle: string;
  @Input() chartType;
  @Input() sources = false;
  @Output() resultClicked = new EventEmitter<any>();
  chartOptions: any;
  chartInstance: any;


  constructor() {
    highcharts.setOptions({
      lang: {thousandsSep: ','},
    });
  }

  /* Todo -- maybe use this in future
  saveInstance(chartInstance: any) {
    this.chartInstance = chartInstance;
  }*/

  // Render new chart on changes
  ngOnChanges() {
    console.log(this.chartType, this.analysis);
    if ((this.analysis && this.analysis.results.length) ||
      (this.concepts && this.concepts.length)) {
      // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions();
    }

  }

  public chartClick(e) {
    console.log('Chart clicked ', e);
  }

  columnClick = function (e) {
    console.log('Column clicked ', e);
  };

  public hcChartOptions(): any {
    const options = this.makeChartOptions();
    // Override title if they passed one
    if (this.chartTitle) {
      options.title.text = this.chartTitle;
    }
    // Override chart type if we have it
    // todo  -- check we don't need this. too hacky
    /*if (this.chartType) {
      options.chart.type = this.chartType;
    }*/

    return {
      chart: options.chart,
      credits: {
        enabled: false
      },
      title: options.title,
      subtitle: {},
      tooltip: {
        pointFormat: '<b>{point.y} </b><br>{series.name}'
      },
      plotOptions: {
        series: {
          animation: {
            duration: 100,
          },
          pointWidth: options.pointWidth ? options.pointWidth : null,
          minPointLength: 3,
          events: {
            click: function (event) {
              console.log('plot options clicked ', event.point);
            }
          }
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
          },
          events: {},
        },
        bar: {
          shadow: false,
          borderColor: null,
          colorByPoint: true,
          groupPadding: 0,
          dataLabels: {
            enabled: false,
          },
          events: {}
        }
      },
      yAxis: {
        title: {
          text: null
        },
        lineWidth: 1,
        lineColor: '#979797',
        gridLineColor: this.backgroundColor
      },
      xAxis: {
        title: {
          text: options.xAxisTitle ? options.xAxisTitle : null
        },
        categories: options.categories,
        // type: 'category',
        labels: {
          style: {
            whiteSpace: 'nowrap',
          }
        },
        lineWidth: 1,
        lineColor: '#979797'
      },
      zAxis: {},
      legend: {
        enabled: false // this.seriesLegend()
      },
      series: [options.series],
    };
  }

  /* For ppi answers we have to filter the results to that answer because all answers
   * for each question come in the analyses results
  */
  public getSelectedResults(selectedResult: any) {
    const results = [];
    for (const r of this.analysis.results) {
      if (r.stratum4 === selectedResult.stratum4) {
        results.push(r);
      }
    }
    return results;
  }

  public makeChartOptions() {
    if (this.concepts.length > 0) {
      return this.makeConceptChartOptions();
    }
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
    if (this.analysis.analysisId === MEASUREMENT_VALUE_ANALYSIS_ID) {
      console.log('Making histogram opts');
      const options =  this.makeMeasurementChartOptions();
      return options;
    }


  }

  seriesClick(event) {
    console.log('Global series clicked ', this.analysis, 'Clicked analysis', event.point);
  }

  public makeCountChartOptions() {
    let data = [];
    let cats = [];
    for (const a  of this.analysis.results) {
      data.push({name: a.stratum4, y: a.countValue, thisCtrl: this, result: a});
      cats.push(a.stratum4);
    }
    data = data.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0;
      }
    );
    cats = cats.sort((a, b) => {
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      return 0;
    });

    const seriesClick = function (event) {
      const thisCtrl = event.point.options.thisCtrl;
      console.log('Count plot Clicked point :', event.point);
      thisCtrl.resultClicked.emit(event.point.result);
    };
    // Override tooltip and colors and such
    const series = {
      name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#6CAEE3'],
      tooltip: {pointFormat: '<b>{point.y} </b>'},
      events: {
        click: seriesClick
      }

    };
    return {
      chart: {type: 'column', backgroundColor: this.backgroundColor},
      title: {text: null},
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: null
    };

  }

  public makeConceptChartOptions() {
    const data = [];
    const cats = [];

    // Sort by count value
    this.concepts = this.concepts.sort((a, b) => {
        if (a.countValue < b.countValue) {
          return 1;
        }
        if (a.countValue > b.countValue) {
          return -1;
        }
        return 0;
      }
    );

    for (const a  of this.concepts) {
      data.push({
        name: a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ',
        y: a.countValue
      });
      if (!this.sources) {
        cats.push(a.conceptName);
      } else {
        cats.push(a.vocabularyId + '-' + a.conceptCode);
      }
    }

    // Override tooltip and colors and such
    const series = {
      name: this.concepts[0].domainId, colorByPoint: true, data: data, colors: ['#6CAEE3'],
      tooltip: {pointFormat: '<b>{point.y} </b>'}
    };
    return {
      chart: {
        type: 'column',
        backgroundColor: this.backgroundColor,
      },
      title: {text: null, style: CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: null
    };

  }

  public makeGenderChartOptions() {
    // For ppi we need to filter the results to the particular answer that the user selected
    // because we only show the breakdown for one answer on this chart
    let results = [];
    let seriesName = '';
    if (this.analysis.analysisId === GENDER_ANALYSIS_ID) {
      results = this.analysis.results;
      seriesName = this.analysis.analysisName;
    } else {
      results = this.getSelectedResults(this.selectedResult);
      // Series name for answers is the answer selected which is in stratum4
      seriesName = this.selectedResult.stratum4;
    }
    let data = [];
    let cats = [];
    for (const a  of results) {
      // For normal Gender Analysis , the stratum2 is the gender . For ppi it is stratum5;
      const color = a.analysisId === GENDER_ANALYSIS_ID ?
        GENDER_COLORS[a.stratum2] : GENDER_COLORS[a.stratum5];
      data.push({
        name: a.analysisStratumName
        , y: a.countValue, color: color, sliced: true
      });
      cats.push(a.stratum4);
    }
    data = data.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0;
      }
    );
    cats = cats.sort((a, b) => {
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      return 0;
    });
    const series = {name: seriesName, colorByPoint: true, data: data};
    return {
      chart: {type: 'pie', backgroundColor: this.backgroundColor}, // '#D9E4EA'
      title: {text: this.analysis.analysisName, style: CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: null,
      xAxisTitle: null
    };

  }

  public makeAgeChartOptions() {
    let results = [];
    let seriesName = '';

    // Question/answers have a different data structure than other concepts
    if (this.analysis.analysisId === AGE_ANALYSIS_ID) {
      results = this.analysis.results;
      seriesName = this.analysis.analysisName;
    } else {
      results = this.getSelectedResults(this.selectedResult);
      // Series name for answers is the answer selected which is in stratum4
      seriesName = this.selectedResult.stratum4;
    }
    // Age results have two stratum-- 1 is concept, 2 is age decile
    // Sort by age decile (stratum2)
    results = results.sort((a, b) => {
        if (Number(a.stratum2) > Number(b.stratum2)) {
          return 1;
        }
        if (Number(a.stratum2) < Number(b.stratum2)) {
          return -1;
        }
        return 0;
      }
    );
    const data = [];
    const cats = [];
    for (const a  of results) {
      // For normal AGE Analysis , the stratum2 is the age decile. For ppi it is stratum5;
      const color = a.analysisId === AGE_ANALYSIS_ID ? AGE_COLORS[a.stratum2] :
        AGE_COLORS[a.stratum5];
      data.push({
        name: a.analysisStratumName
        , y: a.countValue, color: color
      });
      cats.push(a.analysisStratumName);
    }

    const series = {name: seriesName, colorByPoint: true, data: data};
    return {
      chart: {type: 'column', backgroundColor: this.backgroundColor},
      title: {text: this.analysis.analysisName, style: CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: null
    };
  }

  // Todo maybe use something like this
 /* public makeMeasurementChartOptions() {
    if (this.analysis.chartType === 'histogram') {
      return this.makeHistogramChartOptions();
    }
    if (this.analysis.chartType === 'column') {
      return this.makeCountChartOptions();
    }

  }
*/
  // Histogram data analyses come already binned
  // The value is in stratum 4, the unit in stratum5, the countValue in the bin is countValue
  // and we also have
  // sourceCountValue
  public makeMeasurementChartOptions() {
    let data = [];
    const cats = [];

    for (const a  of this.analysis.results) {
      data.push({name: a.stratum4, y: a.countValue, thisCtrl: this, result: a});
    }
    data = data.sort((a, b) => {
      let aVal: any = a.name;
      let bVal: any = b.name;
      // Sort  numeric data as number
      if ( isNaN(Number(a.name)) ) {
        // Don't do anything
      } else {
        // Make a number so sort works
        aVal = Number(aVal);
        bVal = Number(b.name);
      }

      if (aVal  > bVal) {
        return 1;
      }
      if (aVal < bVal) {
        return -1;
      }
      return 0;
    });
    for (const d of data) {
      cats.push(d.name);
    }

    // Todo later
    const seriesClick = function(event) {
      const thisCtrl = event.point.options.thisCtrl;
      // console.log('Histogram plot Clicked point :',  event.point);
      thisCtrl.resultClicked.emit(event.point.result);
    };

    // Unit is in stratum5
    const unit: string = this.analysis.results[0].stratum5;
    // Override tooltip and colors and such
    const series: any = {
      name: this.analysis.analysisName, colorByPoint: true, data: data, colors: ['#6CAEE3'],
      tooltip: {
        headerFormat: '<span style="font-size: 10px">{point.key} ' + unit + '</span><br/>',
        pointFormat: '<b> {point.y} participants </b> '
      },
      events: {
        // Todo maybe later click: seriesClick
      }};

    // Note that our data is binned already so we use a column chart to show histogram.
    // How ever some boolean measurements like pregnancy but two bins , Yes and No. So
    // they will pass the chartType="column" to this component
    if (this.chartType === 'histogram') {
      // Make column chart look like  a histogram with these options
      series.pointPadding = 0;
      series.borderWidth = 0;
      series.groupPadding = 0;
      series.pointWidth = null;
      series.shadow = false;
    }

    return {
      chart: {type: 'column', backgroundColor: this.backgroundColor},
      title: { text: this.chartTitle },
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: unit
    };

  }

}
