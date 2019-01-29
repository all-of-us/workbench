import {Component} from '@angular/core';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';

import * as React from 'react';

export class NihCallback extends React.Component<{}, {}> {

  private static async updateNihToken(encodedKey) {
    try {
      await profileApi().updateNihToken({ jwt: encodedKey });
    } catch (e) {
      console.error(e);
    }
    window.location.href = '/';
  }

  constructor(props) {
    super(props);
  }

  componentDidMount(): void {
    console.log(JSON.stringify(window.location));
    const search: string = window.location.search;
    if (search.length > 1) {
      // The `search` component of location starts with `?`
      const token = window.location.search.replace('?', '');
      NihCallback.updateNihToken(token);
    }
  }

  render() {
    return null;
  }

}

/**
 * The general flow for this component is:
 *  1. Act as a destination endpoint after user logs into nih
 *  2. Grab the nih token from the redirected callback request
 *  3. Post nih token to the profile service
 *  4. Redirect user to the home page
 */
@Component({
  template: '<div #root></div>'
})
export class NihCallbackComponent extends ReactWrapperBase {

  constructor() { super(NihCallback, []); }

}
