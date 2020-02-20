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
  const {style, ...other} = props;
  return <div {...other} style={{...flexStyle.row, ...style}}>
    {props.children}
  </div>;
};
FlexRow.displayName = 'FlexRow';

export const FlexColumn = (props) => {
  const {style, ...other} = props;
  return <div {...other} style={{...flexStyle.column, ...style}}>
    {props.children}
  </div>;
};
FlexColumn.displayName = 'FlexColumn';

export const FlexRowWrap = (props) => {
  return <FlexRow style={{flexWrap: 'wrap', ...props.style}}>
    {props.children}
  </FlexRow>;
};

// FlexSpacer will take up as much space as is reasonable. For example:
// [FlexElement1] [FlexElement2] [-------------FlexSpacer-------------] [FlexElement3]
export const FlexSpacer = (props) => {
  return <div style={{display: 'flex', flex: 1, ...props.style}}/>;
};
