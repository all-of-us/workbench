import * as React from 'react';

import colors from 'app/styles/colors';

export const styles = {
  infoIcon: {
    color: colors.blue[0],
    height: '22px',
    width: '22px',
    fill: '#2691D0'
  },

  successIcon: {
    color: colors.green[1],
    width: '20px',
    height: '20px'
  },

  warningIcon: {
    color: colors.yellow[2],
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
