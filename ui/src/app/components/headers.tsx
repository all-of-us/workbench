import * as React from 'react';

import {withStyle} from 'app/utils/index';

const headerColor = '#262262';

export const styles = {
  h1: {
    color: headerColor,
    fontWeight: 500,
    fontSize: '1.6rem',
  },
  h2: {
    color: headerColor,
    fontWeight: 800,
    fontSize: '1.25rem',
  },
  h3: {
    color: headerColor,
    fontWeight: 800,
    fontSize: '1rem',
  },
  h4: {
    fontWeight: 300,
    color: '#000',
  }
};

export const BolderHeader = withStyle(styles.h1)('h1');
export const BoldHeader = withStyle(styles.h2)('h2');
export const Header = withStyle(styles.h3)('h3');
export const SmallHeader = withStyle(styles.h4)('h4');
