import {reactStyles} from 'app/utils';
import * as React from 'react';

export const flexStyle = reactStyles({
  column: {
    display: 'flex',
    flexDirection: 'column'
  },
  row: {
    display: 'flex',
    flexDirection: 'row'
  }
});


export const FlexRow = (props) => {
  return <div style={{...flexStyle.row, ...props.style}}>
    {props.children}
  </div>;
};


export const FlexColumn = (props) => {
  return <div style={{...flexStyle.column, ...props.style}}>
    {props.children}
  </div>;
};

export const FlexRowWrap = (props) => {
  return <FlexRow style={{flexWrap: 'wrap', ...props.style}}>
    {props.children}
  </FlexRow>;
};

export const FlexDivider = (props) => {
  return <div style={{display: 'flex', flex: 1, ...props.style}}/>;
};
