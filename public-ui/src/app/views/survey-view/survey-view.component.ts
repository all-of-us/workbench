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

  /* Have questions array for filtering */
  questions: QuestionConcept[] = [];
  searchText = '';
  prevSearchText = '';

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
            this.api.getSurveyResults(this.survey.conceptId.toString()).subscribe({
              next: x => {
                const questions = x.items;
                this.surveyResult = x;
                this.surveyResult.items = [];
                // Todo ignore these on server side , anything that doesn't have a count
                for (let i = 0;  i < questions.length ; i++ ) {
                  const q = questions[i];
                  if (q.countAnalysis != null) {
                    this.surveyResult.items.push(q);
                  }
                }
                // Copy all qustions to display initially
                this.questions = this.surveyResult.items;
                console.log(this.surveyResult);
              },
              error: err => console.error('Observer got an error: ' + err),
              complete: () => { this.resultsComplete = true; }
            });
          }
        }
      }
    );


  }

  public searchQuestion(q: QuestionConcept) {
      if (q.conceptName.toLowerCase().indexOf(this.searchText) >= 0 ) { return true; }
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
    /* Reset questions before filtering if length becomes less than prev or zero */
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

}
