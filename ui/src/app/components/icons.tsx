import {faCheckCircle} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import * as React from 'react';

import colors from 'app/styles/colors';

export const styles = {
  infoIcon: {
    color: colors.accent,
    height: '22px',
    width: '22px',
    fill: colors.accent
  },

  successIcon: {
    color: colors.success,
    width: '20px',
    height: '20px'
  },

  dangerIcon: {
    color: colors.danger,
    width: '20px',
    height: '20px'
  },

  snowmanIcon: {
    marginLeft: -9,
    width: '21px',
    height: '21px'
  }
};

export const ClrIcon = ({className = '', ...props}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return React.createElement('clr-icon', {class: className, ...fp.omit(['data-test-id'], props)});
};

export const SnowmanIcon = ({style= {}, disabled = false, ...props}) => {
  return <ClrIcon shape='ellipsis-vertical' {...props}
                  style={{
                    ...styles.snowmanIcon,
                    color: disabled ? colors.disabled : colors.accent,
                    cursor: disabled ? 'auto' : 'pointer',
                    ...style
                  }}/>;
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
                    style={{...styles.dangerIcon, ...props.style}}/>;
  }
};

const Icon = ({ shape, size, style, color, ...props }) => {
  return <FontAwesomeIcon icon={shape} style={{height: size, width: size, color, ...style}} {...props}/>;
};

export const CheckCircle = props => <Icon shape={faCheckCircle} {...props}/>;
export const ControlledTierBadge = ({size = 25, ...props}) =>
  <img style={{height: size, width: size}} src={'/assets/icons/controlled-tier-badge.svg'} {...props}/>;
