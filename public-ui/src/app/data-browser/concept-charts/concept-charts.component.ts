import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import {ISubscription} from 'rxjs/Subscription';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';

import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Concept} from '../../../publicGenerated/model/concept';
import {ConceptAnalysis} from '../../../publicGenerated/model/conceptAnalysis';

@Component({
  selector: 'app-concept-charts',
  templateUrl: './concept-charts.component.html',
  styleUrls: ['./concept-charts.component.css']
})
export class ConceptChartsComponent implements OnInit, OnDestroy {
  @Input() concept: Concept;
  @Input() backgroundColor = '#ECF1F4'; // background color to pass to the chart component
  @Input() showSources = true;
  @Input() showGender = true;
  @Input() showAge = true;
  @Input() showMeasurementGenderBins = false;

  private subscriptions: ISubscription[] = [];
  loadingStack: any = [];
  loading() {
    return this.loadingStack.length > 0;
  }

  results;
  maleGenderResult: AchillesResult;
  femaleGenderResult: AchillesResult;
  otherGenderResult: AchillesResult;
  sourceConcepts = null;
  analyses: ConceptAnalysis;

  MALE_GENDER_ID = '8507';
  FEMALE_GENDER_ID = '8532';
  OTHER_GENDER_ID = '8521';

  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    this.loadingStack.push(true);
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.subscriptions.push( this.api.getConceptAnalysisResults([conceptIdStr]).subscribe(
      results =>  {
        this.results = results.items;
        this.analyses = results.items[0];
        this.organizeGenders(this.analyses.genderAnalysis);
        this.loadingStack.pop();
      }));
    this.getSourceConcepts();
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  getSourceConcepts() {
    this.loadingStack.push(true);
    this.subscriptions.push( this.api.getSourceConcepts(this.concept.conceptId).subscribe(
      results => {
        this.sourceConcepts = results.items;
        this.loadingStack.pop();
      }));
  }

  organizeGenders(analysis: Analysis) {
    let otherCountValue = 0;
    const others = [];

    // No need to do anything if only one gender
    if (analysis.results.length <= 1) {
      return;
    }
    const results = [];
    for (const g of analysis.results) {
      if (g.stratum2 === this.MALE_GENDER_ID) {
        this.maleGenderResult = g;
      } else if (g.stratum2 === this.FEMALE_GENDER_ID) {
        this.femaleGenderResult = g;

      } else {
        otherCountValue += g.countValue;
      }
    }

    // Put our gender results in order we want
    analysis.results = [this.maleGenderResult, this.femaleGenderResult];

    // Make Other results in one concept if we have them.
    // Todo -- when we get more data will will have more genders
    if (otherCountValue > 0) {
      this.otherGenderResult = {
        analysisId: analysis.results[0].analysisId,
        stratum1: analysis.results[0].stratum1,
        stratum2: this.OTHER_GENDER_ID,
        analysisStratumName: 'Other',
        countValue: otherCountValue
      };
      analysis.results.push(this.otherGenderResult);
    }
  }

  makeChartTitle(result: AchillesResult) {
    console.log(result);
    const title = result.analysisStratumName + ' - ' +
      result.countValue.toLocaleString();
    return title;
  }
}
