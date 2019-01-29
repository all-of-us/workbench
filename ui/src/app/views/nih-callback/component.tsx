import {Component} from '@angular/core';
import {c} from "@angular/core/src/render3";
import {el} from "@angular/platform-browser/testing/src/browser_util";
import {styles} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withStyle} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';

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
