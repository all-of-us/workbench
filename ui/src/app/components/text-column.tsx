import {FlexColumn} from 'app/components/flex';
import colors from 'app/styles/colors';
import * as React from 'react';

export const TextColumn = (props) => {
  return <FlexColumn style={{
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '22px',
    marginBottom: '0.5rem'
  }}>
    {props.children}
  </FlexColumn>;
};
