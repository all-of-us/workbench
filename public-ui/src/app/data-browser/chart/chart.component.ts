import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import * as highcharts from 'highcharts';

import {Analysis} from '../../../publicGenerated/model/analysis';
import {Concept} from '../../../publicGenerated/model/concept';
import {DbConfigService} from '../../utils/db-config.service';

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
export class ChartComponent implements OnChanges {
  @Input() analysis: Analysis;
  @Input() concepts: Concept[] = []; // Can put in analysis or concepts to chart. Don't put both
  @Input() selectedResult: any; // For ppi question, this is selected answer.
  @Input() pointWidth = 10;   // Optional width of bar or point or box plot
  @Input() backgroundColor = '#FFFFFF'; // Optional background color
  @Input() chartTitle: string;
  @Input() chartType: string;
  @Input() sources = false;
  @Input() genderId: string; // Hack until measurement design of graphs gender overlay
  @Output() resultClicked = new EventEmitter<any>();
  chartOptions: any = null;

  constructor(private dbc: DbConfigService) {
    highcharts.setOptions({
      lang: {thousandsSep: ','},
    });
  }

  // Render new chart on changes
  ngOnChanges() {
    if ((this.analysis && this.analysis.results && this.analysis.results.length) ||
      (this.concepts && this.concepts.length)) {
      // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions();
    }
  }

  public isSurveyGenderAnalysis() {
    return this.analysis ?
      (this.analysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_GENDER_IDENTITY_ANALYSIS_ID)
      : false;
  }

  public isGenderIdentityAnalysis() {
    return this.analysis ?
      (this.analysis.analysisId === this.dbc.GENDER_IDENTITY_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_GENDER_IDENTITY_ANALYSIS_ID)
      : false;
  }

  public hcChartOptions(): any {
    const options = this.makeChartOptions();
    // Override title if they passed one
    if (this.chartTitle) {
      options.title.text = this.chartTitle;
    }

    return {
      chart: options.chart,
      lang: {
        noData: {
        style: {
          fontWeight: 'bold',
          fontSize: '15px',
          color: '#303030'
        }
        }
      },
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
              // Todo handle click and log events in analytics
              // console.log('plot options clicked ', event.point);
            }
          }
        },
        pie: {
          borderColor: null,
          slicedOffset: 4,
          size: this.isSurveyGenderAnalysis() ? '60%' : '100%',
          dataLabels: {
            enabled: true,
            style: this.isGenderIdentityAnalysis()
                ? this.dbc.GI_DATA_LABEL_STYLE : this.dbc.DATA_LABEL_STYLE,
            distance: this.isGenderIdentityAnalysis() ? 3 : -30,
            format: '{point.name} {point.percentage:.0f}%',
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
        lineColor: this.dbc.AXIS_LINE_COLOR,
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
        lineColor: this.dbc.AXIS_LINE_COLOR
      },
      zAxis: {},
      legend: {
        enabled: false
      },
      series: [options.series],
    };
  }


  public makeChartOptions() {
    if (this.concepts.length > 0) {
      return this.makeConceptChartOptions();
    }
    if (this.analysis.analysisId === this.dbc.COUNT_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_COUNT_ANALYSIS_ID) {
      return this.makeCountChartOptions();
    }

    if (this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID) {
      return this.makeGenderChartOptions();
    }

    if (this.analysis.analysisId === this.dbc.GENDER_IDENTITY_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_GENDER_IDENTITY_ANALYSIS_ID) {
      return this.makeGenderChartOptions();
    }

    /* Todo make charts for ethniticy and race
     * maybe cleanup / generalize pie chart
    if (
      this.analysis.analysisId === this.dbc.ETHNICITY_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.RACE_ANALYSIS_ID) {
      return this.makePieChartOptions();
    }*/

    if (this.analysis.analysisId === this.dbc.AGE_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID) {
      return this.makeAgeChartOptions();
    }
    if (this.analysis.analysisId === this.dbc.MEASUREMENT_VALUE_ANALYSIS_ID) {
      return this.makeMeasurementChartOptions();
    }
    console.log('Error: Can not make chart options for this analysis. :', this.analysis);
  }

  seriesClick(event) {
    // Todo handle click and log events in analytics
    // console.log('Global series clicked ', this.analysis, 'Clicked analysis', event.point);
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
      // Todo handle click and log events in analytics
      // console.log('Count plot Clicked point :', event.point);
      thisCtrl.resultClicked.emit(event.point.result);
    };
    // Override tooltip and colors and such
    const series = {
      name: this.analysis.analysisName,
      colorByPoint: true,
      data: data,
      colors: [this.dbc.COLUMN_COLOR],
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
      title: {text: null, style: this.dbc.CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: null
    };

  }

  public makeGenderChartOptions() {
    let results = [];
    let seriesName = '';
    if (this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.ETHNICITY_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.RACE_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.GENDER_IDENTITY_ANALYSIS_ID) {
      results = this.analysis.results;
      seriesName = this.analysis.analysisName;
    } else {
      // For ppi we need to filter the results to the particular answer that the user selected
      // because we only show the breakdown for one answer on this chart
      // results = this.getSelectedResults(this.selectedResult);
      results = this.analysis.results.filter( r => r.stratum4 === this.selectedResult.stratum4);
      // Series name for answers is the answer selected which is in stratum4
      seriesName = this.selectedResult.stratum4;
    }
    let data = [];
    let cats = [];
    for (const a  of results) {
      // For normal Gender Analysis , the stratum2 is the gender . For ppi it is stratum5;
      let color = null;
      if (this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID) {
        color = this.dbc.GENDER_COLORS[a.stratum2];
      }
      if (this.analysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID) {
        color = this.dbc.GENDER_COLORS[a.stratum5];
      }
      if (this.analysis.analysisId === this.dbc.GENDER_IDENTITY_ANALYSIS_ID) {
        color = this.dbc.GENDER_IDENTITY_COLORS[a.stratum2];
      }

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
      title: {text: this.analysis.analysisName, style: this.dbc.CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: null,
      xAxisTitle: null
    };

  }

  public makeAgeChartOptions() {
    let results = [];
    let seriesName = '';
    let ageDecileStratum = '';

    // Question/answers have a different data structure than other concepts
    if (this.analysis.analysisId === this.dbc.AGE_ANALYSIS_ID) {
      results = this.analysis.results;
      seriesName = this.analysis.analysisName;
      ageDecileStratum = 'stratum2';
    } else if (this.analysis.analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID) {
      // For ppi survey we filter the results to the particular answer that the user selected
      results = this.analysis.results.filter( r => r.stratum4 === this.selectedResult.stratum4);
      // Series name for answers is the answer selected which is in stratum4
      seriesName = this.selectedResult.stratum4;
      ageDecileStratum = 'stratum5';
    }

    // Age results have two stratum-- 1 is concept, 2 is age decile
    // Sort by age decile (stratum2 or stratum5)
    results = results.sort((a, b) => {
      const anum = Number(a[ageDecileStratum]);
      const bnum = Number(b[ageDecileStratum]);
      if (anum > bnum) {
        return 1;
      }
      if (anum < bnum) {
        return -1;
      }
      return 0;
      }
    );
    const data = [];
    const cats = [];
    const color = this.dbc.AGE_COLOR;
    for (const a  of results) {
      data.push({
        name: a.analysisStratumName,
        y: a.countValue, color: color
      });
      cats.push(a.analysisStratumName);
    }

    const series = {name: seriesName, colorByPoint: true, data: data};
    return {
      chart: {type: 'column', backgroundColor: this.backgroundColor},
      title: {text: this.analysis.analysisName, style: this.dbc.CHART_TITLE_STYLE},
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: null
    };
  }

  // Histogram data analyses come already binned
  // The value is in stratum 4, the unit in stratum5, the countValue in the bin is countValue
  // and we also have
  // sourceCountValue
  public makeMeasurementChartOptions() {
    let data = [];
    const cats = [];
    // Todo overlay genders on one graph , use hack for separate gender graphs now
    // Hack to filter gender
    let results = this.analysis.results.concat([]);
    if (this.genderId) {
      results = results.filter(r => r.stratum3 === this.genderId);
    }
    for (const a  of results) {
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

    // Todo we will use this later in drill downs and such
    const seriesClick = function(event) {
      const thisCtrl = event.point.options.thisCtrl;
      // Todo handle click events
      // console.log('Histogram plot Clicked point :',  event.point);
      // thisCtrl.resultClicked.emit(event.point.result);
    };

    // Unit for measurements is in stratum5
    const unit = this.analysis.unitName ? this.analysis.unitName : '';
    const series: any = {
      name: this.analysis.analysisName,
      colorByPoint: true,
      data: data,
      colors: [this.dbc.COLUMN_COLOR],
      tooltip: {
        headerFormat: '<span style="font-size: 10px">{point.key} ' + unit + '</span><br/>',
        pointFormat: '<b> {point.y} participants </b> '
      },
    };

    // Note that our data is binned already so we use a column chart to show histogram
    // however we need to style it to make it look like a histogram. Some measurements
    // like pregnancy and wheel chair we don't want a histogram.
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
