import * as React from 'react';
import RSelect from 'react-select';

import {withStyle} from 'app/utils/index';

export const styles = {
  unsuccessfulInput: {
    backgroundColor: '#FCEFEC',
    borderColor: '#F68D76'
  },

  successfulInput: {
    borderColor: '#7AC79B'
  },

  error: {
    padding: '0 0.5rem',
    fontWeight: 600,
    color: '#2F2E7E',
    marginTop: '0.2rem',
    width: '90%'
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


export const Error = withStyle(styles.error)('div');
export const ErrorMessage = withStyle(styles.errorMessage)('div');

export const ValidationError = ({children}) => {
  if (!children) {
    return null;
  }
  return <div
    style={{
      color: '#c72314',
      fontSize: 10, fontWeight: 500, textTransform: 'uppercase',
      marginLeft: '0.5rem', marginTop: '0.25rem'
    }}
  >{children}</div>;
};

export const TextInput = React.forwardRef(({style = {}, onChange, invalid = false, ...props}:
      {style?: React.CSSProperties, onChange: Function, invalid?: boolean, [key: string]: any},
                                           ref: React.Ref<HTMLInputElement>) => {
  return <input
    {...props}
    ref = {ref}
    onChange={onChange ? (e => onChange(e.target.value)) : undefined}
    style={{
      width: '100%', height: '1.5rem',
      borderColor: '#c5c5c5', borderWidth: 1, borderStyle: 'solid', borderRadius: 3,
      padding: '0 0.5rem',
      backgroundColor: '#fff',
      ...(invalid ? styles.unsuccessfulInput : {}),
      ...style
    }}
  />;
});

export const TextArea = ({style = {}, onChange, invalid = false, ...props}) => {
  return <textarea
    {...props}
    onChange={onChange ? (e => onChange(e.target.value)) : undefined}
    style={{
      width: '100%',
      borderColor: '#c5c5c5', borderWidth: 1, borderStyle: 'solid', borderRadius: 3,
      padding: '0.25rem 0.5rem',
      backgroundColor: '#fff',
      ...(invalid ? styles.unsuccessfulInput : {}),
      ...style
    }}
  />;
};

export const RadioButton = ({ onChange, ...props }) => {
  return <input
    type='radio'
    {...props}
    onChange={onChange}
    onClick={onChange}
  />;
};

export const Select = ({value, options, onChange, ...props}) => {
  return <RSelect
    value={options.find(o => o.value === value)}
    options={options}
    onChange={o => onChange(o && o.value)}
    {...props}
  />;
};
