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

  successfulInput: {
    borderColor: '#7AC79B'
  },

  longInput: {
    width: '16rem'
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
  },

  errorMessage: {
    width: '12.5rem',
    padding: '.25rem',
    float: 'right' as 'right',
    marginTop: 0,
    background: '#f5dbd9',
    color: '#565656',
    border: '1px solid #ebafa6',
    display: 'flex' as 'flex',
    flexDirection: 'row' as 'row',
    fontSize: '13px'
  },

  iconArea: {
    display: 'inline-block',
    marginLeft: '-30px',
    minWidth: '30px'
  }
};


export const FieldInput = ({style = {}, ...props}) =>
  <input {...props} style={{...styles.input, ...style}} />;
export const FormInput = ({style = {}, ...props}) =>
    <input {...props} style={{...styles.formInput, ...style}} ref={props.inputref}/>;
export const LongInput = ({style = {}, ...props}) =>
  <input {...props} style={{...styles.formInput, ...styles.longInput, ...style}} />;
export const Error = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.error, ...style}} />;
export const ErrorMessage = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.errorMessage, ...style}} />;
