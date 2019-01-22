import * as React from 'react';

import {withStyle} from 'app/utils/index';

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


export const FieldInput = withStyle(styles.input)('input');
export const FormInput = withStyle(styles.formInput)('input');
export const LongInput = withStyle({...styles.formInput, ...styles.longInput})('input');
export const Error = withStyle(styles.error)('div');
export const ErrorMessage = withStyle(styles.errorMessage)('div');
