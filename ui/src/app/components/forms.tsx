import * as React from 'react';

export const styles = {
  formSection: {
    marginTop: '1rem',
    minWidth: '29rem'
  }
};

export const FormSection = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.formSection, ...style}} />;