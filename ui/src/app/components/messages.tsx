import * as React from 'react';

import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, { colorWithWhiteness } from 'app/styles/colors';

const MessageWithIcon = ({
  messageType,
  iconSize = 30,
  iconPosition,
  children,
}: {
  messageType: 'warning' | 'error' | 'info';
  iconSize: number;
  iconPosition: 'top' | 'center' | 'bottom';
  children;
}) => {
  const icon = {
    warning: 'warning-standard',
    error: 'warning-standard',
    info: 'info-standard',
  };

  const color = {
    warning: colors.warning,
    error: colors.danger,
    info: colors.secondary,
  };

  const position = {
    top: { alignSelf: 'flex-start' },
    // not necessary bc of top level align-items, but this does make it explicit what the default does
    center: { alignSelf: 'center' },
    bottom: { alignSelf: 'flex-end' },
  };

  return (
    <FlexRow
      style={{
        alignItems: 'center',
        backgroundColor: colorWithWhiteness(color[messageType], 0.9),
        border: `1px solid ${color[messageType]}`,
        borderRadius: '5px',
        color: colors.dark,
        marginTop: '.75rem',
        padding: '.75rem 0px',
      }}
    >
      <ClrIcon
        style={{
          color: color[messageType],
          flex: '0 0 auto',
          marginLeft: '.75rem',
          ...position[iconPosition],
        }}
        shape={icon[messageType]}
        size={iconSize}
        class={'is-solid'}
      />
      <div style={{ paddingLeft: '0.75rem', paddingRight: '0.75rem' }}>
        {children}
      </div>
    </FlexRow>
  );
};

export const WarningMessage = ({
  iconSize = 30,
  iconPosition = 'center',
  children,
}: {
  iconSize?: number;
  iconPosition?: 'top' | 'center' | 'bottom';
  children: string | React.ReactNode;
}) => {
  return (
    <MessageWithIcon
      messageType={'warning'}
      iconSize={iconSize}
      iconPosition={iconPosition}
    >
      {children}
    </MessageWithIcon>
  );
};

export const ErrorMessage = ({
  iconSize = 30,
  iconPosition = 'center',
  children,
}: {
  iconSize?: number;
  iconPosition?: 'top' | 'center' | 'bottom';
  children: string | React.ReactNode;
}) => {
  return (
    <MessageWithIcon
      messageType={'error'}
      iconSize={iconSize}
      iconPosition={iconPosition}
    >
      {children}
    </MessageWithIcon>
  );
};

export const InfoMessage = ({
  children,
}: {
  children: string | React.ReactNode;
}) => {
  return (
    <MessageWithIcon messageType={'info'} iconSize={20} iconPosition='top'>
      {children}
    </MessageWithIcon>
  );
};
