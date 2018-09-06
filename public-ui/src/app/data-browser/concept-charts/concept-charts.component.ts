import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import {ISubscription} from 'rxjs/Subscription';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';

import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Concept} from '../../../publicGenerated/model/concept';
import {ConceptAnalysis} from '../../../publicGenerated/model/conceptAnalysis';
import {DbConfigService} from '../../utils/db-config.service';

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
  @Input() showRace = false;
  @Input() showEthnicity = false;

  private subscriptions: ISubscription[] = [];
  loadingStack: any = [];
  results;
  maleGenderResult: AchillesResult;
  femaleGenderResult: AchillesResult;
  otherGenderResult: AchillesResult;
  maleGenderChartTitle =  '';
  femaleGenderChartTitle = '';
  otherGenderChartTitle = '';
  sourceConcepts: Concept[] = null;
  analyses: ConceptAnalysis;

  constructor(private api: DataBrowserService, public dbc: DbConfigService) { }

  loading() {
    return this.loadingStack.length > 0;
  }

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

    this.loadingStack.push(true);
    this.subscriptions.push( this.api.getSourceConcepts(this.concept.conceptId).subscribe(
      results => {
        this.sourceConcepts = results.items;
        this.loadingStack.pop();
      }));
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  // Organize genders and set the chart title for the gender charts for simple display
  organizeGenders(analysis: Analysis) {
    let otherCountValue = 0;
    const others = [];

    // No need to do anything if only one gender
    if (analysis.results.length <= 1) {
      return;
    }
    const results = [];
    for (const g of analysis.results) {
      if (g.stratum2 === this.dbc.MALE_GENDER_ID) {
        this.maleGenderResult = g;
        this.maleGenderChartTitle = g.analysisStratumName + ' - ' + g.countValue.toLocaleString();
      } else if (g.stratum2 === this.dbc.FEMALE_GENDER_ID) {
        this.femaleGenderResult = g;
        this.femaleGenderChartTitle = g.analysisStratumName + ' - ' + g.countValue.toLocaleString();
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
        stratum2: this.dbc.OTHER_GENDER_ID,
        analysisStratumName: 'Other',
        countValue: otherCountValue
      };
      this.otherGenderChartTitle = this.otherGenderResult.analysisStratumName + ' - ' +
        this.otherGenderResult.countValue.toLocaleString();
      analysis.results.push(this.otherGenderResult);
    }
  }
}
