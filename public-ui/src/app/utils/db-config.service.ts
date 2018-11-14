import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {map} from 'rxjs/operators';
import {DataBrowserService} from '../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../publicGenerated/model/achillesResult';
import {Analysis} from '../../publicGenerated/model/analysis';
import {ConceptAnalysis} from '../../publicGenerated/model/conceptAnalysis';
import {ConceptGroup} from './conceptGroup';
import {ConceptWithAnalysis} from './conceptWithAnalysis';


@Injectable()

export class DbConfigService {
  /* CONSTANTS */
  MALE_GENDER_ID = '8507';
  FEMALE_GENDER_ID = '8532';
  OTHER_GENDER_ID = '8521';
  INTERSEX_GENDER_ID = '1585848';
  NONE_GENDER_ID = '1585849';
  PREGNANCY_CONCEPT_ID = '903120';
  WHEEL_CHAIR_CONCEPT_ID = '903111';

  COUNT_ANALYSIS_ID = 3000;
  GENDER_ANALYSIS_ID = 3101;
  GENDER_IDENTITY_ANALYSIS_ID = 3107;
  AGE_ANALYSIS_ID = 3102;
  SURVEY_COUNT_ANALYSIS_ID = 3110;
  SURVEY_GENDER_ANALYSIS_ID = 3111;
  SURVEY_GENDER_IDENTITY_ANALYSIS_ID = 3113;
  SURVEY_AGE_ANALYSIS_ID = 3112;
  MEASUREMENT_AGE_ANALYSIS_ID = 3112;
  MEASUREMENT_VALUE_ANALYSIS_ID = 1900;
  ETHNICITY_ANALYSIS_ID = 3104;
  RACE_ANALYSIS_ID = 3103;

  /* Colors for chart */

  GENDER_COLORS = {
    '8507': '#8DC892',
    '8532': '#6CAEE3',
    '1585848': '#4259A5',
    '1585849': '#252660'
  };

  GENDER_IDENTITY_COLORS = {
    '1585839': '#8DC892',
    '1585840': '#6CAEE3',
    '903070': '#252660',
    '903079': '#80C4EC',
    '1585841': '#4259A5',
    '1585842': '#80C4EC',
    '1585843': '#8DC892'
  };

  /* Age colors -- for now we just use one color pending design */
  AGE_COLOR = '#252660';

  /* Map for age decile to color */
  AGE_COLORS = {
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
  COLUMN_COLOR = '#6CAEE3';
  AXIS_LINE_COLOR = '#979797';

  /* Chart Styles */
  CHART_TITLE_STYLE = {
    'color': '#302C71', 'font-family': 'Gotham HTF',	'font-size': '14px', 'font-weight': '300'
  };
  DATA_LABEL_STYLE = {
    'color': '#FFFFFF', 'font-family': 'Gotham HTF',	'font-size': '14px',
    'font-weight': '300', 'textOutline': 'none'
  };
  GI_DATA_LABEL_STYLE = {
    'color': 'black', 'font-family': 'Gotham HTF',	'font-size': '14px',
      'font-weight': '300', 'textOutline': 'none'
  };

  pmGroups: ConceptGroup[] = [];
  genderAnalysis: Analysis;

  constructor(private api: DataBrowserService) {
    // Load up common simple data needed on pages
    // Get Demographic totals
    this.api.getGenderAnalysis().subscribe(result => {
      this.genderAnalysis = result;
    });

  }

  getPmGroups(): Observable<ConceptGroup[]> {
    /* What we want here is a function that will load the pm groups from the database one time
     * and save them on the service property for pages to use.
     * Todo this will have to be refreshed based on cdr version eventually
     */

    // Get pm data and groups for the app.
    if (this.pmGroups.length > 0) {
      return Observable.of(this.pmGroups);
    }

   // const conceptIds = ['903118', '903115', '903133', '903121', '903135',
    //  '903136', '903126', '903111', '903120'];

    // Set up the groups
    let chartType = 'histogram';
    const pmGroups: ConceptGroup[] = [];
    let group = new ConceptGroup('blood-pressure', 'Mean Blood Pressure');
    group.concepts.push(new ConceptWithAnalysis('903118', 'Systolic', chartType));
    group.concepts.push(new ConceptWithAnalysis('903115', 'Diastolic', chartType));
    pmGroups.push(group);

    group = new ConceptGroup('height', 'Height');
    group.concepts.push(new ConceptWithAnalysis('903133', group.groupName, chartType));
    pmGroups.push(group);

    group = new ConceptGroup('weight', 'Weight');
    group.concepts.push(new ConceptWithAnalysis('903121', group.groupName, chartType));
    pmGroups.push(group);

    group = new ConceptGroup('mean-waist', 'Mean waist circumference');
    group.concepts.push(new ConceptWithAnalysis('903135', group.groupName, chartType));
    pmGroups.push(group);

    group = new ConceptGroup('mean-hip', 'Mean hip circumference');
    group.concepts.push(new ConceptWithAnalysis('903136', group.groupName, chartType));
    pmGroups.push(group);

    group = new ConceptGroup('mean-heart-rate', 'Mean heart rate');
    group.concepts.push(new ConceptWithAnalysis('903126', group.groupName, chartType));
    pmGroups.push(group);

    chartType = 'column';

    group = new ConceptGroup('wheel-chair', 'Wheel chair use');
    group.concepts.push(new ConceptWithAnalysis('903111', group.groupName, chartType));
    pmGroups.push(group);

    group = new ConceptGroup('pregnancy', 'Pregnancy');
    group.concepts.push(new ConceptWithAnalysis('903120', group.groupName, chartType));
    pmGroups.push(group);

    // Get all the data for the concepts in the groups and put the analyses on the concepts
    const conceptIds = [];
    for (const g of pmGroups) {
      for (const c of g.concepts) {
        conceptIds.push(c.conceptId);
      }
    }


    return this.api.getConceptAnalysisResults(conceptIds).pipe(
      map(result => {
        // Put each concept analysis on the concept
        for (const item of result.items) {
          for (const g of pmGroups) {
            for (const c of g.concepts) {
              if (c.conceptId === item.conceptId) {
                c.analyses = item;
                // Arrage arrange the data and genders
                this.arrangeConceptAnalyses(c);
              }
            }
          }
        }
        // Finally have our physical measurement groups
        this.pmGroups = pmGroups;
        return this.pmGroups;
      })
    );
  }

  arrangeConceptAnalyses(concept: any) {
    if (concept.analyses.genderAnalysis) {
      this.organizeGenders(concept);
    }
    /* Todo maybe will use this next version of graphing
     if (concept.conceptId === this.PREGNANCY_CONCEPT_ID) {

      const pregnantResult  = concept.analyses.measurementValueFemaleAnalysis.results[0];
      if (pregnantResult) {
        //console.log("pregnancy concept ", concept.analyses.measurementValueFemaleAnalysis);
        // Delete any male results so we don't look dumb with dumb data
        concept.analyses.measurementValueMaleAnalysis = null;
        concept.analyses.measurementValueOtherGenderAnalysis = null;
        concept.analyses.genderAnalysis.results =
          concept.analyses.genderAnalysis.results.filter(result =>
            result.stratum2 === this.FEMALE_GENDER_ID);
        // Get the total female gender cout
        let femaleCount = 0;
        // Add not pregnant result to the female value results
        // because this concept is just a one value Yes
        femaleCount = concept.analyses.genderAnalysis.results[0].countValue;
        const notPregnantResult: AchillesResult = {
          analysisId: pregnantResult.analysisId,
          stratum1: pregnantResult.stratum1,
          stratum2: pregnantResult.stratum2,
          stratum3: pregnantResult.stratum3,
          stratum4: 'Not Pregnant',
          stratum5: pregnantResult.stratum5,
          countValue: femaleCount - pregnantResult.countValue
        };

        // Add Not pregnant to results,
        concept.analyses.measurementValueFemaleAnalysis.results.push(notPregnantResult);
      }
    }
    if (concept.conceptId === this.WHEEL_CHAIR_CONCEPT_ID) {
      // Todo What to do about this boolean concept , wait for design
      // Maybe we just handle thes on the api side
    }
    */
  }

  // Put the gender analysis in the order we want to show them
  // Sum up the other genders and make a result for that
  organizeGenders(concept: ConceptWithAnalysis) {
    const analysis: Analysis = concept.analyses.genderAnalysis;
    let male = null;
    let female = null;
    let intersex = null;
    let none = null;

    // No need to do anything if only one gender
    if (analysis.results.length <= 1) {
      return;
    }
    const results = [];
    for (const g of analysis.results) {
      if (g.stratum2 === this.MALE_GENDER_ID) {
        male = g;
      } else if (g.stratum2 === this.FEMALE_GENDER_ID) {
        female = g;
      } else if (g.stratum2 === this.INTERSEX_GENDER_ID) {
        intersex = g;
      } else if (g.stratum2 === this.NONE_GENDER_ID) {
        none = g;
      }
    }

    // Order genders how we want to display  Male, Female , Others
    if (male) { results.push(male); }
    if (female) { results.push(female); }
    if (intersex) { results.push(intersex); }
    if (none) { results.push(none); }
    analysis.results = results;
  }

}
