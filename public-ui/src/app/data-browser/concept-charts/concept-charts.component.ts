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

  results;
  loading = false;
  // Get counts results for genders
  maleGenderResult: AchillesResult;
  femaleGenderResult: AchillesResult;
  otherGenderResult: AchillesResult;
  sourceConcepts = null;
  analyses: ConceptAnalysis;

  MALE_GENDER_ID = '8507';
  FEMALE_GENDER_ID = '8532';
  OTHER_GENDER_ID = '8521';
  PREGNANCY_CONCEPT_ID = '903120';
  WHEEL_CHAIR_CONCEPT_ID = '903111';

  private subscription: ISubscription;
  private subscription2: ISubscription;

  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    console.log("Concept chart concept", this.concept);
    this.loading = true;
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.subscription = this.api.getConceptAnalysisResults([conceptIdStr]).subscribe(results =>  {
      console.log(results);
      this.results = results.items;
      this.analyses = results.items[0];
      this.organizeGenders(this.analyses.genderAnalysis);
      console.log('analyses: ' , this.analyses);
      this.loading = false;
    });

    this.getSourceConcepts();
  }

  ngOnDestroy() {
    console.log('unsubscribing concept-charts');
    this.subscription.unsubscribe();
    this.subscription2.unsubscribe();
  }

  getSourceConcepts() {
    this.subscription2 = this.api.getSourceConcepts(this.concept.conceptId).subscribe(
      results => this.sourceConcepts = results.items
      );
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

    // Make Other results in one concept if we have them. Todo -- when we get more data will will have more genders
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
