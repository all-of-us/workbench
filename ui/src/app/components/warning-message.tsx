import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';

export const WarningMessage = ({iconSize = 30, iconPosition = 'center', children}) => {
  const position = {
    top: {alignSelf: 'flex-start'},
    // not necessary bc of top level align-items, but this does make it explicit what the default does
    center: {alignSelf: 'center'},
    bottom: {alignSelf: 'flex-end'}
  };

  return <FlexRow
    style={{
      alignItems: 'center',
      backgroundColor: colorWithWhiteness(colors.warning, .9),
      border: `1px solid ${colors.warning}`,
      borderRadius: '5px',
      color: colors.dark,
      marginTop: '.5rem',
      padding: '.5rem 0px',
    }}
  >
    <ClrIcon
      style={{
        color: colors.warning,
        flex: '0 0 auto',
        marginLeft: '.5rem',
        ...position[iconPosition]
      }}
      shape={'warning-standard'}
      size={iconSize}
      class={'is-solid'}
    />
    <div style={{paddingLeft: '0.5rem', paddingRight: '0.5rem'}}>
      {children}
    </div>
  </FlexRow>;
};
