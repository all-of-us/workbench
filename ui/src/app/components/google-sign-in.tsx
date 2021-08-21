import {Button} from 'app/components/buttons';
import {reactStyles} from 'app/utils';

import {signIn} from 'app/utils/authentication';
import * as React from 'react';

export const styles = reactStyles({
  button: {
    marginTop: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    height: 'auto',
    paddingLeft: '0',
    fontSize: '18px',
    fontStyle: 'normal',
    textTransform: 'none',
    borderRadius: '2px',
    justifyContent: 'baseline',
    width: '6.5rem'
  },
});

export const GoogleSignInButton: React.FunctionComponent = () =>
  <Button type='primary' style={styles.button} onClick={() => signIn()}>
    <img src='/assets/icons/google-icon.png'
         style={{ height: '54px', width: '54px', margin: '-3px 19px -3px -3px'}}/>
    <div>
      Sign In
    </div>
  </Button>;
