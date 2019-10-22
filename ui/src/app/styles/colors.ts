import * as Color from 'color';

export default {
  // Style guide found here:
  // https://projects.invisionapp.com/d/main#/console/17894189/371138106/preview

  // Used for buttons and text
  primary: '#262262',
  // Used for tab navigation
  secondary: '#6CACE4',
  success: '#8BC990',
  // Used for tables
  light: '#E9ECEF',
  lightBackground: '#E6E4Ed',
  highlight: '#F8C954',
  warning: '#F7981C',
  danger: '#DB3214',
  dark: '#4A4A4A',
  // Text links and Icon links
  accent: '#216FB4',
  disabled: '#9B9B9B',
  resourceCardHighlights: {
    cohort: '#F8C954',
    cohortReview: '#A8385D',
    conceptSet: '#A27BD7',
    dataSet: '#6CACE4',
    notebook: '#8BC990'
  },
  white: '#fff',
  workspacePermissionsHighlights: {
    'OWNER': '#4996A2',
    'READER': '#8F8E8F',
    'WRITER': '#92B572'
  }
};

// Range for whiteness is {-1, 1} where -1 is fully black, 0 is the color, and 1 is fully white.
export const colorWithWhiteness = (color: string, whiteness: number) => {
  return Color(color).mix(Color('white'), whiteness).toString();
};
