import * as fp from 'lodash/fp';

import * as Utils from 'app/utils';
import { usernameWithoutDomain } from 'app/utils';

describe('Helper functions', () => {
  it('(maybe) Should return nothing when the value is not set and execute the function when it is', async () => {
    const setFirstName = (data) => ({ ...data, firstName: 'first' });
    const setLastName = (data) => ({ ...data, lastName: 'last' });

    const r1 = Utils.maybe(setFirstName, null);
    expect(r1).toEqual(Utils.nothing);

    const r2 = Utils.maybe(setFirstName, {});
    expect(r2).toEqual({ firstName: 'first' });

    const r3 = fp.flow(Utils.maybe(setFirstName), Utils.maybe(setLastName))({});
    expect(r3).toEqual({ firstName: 'first', lastName: 'last' });
  });

  it('(usernameWithoutDomain) Should normalize usernames appropriately', () => {
    expect(usernameWithoutDomain('user@google.com')).toEqual('user');
    expect(usernameWithoutDomain('user')).toEqual('user');
    expect(usernameWithoutDomain(undefined)).toEqual('');
    expect(usernameWithoutDomain(null)).toEqual('');
  });
});
