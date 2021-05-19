import {faCheck, faCheckCircle, faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import colors, {addOpacity} from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {faAlarmExclamation} from '@fortawesome/pro-solid-svg-icons';

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
  },

  circleBackground: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },

  defaultCircle: {
    height: '1.5rem',
    width: '1.5rem',
  }
};

export const PlaygroundIcon = () => <svg
  version='1.1'
  id='Layer_1'
  viewBox='0 0 14.1 12.3'>
      <title>Group</title>
      <desc>Created with Sketch.</desc>
      <g id='ANAYLSIS'>
        <g id='Notebook-PREVIEW' transform='translate(-363.000000, -85.000000)'>
          <g id='Group-5' transform='translate(363.000000, 85.000000)'>
            <g id='Group-3' transform='translate(0.889808, 0.297292)'>
              <g id='Group' transform='translate(0.047675, 0.625027)'>
                <path d={
                  `M11.8-0.9c0.7,0,1.3,0.6,1.3,1.3V10c0,0.7-0.6,1.3-1.3,1.3H0.4c
            -0.7,0-1.3-0.6-1.3-1.3V0.4c0-0.7,0.6-1.3,1.3-1.3C0.4-0.9,11.8-0.9,11.8-0.9z`
                }/>
              </g>
            </g>
          </g>
        </g>
      </g>
      <path fill='#FFFFFF' d={`M3.5,3.2H2.1C2,3.2,1.9,3.1,1.9,3V1.7c0-0.1,0.1-0.2,0.2-0.2h1.5c0.1,0,
  0.2,0.1,0.2,0.2V3C3.7,3.1,3.6,3.2,3.5,3.2z`}/>
      <path fill='#FFFFFF' d={`M12.4,3.2h-7C5.3,3.2,5.2,3.1,5.2,3V1.7c0-0.1,0.1-0.2,0.2-0.2h7c0.1,0,
  0.2,0.1,0.2,0.2V3C12.6,3.1,12.5,3.2,12.4,3.2z`}/>
      <g>
        <path fill='#FFFFFF' d={`M10.8,4.6l0.5,0.5c0.1,0.1,0.1,0.2,0,0.3l-5.4,5.4c-0.1,0.1-0.2,
    0.1-0.3,0L3.2,8.4c-0.1-0.1-0.1-0.2,0-0.3l0.5-0.5c0.1-0.1,0.2-0.1,0.3,0l1.7,1.7l4.8-4.8C10.6,
    4.5,10.8,4.5,10.8,4.6z`}/>
      </g>
</svg>;


export const ClrIcon = ({className = '', ...props}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return React.createElement('clr-icon', {class: className, ...fp.omit(['data-test-id'], props)});
};

export const SnowmanIcon = ({style= {}, ...props}) => {
  return <ClrIcon shape='ellipsis-vertical' {...props} style={{...styles.snowmanIcon, ...style}}/>;
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

const svgIcon = src => ({size = 25, ...props}) => <img style={{height: size, width: size}} src={src} {...props}/>;

export const CheckCircle = props => <Icon shape={faCheckCircle} {...props}/>;
export const Check = props => <Icon shape={faCheck} {...props}/>;
export const Times = props => <Icon shape={faTimes} {...props}/>;
export const ControlledTierBadge = svgIcon('/assets/icons/controlled-tier-badge.svg');
export const Arrow = svgIcon('/assets/icons/arrow-left-regular.svg');
export const withCircleBackground = WrappedIcon => ({style = styles.defaultCircle}) => {
  return <div style={{...style, ...styles.circleBackground}}>
    <WrappedIcon/>
  </div>;
};
export const AlarmExclamation = props => <Icon shape={faAlarmExclamation} {...props}/>;
