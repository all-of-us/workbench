import * as React from 'react';
export const styles = {
  input: {
    marginLeft: '.5rem',
    width: '90%'
  },

  unsuccessfulInput: {
    backgroundColor: '#FCEFEC',
    borderColor: '#F68D76'
  },

  error: {
    padding: '0 0.5rem',
    fontWeight: 600,
    color: '#2F2E7E',
    marginTop: '0.2rem',
    width: '90%'
  }
};


export const FieldInput = ({style = {}, ...props}) =>
  <input {...props} style={{...styles.input, ...style}} />;
export const Error = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.error, ...style}} />;

