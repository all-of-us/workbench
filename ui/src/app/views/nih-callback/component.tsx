import {Component} from '@angular/core';
import {styles} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withStyle} from 'app/utils';

import * as React from 'react';

export interface NihCallbackState {
  error: boolean;
  errorMessage: string;
}

export const Error = withStyle(styles.error)('div');

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
        await profileApi().updateNihToken({ jwt: token });
        window.location.assign('/');
      } catch (e) {
        this.setState({error: true, errorMessage: e});
      }
    } else {
      this.setState({error: true, errorMessage: 'Invalid callback token provided.'});
    }
  }

  render() {
    const error = <Error>Error Linking NIH Username: {this.state.errorMessage}</Error>;
    return (this.state.error ? error : <SpinnerOverlay />);
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
