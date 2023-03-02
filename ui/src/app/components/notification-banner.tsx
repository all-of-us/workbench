import * as React from 'react';
import { CSSProperties } from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { Button, ButtonWithLocationState } from './buttons';
import { FlexRow } from './flex';
import { AlarmExclamation } from './icons';

const styles = reactStyles({
  box: {
    boxSizing: 'border-box',
    height: '56.5px',
    border: '0.5px solid rgba(38,34,98,0.5)',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.warning, 0.9),
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 'auto',
    gap: '24.5px',
    paddingLeft: '24.5px',
    paddingRight: '24.5px',
  },
  icon: {
    height: '25px',
    width: '25px',
    color: colors.warning,
    fontFamily: 'Font Awesome 5 Pro',
    fontSize: '25px',
    letterSpacing: 0,
    lineHeight: '25px',
    alignItems: 'center',
  },
  text: {
    height: '40px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '20px',
  },
  button: {
    boxSizing: 'border-box',
    height: '30px',
    width: '102px',
    border: '0.8px solid #216FB4',
    borderRadius: '1.6px',
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
  text: string | JSX.Element;
  buttonText: string;
  buttonPath?: string;
  buttonDisabled?: boolean;
  useLocationLink?: boolean;
  boxStyle?: CSSProperties;
  textStyle?: CSSProperties;
  buttonStyle?: CSSProperties;
  buttonOnClick?: () => void;
  bannerTextWidth: CSSProperties['width'];
  buttonAriaLabel?: string;
  iconStyle?: CSSProperties;
}

export const NotificationBanner = ({
  dataTestId,
  text,
  buttonText,
  buttonPath,
  buttonDisabled,
  useLocationLink,
  boxStyle,
  textStyle,
  buttonStyle,
  buttonOnClick,
  buttonAriaLabel,
  bannerTextWidth,
  iconStyle,
}: NotificationProps) => {
  return (
    <FlexRow data-test-id={dataTestId} style={{ ...styles.box, ...boxStyle }}>
      <AlarmExclamation style={{ ...styles.icon, ...iconStyle }} />
      <div style={{ ...styles.text, ...textStyle, width: bannerTextWidth }}>
        {text}
      </div>
      {useLocationLink ? (
        <ButtonWithLocationState
          type='primary'
          style={{ ...styles.button, ...buttonStyle }}
          path={buttonPath}
          disabled={buttonDisabled}
          onClick={buttonOnClick}
          aria-label={buttonAriaLabel}
        >
          <div style={styles.buttonText}>{buttonText}</div>
        </ButtonWithLocationState>
      ) : (
        <Button
          type='primary'
          style={styles.button}
          path={buttonPath}
          disabled={buttonDisabled}
          onClick={buttonOnClick}
          aria-label={buttonAriaLabel}
        >
          <div style={styles.buttonText}>{buttonText}</div>
        </Button>
      )}
    </FlexRow>
  );
};
