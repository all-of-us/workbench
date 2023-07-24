import * as React from 'react';

import { Button } from 'app/components/buttons';
import { reactStyles } from 'app/utils';
import { signIn } from 'app/utils/authentication';
import googleIcon from 'assets/icons/google-icon.png';

export const styles = reactStyles({
  button: {
    marginTop: '0.75rem',
    display: 'flex',
    alignItems: 'center',
    height: 'auto',
    paddingLeft: '0',
    fontSize: '18px',
    fontStyle: 'normal',
    textTransform: 'none',
    borderRadius: '2px',
    justifyContent: 'baseline',
    width: '9.75rem',
  },
});

export const GoogleSignInButton = () => (
  <Button type='primary' style={styles.button} onClick={() => signIn()}>
    <img
      src={googleIcon}
      style={{ height: '54px', width: '54px', margin: '-3px 19px -3px -3px' }}
    />
    <div>Sign In</div>
  </Button>
);
