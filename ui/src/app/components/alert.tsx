import * as React from 'react';

import { Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withStyle } from 'app/utils';

export const styles = reactStyles({
  alert: {
    fontSize: '.54167rem',
    letterSpacing: 'normal',
    lineHeight: '.75rem',
    position: 'relative',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'row',
    width: 'auto',
    borderRadius: '.125rem',
    marginTop: '.25rem',
    background: colors.light,
    color: colors.dark,
    border: `1px solid ${colors.secondary}`,
  },

  danger: {
    background: colorWithWhiteness(colors.danger, 0.8),
    color: colors.dark,
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.6)}`,
  },
  info: {
    background: colorWithWhiteness(colors.secondary, 0.75),
    color: colors.primary,
    border: `1px solid ${colorWithWhiteness(colors.primary, 0.65)}`,
    fontSize: '11px',
    padding: '.2rem',
  },
  warning: {
    background: colors.warning,
    color: colors.white,
    border: 'none',
    padding: '.2rem',
  },
});

export const Alert = withStyle(styles.alert)('div');
export const AlertDanger = withStyle(styles.danger)(Alert);
export const AlertInfo = withStyle(styles.info)(Alert);
export const AlertWarning = withStyle(styles.warning)(Alert);
export const AlertClose = ({ style = {}, ...props }) => {
  return (
    <Clickable style={{ ...style, height: '16px' }} {...props}>
      <ClrIcon shape='times' />
    </Clickable>
  );
};
