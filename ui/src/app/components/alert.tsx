import {reactStyles, withStyle} from 'app/utils';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import * as React from 'react';

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
    background: '#e1f1f6',
    color: '#565656',
    border: '1px solid #49afd9'
  },

  danger: {
    background: '#f5dbd9',
    color: '#565656',
    border: '1px solid #ebafa6'
  },
  warning: {
    background: '#FFFCEB',
    color: '#E28327',
    border: 'none',
    padding: '.2rem'
  }
});

export const Alert = withStyle(styles.alert)('div');
export const AlertDanger = withStyle(styles.danger)(Alert);
export const AlertWarning = withStyle(styles.warning)(Alert);
export const AlertClose = ({style = {}, ...props}) => {
  return <Clickable style={{...style, width: '8%', height: '16px'}}
                    {...props}>
    <ClrIcon shape='times'/>
  </Clickable>;
};
