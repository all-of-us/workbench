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
  disabled: '#9B9B9B',
  select: '#659F3D',
  resourceCardHighlights: {
    cohort: '#F8C954',
    cohortReview: '#A8385D',
    conceptSet: '#A27BD7',
    dataSet: '#6CACE4',
    notebook: '#8BC990'
  },
  white: '#fff',
  black: '#000',
  workspacePermissionsHighlights: {
    'OWNER': '#4996A2',
    'READER': '#8F8E8F',
    'WRITER': '#92B572'
  },
  chartColors: ['#216FB4', '#6CACE4', '#8BC990', '#F8C954', '#F7981C', '#F0718B', '#F38D7A', '#A27DB7', '#CAB2D6'],
  // High-contrast colors for runtime / extract status icon
  asyncOperationStatus: {
    starting: '#9FFF00',
    running: '#73EF0A',
    succeeded: '#73EF0A',
    stopping: '#FFD700',
    stopped: '#F8C954',
    error: '#DB3214',
  }
};

class Rgba {
  r: number;
  g: number;
  b: number;
  a?: number;

  constructor(r: number, g: number, b: number, a?: number) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = a;
  }

  toString() {
    return `rgba(${this.r}, ${this.g}, ${this.b}, ${this.a})`;
  }
}

// Range for whiteness is {-1, 1} where -1 is fully black, 0 is the color, and 1 is fully white.
export const colorWithWhiteness = (color: string, whiteness: number) => {
  return Color(color).mix(Color('white'), whiteness).toString();
};

export function hexToRgb(hex: string): Rgba {
  // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
  const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
  hex = hex.replace(shorthandRegex, (m, r, g, b) => {
    return r + r + g + g + b + b;
  });

  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? new Rgba(
    parseInt(result[1], 16),
    parseInt(result[2], 16),
    parseInt(result[3], 16)) : null;
}

export function addOpacity(color: string, opacity: number) {
  const rgba = hexToRgb(color);
  rgba.a = opacity;
  return rgba;
}
