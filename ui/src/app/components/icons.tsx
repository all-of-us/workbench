import * as React from 'react';

export const styles = {
  infoIcon: {
    color: '#302C71',
    height: '22px',
    width: '22px',
    fill: '#302C71'
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

export const ClrIcon = props => {
  return React.createElement('clr-icon', props);
};

export const InfoIcon = ({style = {}, ...props}) =>
  <ClrIcon shape='info-standard' {...props} class='is-solid'
           style={{...styles.infoIcon, ...style}}/>;

export const ValidationIcon = props => {
  if (props.notValid()) {
    return <ClrIcon shape='warning-standard' class='is-solid'
                    style={{...styles.warningIcon, ...props.style}}/>;
  } else if (props.validSuccess()) {
    return <ClrIcon shape='success-standard' class='is-solid'
                    style={{...styles.successIcon, ...props.style}}/>;
  } else {
    return <div/>;
  }
};
