import * as React from 'react';

export const buttonStyles = {
  btn: {
   fontWeight: 500,
   fontSize: '12px'
  } as React.CSSProperties,
  primary: {
    backgroundColor: '#262262',
    border: 'none',
    padding: '0rem 0.77rem',
    borderRadius: '0.3rem',
    cursor: 'hand',
    textTransform: 'uppercase',
    color: '#fff',
    letterSpacing: '0.02rem',
    lineHeight: '0.77rem'
  } as React.CSSProperties
};

export const PrimaryButton = ({style = {}, ...props}) =>
    <button {...props} style={{...buttonStyles.btn, ...buttonStyles.primary, ...style}}/>;

