import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {FlexColumn, FlexRow} from './flex';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, switchCase} from 'app/utils';
import {ClrIcon, WarningStandardIcon} from './clr-icons';

const styles = reactStyles({
  infoBanner: {
    backgroundColor: colorWithWhiteness(colors.accent, .9),
    color: colors.primary,
    fontSize: '12px',
    marginRight: '1rem',
    marginTop: '1rem',
    padding: '0.5rem',
    width: '300px',
    borderRadius: '0.5em',
    position: 'absolute',
    top: '0',
    right: '0',
  },
  title: {
    fontWeight: 600,
    fontSize: '14px',
    width: '80%',
    lineHeight: '18px',
  },
  message: {
    lineHeight: '20px',
    marginTop: '.3rem',
    paddingRight: '.2rem',
    fontSize: '14px',
  },
  footer: {
    marginTop: '.5rem'
  },
  closeIcon: {
    position: 'absolute',
    top: '.3rem',
    right: '.3rem',
    colors: colors.accent
  },
  warningIcon: {
    color: colors.warning,
    flex: '0 0 auto',
    marginRight: '0.4rem',
  },
});

const warningIcon = <WarningStandardIcon
  class='is-solid'
  size={26}
  style={styles.warningIcon}
/>;

export enum ToastType {
  INFO,
  WARNING
}

const styleForType = (toastType: ToastType, zIndex): React.CSSProperties => switchCase(toastType,
  [ToastType.INFO, () => ({
    ...styles.infoBanner,
    zIndex
  })],
  [ToastType.WARNING, () => ({
    ...styles.infoBanner,
    zIndex,
    backgroundColor: colorWithWhiteness(colors.highlight, .5),
  })],
);

interface ToastProps {
  title: string;
  message: string | JSX.Element;
  onClose: Function;
  toastType: ToastType;
  zIndex: any;  // TODO better type
  footer?: string | JSX.Element;
}
export const ToastBanner = (props: ToastProps) => {
  const {title, message, onClose, toastType, zIndex, footer} = props;
  return ReactDOM.createPortal(
    <FlexColumn style={styleForType(toastType, zIndex)}>
      <FlexRow style={{alignItems: 'center', marginTop: '.1rem'}}>
        {toastType === ToastType.WARNING && warningIcon}
        <div style={styles.title}>{title}</div>
      </FlexRow>
      <div style={styles.message}>{message}</div>
      <div style={styles.footer}>{footer}</div>
      <ClrIcon
        shape={'times'}
        size={20}
        style={styles.closeIcon}
        onClick={() => onClose()}
      />
    </FlexColumn>,
    document.getElementsByTagName('body')[0]);
}
