import * as React from 'react';

import { FlexColumn } from 'app/components/flex';
import colors from 'app/styles/colors';

const style = {
  color: colors.primary,
  fontSize: '14px',
  lineHeight: '22px',
  marginBottom: '0.5rem',
};

export const TextColumn = (props) => {
  return (
    <FlexColumn style={{ ...style, ...props.style }}>
      {props.children}
    </FlexColumn>
  );
};
