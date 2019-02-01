import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as Interactive from 'react-interactive';

const styles = {
  base: {
    display: 'inline-flex', justifyContent: 'space-around', alignItems: 'center',
    height: '1.5rem', minWidth: '3rem', maxWidth: '15rem',
    fontWeight: 500, fontSize: 12, letterSpacing: '0.02rem', textTransform: 'uppercase',
    overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
    userSelect: 'none',
    margin: 0, padding: '0rem 0.77rem',
  }
};

const buttonVariants = {
  primary: {
    style: {
      ...styles.base,
      borderRadius: '0.3rem',
      backgroundColor: '#262262', color: '#fff',
    },
    disabledStyle: {backgroundColor: '#c3c3c3'},
    hover: {backgroundColor: '#4356A7'}
  },
  secondary: {
    style: {
      ...styles.base,
      border: '2px solid', borderRadius: '0.2rem', borderColor: '#262262',
      backgroundColor: 'transparent',
      color: '#262262',
    },
    disabledStyle: {
      borderColor: '#c3c3c3',
      backgroundColor: '#f1f2f2', color: '#c3c3c3'
    },
    hover: {backgroundColor: '#262262', color: '#ffffff'}
  },
  darklingPrimary: {
    style: {
      ...styles.base,
      borderRadius: '0.2rem',
      backgroundColor: '#262262', color: '#ffffff'
    },
    disabledStyle: {backgroundColor: '#c3c3c3'},
    hover: {backgroundColor: 'rgba(255,255,255,0.3)'}
  },
  darklingSecondary: {
    style: {
      ...styles.base,
      borderRadius: '0.2rem',
      backgroundColor: '#0079b8', color: '#ffffff'
    },
    disabledStyle: {backgroundColor: '#c3c3c3'},
    hover: {backgroundColor: '#50ACE1'}
  }
};

const computeStyle = ({style, hover, disabledStyle}, {disabled}) => {
  return {
    style: {...style, ...(disabled ? disabledStyle : {})},
    hover: disabled ? undefined : hover
  };
};

export const Clickable = ({as = 'div', disabled = false, onClick = null, ...props}) => {
  return <Interactive
    as={as} {...props}
    onClick={(...args) => onClick && !disabled && onClick(...args)}
  />;
};

export const Button = ({type = 'primary', style = {}, disabled = false, ...props}) => {
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(buttonVariants[type], {disabled}), {style})}
  />;
};

export const MenuItem = ({icon, tooltip = '', disabled = false, children, ...props}) => {
  return <TooltipTrigger side='left' content={tooltip}>
    <Clickable
      data-test-id={icon}
      disabled={disabled}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'start',
        fontSize: 12, minWidth: 125, height: 32,
        color: disabled ? colors.gray[2] : 'black',
        padding: '0 12px',
        cursor: disabled ? 'not-allowed' : 'pointer'
      }}
      hover={!disabled ? {backgroundColor: colors.blue[3]} : undefined}
      {...props}
    >
      <ClrIcon shape={icon} style={{marginRight: 8}} size={15}/>
      {children}
    </Clickable>
  </TooltipTrigger>;
};
