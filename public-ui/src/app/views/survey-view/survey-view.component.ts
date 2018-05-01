import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Analysis} from '../../../publicGenerated/model/analysis';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
import {QuestionConcept} from '../../../publicGenerated/model/questionConcept';
import {QuestionConceptListResponse} from '../../../publicGenerated/model/questionConceptListResponse';
import {ChartComponent} from '../../data-browser/chart/chart.component';


@Component({
  selector: 'app-survey-view',
  templateUrl: './survey-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './survey-view.component.css']
})

export class SurveyViewComponent implements OnInit {

  domainId: string;
  title = 'Hello Survey';
  subTitle = 'Domain Desc here ';
  surveys: DbDomain[] = [];
  survey: DbDomain;
  surveyResult: QuestionConceptListResponse;
  resultsComplete = false;
  chartOptions;
  countAnalysisId = 3110;

  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }


  ngOnInit() {
    this.api.getSurveyList().subscribe(
      result => {
        this.surveys = result.items;
        console.log('Survey view: ' ,  this.surveys);
        for (const item of this.surveys) {
          if (item.domainId.toLowerCase() === this.domainId.toLowerCase()) {
            this.survey = item;
            console.log('Survey again. Getting results: ' , this.survey);

          }
        }
      }
    );

    this.api.getSurveyResults('1586134').subscribe({
      next: x => {
        this.surveyResult = x; console.log(this.surveyResult);
      },
      error: err => console.error('Observer got an error: ' + err),
      complete: () => { this.resultsComplete = true; }
    });
  }

  public hcChartOptions(analysis: Analysis): any {
    return {

      chart: {
        type: 'column',
      },
      credits: {
        enabled: false
      },

      title: {
        text: 'Response distribution'
      },
      subtitle: {
      },
      plotOptions: {
        series: {
          animation: {
            duration: 350,
          },
          maxPointWidth: 45
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
            enabled: true,
            rotation: -90,
            align: 'right',
            y: 10, // y: value offsets dataLabels in pixels.

            style: {
              'fontWeight': 'thinner',
              'fontSize': '15px',
              'textOutline': '1.75px black',
              'color': 'white'
            }
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
        enabled: true // this.seriesLegend()
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
