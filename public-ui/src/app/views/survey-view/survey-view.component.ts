import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import {ISubscription} from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {QuestionConcept} from '../../../publicGenerated/model/questionConcept';
import {QuestionConceptListResponse} from '../../../publicGenerated/model/questionConceptListResponse';
import {SurveyModule} from '../../../publicGenerated/model/surveyModule';

@Component({
  selector: 'app-survey-view',
  templateUrl: './survey-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './survey-view.component.css']
})

export class SurveyViewComponent implements OnInit, OnDestroy {

  domainId: string;
  title ;
  subTitle;
  surveys: SurveyModule[] = [];
  survey;
  surveyConceptId;
  surveyResult: any;
  resultsComplete = false;
  private subscriptions: ISubscription[] = [];
  loading = false;
  surveyPdfUrl = '/assets/surveys/' + this.surveyConceptId + '.pdf';
  surveyName: string;
  conceptCodeTooltip: any;
  genderGraph: string;
  binnedSurveyQuestions: string[] = ['1585864', '1585870', '1585873', '1585795', '1585802',
    '1585820', '1585889', '1585890'];

  /* Have questions array for filtering and keep track of what answers the pick  */
  questions: any = [];
  searchText: FormControl = new FormControl();
  searchMethod = 'or';

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
    const obj = localStorage.getItem('surveyModule');
    if (obj) {
      const survey = JSON.parse(obj);
      this.surveyConceptId = survey.conceptId;
      this.surveyPdfUrl = '/assets/surveys/' + survey.name.replace(' ', '_') + '.pdf';
    }
    this.searchText.setValue(localStorage.getItem('searchText'));
    if (!this.searchText.value) {
      this.searchText.setValue('');
    }

    this.subscriptions.push(this.api.getSurveyResults(this.surveyConceptId.toString()).subscribe({
      next: x => {
        this.surveyResult = x;
        this.survey = this.surveyResult.survey;
        this.surveyName = this.survey.name;

        // Add Did not answer to each question
        for (const q of this.surveyResult.items) {
          // Get did not answer count for question and count % for each answer
          // Todo -- add this to api maybe
          let didNotAnswerCount  = this.survey.participantCount;
          for (const a of q.countAnalysis.results) {
            didNotAnswerCount = didNotAnswerCount - a.countValue;
            a.countPercent = this.countPercentage(a.countValue);
          }
          const result = q.countAnalysis.results[0];
          if (didNotAnswerCount < 0 ) { didNotAnswerCount = 0; }
          const notAnswerPercent = this.countPercentage(didNotAnswerCount);
          const didNotAnswerResult = {
            analysisId : result.analysisId,
            countValue: didNotAnswerCount,
            countPercent: notAnswerPercent,
            stratum1: result.stratum1,
            stratum2: result.stratum2,
            stratum3: result.stratum3,
            stratum4: 'Did not answer',
            stratum5: result.stratum5
          };
          q.countAnalysis.results.push(didNotAnswerResult);
        }

        this.questions = this.surveyResult.items;
        // Sort count value desc
        for (const q of this.questions ) {
          q.countAnalysis.results.sort((a1, a2) => {
            if (a1.countValue > a2.countValue) { return -1; }
            if (a1.countValue < a2.countValue) { return 1; }
            return 0;
          });
        }

        this.filterResults();
        this.loading = false;
      },
      error: err => {
        console.error('Observer got an error: ' + err);
        this.loading = false;
      },
      complete: () => { this.resultsComplete = true; }
    }));

    // Filter when text value changes
    this.subscriptions.push(
      this.searchText.valueChanges
        .debounceTime(400)
        .distinctUntilChanged()
        .subscribe((query) => { this.filterResults(); } ));

    // Set to loading as long as they are typing
    this.subscriptions.push(this.searchText.valueChanges.subscribe(
      (query) => this.loading = true ));
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  public countPercentage(countValue: number) {
    if (!countValue || countValue <= 0) { return 0; }
    let percent: number = countValue / this.survey.participantCount ;
    percent = parseFloat(percent.toFixed(4));

    return percent * 100;
  }

  public searchQuestion(q: QuestionConcept) {
    // Todo , match all words maybe instead of any. Or allow some operators such as 'OR' 'AND'
    const text = this.searchText.value;

    let words = text.split(new RegExp(',| | and | or '));
    words = words.filter(w => w.length > 0
      && w.toLowerCase() !== 'and'
      && w.toLowerCase() !== 'or');
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
    localStorage.setItem('searchText', this.searchText.value);
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

  public selectSurveyGenderGraph(g) {
      if (g === 'Gender Identity') {
        this.genderGraph = 'GI';
      } else {
        this.genderGraph = 'BS';
      }
  }
  public convertToNum(s) {
    return Number(s);
  }

}
