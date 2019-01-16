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
  },

  formInput: {
    borderRadius: '5px',
    backgroundColor: 'white',
    lineHeight: '1.5rem',
    height: '1.5rem',
    width: '16rem'
  }
};


export const FieldInput = ({style = {}, ...props}) =>
  <input {...props} style={{...styles.input, ...style}} />;
export const FormInput = ({style = {}, ...props}) =>
    <input {...props} style={{...styles.formInput, ...style}} ref={props.inputref}/>;
export const Error = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.error, ...style}} />;

