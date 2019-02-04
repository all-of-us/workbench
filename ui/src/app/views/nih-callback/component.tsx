import {Component} from '@angular/core';
import {Spinner} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withStyle} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';

import * as React from 'react';

const styles = reactStyles({
  overlay: {
    backgroundColor: 'transparent',
    position: 'absolute', top: 0, left: 0, bottom: 0, right: 0,
    display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  square: {
    display: 'flex', backgroundColor: 'transparent', borderRadius: 4, padding: '0.5rem'
  },
  error: {
    fontWeight: 600,
    color: '#2F2E7E',
    margin: '1rem'
  }
});

export const Error = withStyle(styles.error)('div');
const spinner = <div style={styles.overlay}><div style={styles.square}><Spinner /></div></div>;

export interface NihCallbackState {
  errorMessage: string;
}

export class NihCallback extends React.Component<{}, NihCallbackState> {

  constructor(props) {
    super(props);
    this.state = {
      errorMessage: ''
    };
  }

  async componentDidMount() {
    // Assumes callback url has format of `/nih-callback?token=XYZ`
    const token = (new URL(window.location.href)).searchParams.get('token');
    if (token) {
      try {
        await profileApi().updateNihToken({ jwt: token });
        this.navigateHome();
      } catch (e) {
        this.setErrorStatus('Error saving NIH Authentication status');
      }
    } else {
      this.setErrorStatus('An NIH Authentication token is required.');
    }
  }

  private setErrorStatus(message: string) {
    this.setState({errorMessage: message});
  }

  private navigateHome() {
    navigateByUrl('/');
  }

  render() {
    const error = <Error>Error Linking NIH Username: {this.state.errorMessage}
      <a href={'#'} style={{marginLeft: '.25rem'}} onClick={this.navigateHome}>
        Please try linking again.
      </a>
    </Error>;

    return (this.state.errorMessage ? error : spinner);
  }

}

/**
 * The general flow for this component is:
 *  1. Act as a destination endpoint (callback url) after user logs into nih
 *  2. Grab the nih token from the redirected callback request
 *  3. Post nih token to the profile service
 *  4. Redirect user to the home page if successful
 *  5. Display any errors
 */
@Component({
  template: '<app-signed-in><div #root></div></app-signed-in>'
})
export class NihCallbackComponent extends ReactWrapperBase {

  constructor() { super(NihCallback, []); }

}
