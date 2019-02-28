import * as React from 'react';

export const styles = {
  infoIcon: {
    color: '#2691D0',
    height: '22px',
    width: '22px',
    fill: '#2691D0'
  },

  successIcon: {
    color: '#7AC79B',
    width: '20px',
    height: '20px'
  },

  warningIcon: {
    color: '#F68D76',
    width: '20px',
    height: '20px'
  }
};

export const ClrIcon = ({className = '', ...props}) => {
  return React.createElement('clr-icon', {class: className, ...props});
};

export const InfoIcon = ({style = {}, ...props}) =>
  <ClrIcon shape='info-standard' {...props} class='is-solid'
           style={{...styles.infoIcon, ...style}}/>;

export const ValidationIcon = props => {
  if (props.validSuccess === undefined) {
    return null;
  } else if (props.validSuccess) {
    return <ClrIcon shape='success-standard' class='is-solid'
                    style={{...styles.successIcon, ...props.style}}/>;
  } else {
    return <ClrIcon shape='warning-standard' class='is-solid'
                    style={{...styles.warningIcon, ...props.style}}/>;
  }
};
