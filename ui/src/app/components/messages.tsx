import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';

const MessageWithIcon = ({
  messageType = 'warning' || 'error' || 'success',
  iconSize = 30,
  iconPosition = 'top' || 'center' || 'bottom',
  children
}) => {
  const icon = {
    warning: 'warning-standard',
    error: 'warning-standard',
    success: 'success-standard',
  };

  const color = {
    warning: colors.warning,
    error: colors.danger,
    success: colors.success
  };

  const position = {
    top: {alignSelf: 'flex-start'},
    // not necessary bc of top level align-items, but this does make it explicit what the default does
    center: {alignSelf: 'center'},
    bottom: {alignSelf: 'flex-end'}
  };

  return <FlexRow
      style={{
        alignItems: 'center',
        backgroundColor: colorWithWhiteness(color[messageType], .9),
        border: `1px solid ${color[messageType]}`,
        borderRadius: '5px',
        color: colors.dark,
        marginTop: '.5rem',
        padding: '.5rem 0px',
      }}
  >
    <ClrIcon
        style={{
          color: color[messageType],
          flex: '0 0 auto',
          marginLeft: '.5rem',
          ...position[iconPosition]
        }}
        shape={icon[messageType]}
        size={iconSize}
        class={'is-solid'}
    />
    <div style={{paddingLeft: '0.5rem', paddingRight: '0.5rem'}}>
      {children}
    </div>
  </FlexRow>;
};

export const WarningMessage = ({iconSize = 30, iconPosition = 'center', children}) => {
  return <MessageWithIcon
      messageType={'warning'}
      iconSize={iconSize}
      iconPosition={iconPosition}
  >
    {children}
  </MessageWithIcon>;
};

export const ErrorMessage = ({iconSize = 30, iconPosition = 'center', children}) => {
  return <MessageWithIcon
      messageType={'error'}
      iconSize={iconSize}
      iconPosition={iconPosition}
    >
    {children}
  </MessageWithIcon>;
};
