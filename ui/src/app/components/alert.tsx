import {reactStyles} from 'app/utils';

import {withStyle} from 'app/utils/index';

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
  }
});

export const Alert = withStyle(styles.alert)('div');
export const AlertDanger = withStyle(styles.danger)(Alert);
