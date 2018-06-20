import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import {ISubscription} from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {QuestionConcept} from '../../../publicGenerated/model/questionConcept';
import {QuestionConceptListResponse} from '../../../publicGenerated/model/questionConceptListResponse';
@Component({
  selector: 'app-survey-view',
  templateUrl: './survey-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './survey-view.component.css']
})

export class SurveyViewComponent implements OnInit, OnDestroy {

  domainId: string;
  title ;
  subTitle;
  surveys: DbDomain[] = [];
  survey;
  surveyResult: QuestionConceptListResponse;
  resultsComplete = false;
  private subscriptions: ISubscription[] = [];
  loading = false;

  /* Have questions array for filtering and keep track of what answers the pick  */
  questions: any = [];
  searchText: FormControl = new FormControl();
  searchMethod = 'or';
  noResultsMessage = 'No questions match your search term.';

  /* Show answers toggle */
  showAnswer = {};

  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    this.loading = true;
    // Get the survey from local storage the user clicked on on a previous page
    const obj = localStorage.getItem('dbDomain');
    if (obj) {
      this.survey = JSON.parse(obj);
    }
    this.searchText.setValue(localStorage.getItem('searchText'));
    if (!this.searchText.value) {
      this.searchText.setValue('');
    }

    this.subscriptions.push(this.api.getSurveyResults(this.survey.conceptId.toString()).subscribe({
      next: x => {
        const questions = x.items;
        this.surveyResult = x;
        // Temp until answer order is fixed on server side , sort abc
        for (const q of questions ) {
          q.countAnalysis.results.sort((a1, a2) => {
            if (a1.stratum4 > a2.stratum4) { return 1; }
            if (a1.stratum4 < a2.stratum4) { return -1; }
            return 0;
          });
        }

        this.filterResults();
        this.loading = false;
      },
      error: err => console.error('Observer got an error: ' + err),
      complete: () => { this.resultsComplete = true; }
    }));

    // Filter when text value changes
    this.subscriptions.push(
      this.searchText.valueChanges
        .debounceTime(400)
        .distinctUntilChanged()
        .subscribe((query) => { this.filterResults(); } ));

  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  public searchQuestion(q: QuestionConcept) {
    // Todo , match all words maybe instead of any. Or allow some operators such as 'OR' 'AND'
    const text = this.searchText.value;

    let words = text.split(new RegExp(',| | and | or '));
    words = words.filter(w => w.length > 0 && w.toLowerCase() != 'and' && w.toLowerCase() != 'or');
    const reString = words.join('|');
    // If doing an and search match all words
    if (this.searchMethod === 'and') {
      for (const w of words) {
        if (q.conceptName.toLowerCase().indexOf(w.toLowerCase()) === -1  &&
          q.countAnalysis.results.filter(r =>
            r.stratum4.toLowerCase().indexOf(w.toLowerCase()) === -1 )) {
          return false;
        }
      }
      // All words found in either question or answers
      return true;
    }
    // Or search
    const re = new RegExp(reString, 'gi');
    if (re.test(q.conceptName)) {
      return true;
    }
    const results = q.countAnalysis.results.filter(r => re.test(r.stratum4));
    if (results.length > 0) {
      return true;
    }

    return false ;
  }

  public filterResults() {
    this.loading = true;
    this.questions = this.surveyResult.items;
    if (this.searchText.value.length > 0) {
      this.questions = this.questions.filter(this.searchQuestion, this);
    }
    this.loading = false;
  }

  public setSearchMethod(method: string, resetSearch: boolean = false) {
    this.searchMethod = method;
    if (resetSearch) {
      this.searchText.setValue('');
    }
    this.filterResults();
  }

  public toggleAnswer(qid) {
    if (! this.showAnswer[qid] ) {
      this.showAnswer[qid] = true;
    } else {
      this.showAnswer[qid] = false;
    }
  }

  public showAnswerGraphs(q, a: AchillesResult) {
    q.selectedAnswer = a;
  }

  public graphAnswerClicked(achillesResult) {
    console.log('Graph answer clicked ', achillesResult);
  }

}
