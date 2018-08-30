import { Injectable } from '@angular/core';
import {ConceptGroup} from './conceptGroup';
import {ConceptWithAnalysis} from './conceptWithAnalysis';

@Injectable()

export class DbConfigService {
  /* CONSTANTS */
  MALE_GENDER_ID = '8507';
  FEMALE_GENDER_ID = '8532';
  OTHER_GENDER_ID = '8521';
  PREGNANCY_CONCEPT_ID = '903120';
  WHEEL_CHAIR_CONCEPT_ID = '903111';

  COUNT_ANALYSIS_ID = 3000;
  GENDER_ANALYSIS_ID = 3101;
  AGE_ANALYSIS_ID = 3102;
  SURVEY_COUNT_ANALYSIS_ID = 3110;
  SURVEY_GENDER_ANALYSIS_ID = 3111;
  SURVEY_AGE_ANALYSIS_ID = 3112;
  MEASUREMENT_AGE_ANALYSIS_ID = 3112;
  MEASUREMENT_VALUE_ANALYSIS_ID = 1900;

  /* Colors for chart */

  GENDER_COLORS = {
    '8507': '#8DC892',
    '8532': '#6CAEE3'
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


  pmGroups: ConceptGroup[] = [];

  constructor() {

  }

  getPmGroups(): ConceptGroup[] {
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

    return pmGroups;
  }

/*
  conceptGroups = [
    { group: 'blood-pressure', groupName: 'Mean Blood Pressure', concepts: [
      ,
      {conceptId: '903115', conceptName: 'Diastolic', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'height', groupName: 'Height', concepts: [
      {conceptId: '903133', conceptName: 'Height', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0}
    ]},
    { group: 'weight', groupName: 'Weight', concepts: [
      {conceptId: '903121', conceptName: 'Weight', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-waist', groupName: 'Mean waist circumference', concepts: [
      { conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-hip', groupName: 'Mean hip circumference', concepts: [
      {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-heart-rate', groupName: 'Mean heart rate', concepts: [
      {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'wheel-chair', groupName: 'Wheel chair use', concepts: [
      {conceptId: '903111', conceptName: 'Wheel chair use', analyses: null, chartType: 'column',
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'pregnancy', groupName: 'Pregnancy', concepts: [
      {conceptId: '903120', conceptName: 'Pregnancy', analyses: null, chartType: 'column',
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]}
  ];
*/


}
