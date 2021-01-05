import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';

export const WarningMessage = ({children}) => {
  return <FlexRow
    style={{
      alignItems: 'center',
      backgroundColor: colorWithWhiteness(colors.warning, .9),
      border: `1px solid ${colors.warning}`,
      borderRadius: '5px',
      color: colors.dark,
      marginTop: '.5rem',
      padding: '.5rem 0px'
    }}
  >
    <ClrIcon
      style={{color: colors.warning, marginLeft: '.5rem'}}
      shape={'warning-standard'}
      size={30}
      class={'is-solid'}
    />
    <div style={{paddingLeft: '0.5rem', paddingRight: '0.5rem'}}>
      {children}
    </div>
  </FlexRow>;
};
