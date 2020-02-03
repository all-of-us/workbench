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
  // The object destructuring below serves two purposes: (1) allows users of this component to pass
  // on styles which will override default FlexRow styles, and (2) allows other props passed to
  // FlexRow to be passed onto the inner <div>. We exclude data-test-id from being passed on, since
  // passing it onto the <div> would cause both the FlexRow and the <div> component to have the same
  // data-test-id, which complicates testing.
  const {style, 'data-test-id': dataTestId, ...other} = props;
  return <div {...other} style={{...flexStyle.row, ...style}}>
    {props.children}
  </div>;
};
FlexRow.displayName = 'FlexRow';

export const FlexColumn = (props) => {
  const {style, 'data-test-id': dataTestId, ...other} = props;
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
