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

export const BolderHeader = ({children, style = {}}) =>
  <h1 style={{...styles.h1, ...style}}>{children}</h1>;
export const BoldHeader = ({children, style = {}}) =>
  <h2 style={{...styles.h2, ...style}}>{children}</h2>;
export const Header = ({children, style = {}}) =>
  <h3 style={{...styles.h3, ...style}}>{children}</h3>;
export const SmallHeader = ({children, style = {}}) =>
  <h4 style={{...styles.h4, ...style}}>{children}</h4>;
