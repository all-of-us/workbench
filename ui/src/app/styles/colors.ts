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
  highlight: '#F8C954',
  warning: '#F7981C',
  danger: '#DB3214',
  dark: '#4A4A4A',
  // Text links and Icon links
  accent: '#216FB4',
  resourceCardHighlights: {
    conceptSet: '#A27BD7',
    cohort: '#F8C954',
    notebook: '#8BC990',
    dataSet: '#6CACE4'
  },
  white: '#fff',
};

// Range for whiteness is {-1, 1} where -1 is fully black, 0 is the color, and 1 is fully white.
export const colorWithWhiteness = (color: string, whiteness: number) => {
  return Color(color).mix(Color('white'), whiteness).toString();
};
