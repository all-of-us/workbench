import * as React from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { Button } from './buttons';
import { FlexRow } from './flex';
import { AlarmExclamation } from './icons';

const styles = reactStyles({
  box: {
    boxSizing: 'border-box',
    height: '56.5px',
    width: '380.5px',
    border: '0.5px solid rgba(38,34,98,0.5)',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.warning, 0.9),
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 'auto',
  },
  icon: {
    height: '25px',
    width: '25px',
    color: colors.warning,
    fontFamily: 'Font Awesome 5 Pro',
    fontSize: '25px',
    letterSpacing: 0,
    lineHeight: '25px',
    marginLeft: 'auto',
  },
  text: {
    height: '40px',
    width: '177px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '20px',
    marginLeft: 'auto',
  },
  button: {
    boxSizing: 'border-box',
    height: '30px',
    width: '102px',
    border: '0.8px solid #216FB4',
    borderRadius: '1.6px',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  buttonText: {
    height: '20px',
    width: '79.6px',
    fontFamily: 'Montserrat',
    fontSize: '11.2px',
    fontWeight: 500,
    letterSpacing: '-0.24px',
    lineHeight: '19.2px',
    textAlign: 'center',
  },
});

interface NotificationProps {
  dataTestId: string;
  text: string;
  buttonText: string;
  buttonPath: string;
  buttonDisabled: boolean;
}

export const NotificationBanner = ({
  dataTestId,
  text,
  buttonText,
  buttonPath,
  buttonDisabled,
}: NotificationProps) => {
  return (
    <FlexRow data-test-id={dataTestId} style={styles.box}>
      <AlarmExclamation style={styles.icon} />
      <div style={styles.text}>{text}</div>
      <Button
        type='primary'
        style={styles.button}
        path={buttonPath}
        disabled={buttonDisabled}
      >
        <div style={styles.buttonText}>{buttonText}</div>
      </Button>
    </FlexRow>
  );
};
