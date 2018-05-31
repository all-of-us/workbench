import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
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
  title ;
  subTitle;
  surveys: DbDomain[] = [];
  survey;
  surveyResult: QuestionConceptListResponse;
  resultsComplete = false;


  /* Have questions array for filtering and keep track of what answers the pick  */
  questions: any = [];
  searchText = '';
  prevSearchText = '';

  /* Show answers toggle */
  showAnswer = {};

  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }


  function;
  ngOnInit() {

    // Get the survey from local storage the user clicked on on a previous page
    const obj = localStorage.getItem('dbDomain');
    if (obj) {
      this.survey = JSON.parse(obj);
    }
    this.searchText = localStorage.getItem('searchText');
    if (!this.searchText) {
      this.searchText = '';
    }

    this.api.getSurveyResults(this.survey.conceptId.toString()).subscribe({
      next: x => {
        const questions = x.items;
        this.surveyResult = x;
        this.surveyResult.items = questions ;
        // Temp until answer order is fixed on server side , sort abc
        for (const q of questions ) {
          q.countAnalysis.results.sort((a1, a2) => {
            if (a1.stratum4 > a2.stratum4) { return 1; }
            if (a1.stratum4 < a2.stratum4) { return -1; }
            return 0;
          });
        }

        // Copy all qustions to display initially and filter on any search text passed in.
        this.questions = this.surveyResult.items;
        this.filterResults();
      },
      error: err => console.error('Observer got an error: ' + err),
      complete: () => { this.resultsComplete = true; }
    });

    this.api.getSurveyList().subscribe(
      result => {
        this.surveys = result.items;
      });

  }

  public searchQuestion(q: QuestionConcept) {
      if (q.conceptName.toLowerCase().indexOf(this.searchText.toLowerCase()) >= 0 ) { return true; }
      const results = q.countAnalysis.results.filter(r =>
          r.stratum4.toLowerCase().indexOf(this.searchText) >= 0);
      console.log('answer results filter ', results);
      if (results.length > 0) {
        return true;
      }
      // No hit
      return false ;
  }

  public filterResults() {
    /* Reset questions before filtering if length becomes less than prev or zero
      so backspacing works */
    if (this.prevSearchText.length > this.searchText.length ||
            this.searchText.length === 0) {
        this.questions = this.surveyResult.items;
    }
    this.prevSearchText = this.searchText;
    if (this.searchText.length > 0) {
        let filtered: QuestionConcept[] = [];
        filtered = this.questions.filter(this.searchQuestion, this);
        this.questions = filtered;
        console.log('Filtered to ' + this.searchText);
    }
  }

  public toggleAnswer(qid) {
    if (! this.showAnswer[qid] ) {
      this.showAnswer[qid] = true;
    } else {
      this.showAnswer[qid] = false;
    }
  }

  public showAnswerGraphs(q, a: AchillesResult) {
    console.log('In show answer graphs', a);
    q.selectedAnswer = a;
    console.log('Selected answer for q ' , q );
  }

}
