import {Component} from '@angular/core';
import {Spinner} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withStyle} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';

import * as React from 'react';

export interface NihCallbackState {
  error: boolean;
  errorMessage: string;
}

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
    padding: '0 0.5rem',
    fontWeight: 600,
    color: '#2F2E7E',
    marginTop: '0.2rem',
    width: '90%'
  }
});

export const Error = withStyle(styles.error)('div');
const spinner = <div style={styles.overlay}><div style={styles.square}><Spinner /></div></div>;

export class NihCallback extends React.Component<{}, NihCallbackState> {

  constructor(props) {
    super(props);
    this.state = {
      error: false,
      errorMessage: ''
    };
  }

  async componentDidMount() {
    const search: string = window.location.search;
    if (search.length > 1) {
      // The `search` component of `location` starts with `?`
      const token = window.location.search.replace('?', '');
      try {
        const response = await profileApi().updateNihToken({ jwt: token });
        this.navigateHome();
      } catch (e) {
        this.setState({error: true, errorMessage: 'Error saving NIH Authentication'});
      }
    } else {
      this.setState({error: true, errorMessage: 'An NIH Authentication token is required.'});
    }
  }

  navigateHome() {
    navigateByUrl('/');
  }

  render() {
    const error = <Error>Error Linking NIH Username: {this.state.errorMessage}
      <div onClick={this.navigateHome} style={{cursor: 'pointer'}}>Please try linking again.</div>
    </Error>;

    return (this.state.error ? error : spinner);
  }

}

/**
 * The general flow for this component is:
 *  1. Act as a destination endpoint (callback url) after user logs into nih
 *  2. Grab the nih token from the redirected callback request
 *  3. Post nih token to the profile service
 *  4. Redirect user to the home page
 *  5. Display any errors
 */
@Component({
  template: '<div #root></div>'
})
export class NihCallbackComponent extends ReactWrapperBase {

  constructor() { super(NihCallback, []); }

}
