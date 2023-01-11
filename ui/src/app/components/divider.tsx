import * as React from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';

export const Divider = (props) => {
  const verticalMargin = props.verticalMargin || '.75rem';
  return (
    <hr
      style={{
        ...{
          width: '100%',
          margin: verticalMargin + ' 0',
          backgroundColor: colorWithWhiteness(colors.dark, 0.5),
          border: '0 none',
          height: 1,
        },
        ...props.style,
      }}
    />
  );
};
