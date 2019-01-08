import * as React from 'react';

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

export const BolderHeader = ({style = {}, ...props}) =>
  <h1 {...props} style={{...styles.h1, ...style}}/>;
export const BoldHeader = ({style = {}, ...props}) =>
  <h2 {...props} style={{...styles.h2, ...style}}/>;
export const Header = ({style = {}, ...props}) =>
  <h3 {...props} style={{...styles.h3, ...style}}/>;
export const SmallHeader = ({style = {}, ...props}) =>
  <h4 {...props} style={{...styles.h4, ...style}}/>;
