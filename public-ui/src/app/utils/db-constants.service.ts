import { Injectable } from '@angular/core';

@Injectable()
export class DbConstantsService {
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


  constructor() { }

}
