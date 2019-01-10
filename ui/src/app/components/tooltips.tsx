import * as React from 'react';

import {ClrIcon} from 'app/components/icons';

export const styles = {
  verticalAlign: {
    verticalAlign: 'middle',
    paddingLeft: '.25rem',
    paddingTop: '2.5rem'
  },

  tooltipContent: {
    position: 'absolute' as 'absolute',
    top: '0px',
    bottom: 'auto',
    left: '0px',
    right: 'auto',
  }
};

export const Tooltip = ({style = {}, ...props}) =>
  <input {...props} style={{...styles.verticalAlign, ...style}}/>;
export const TooltipContent = ({style = {}, ...props}) =>
  <div {...props} style={{...styles.tooltipContent, ...style}}>{props.children}</div>;
