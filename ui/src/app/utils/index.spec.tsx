import * as Utils from 'app/utils';
import * as fp from 'lodash/fp';


describe('Helper functions', () => {
  it('(cond) Should handle conditionals and defaults correctly', async() => {
    const test = (x, y) => {
      return [
        Utils.cond<number | string>([x === 5, () => 1], [y === 10, () => 2], () => 'default'),
        Utils.cond<number | string>([x === 4, () => 1], [y === 10, () => 2], () => 'default'),
        Utils.cond<number | string>([x === 4, () => 1], [y === 9, () => 2], () => 'default')
      ]
    }

    const [r1, r2, r3] = test(5, 10);
    expect(r1).toBe(1);
    expect(r2).toBe(2);
    expect(r3).toBe('default');
  });

  it('(maybe) Should return nothing when the value is not set and execute the function when it is', async() => {
    const setFirstName = data => ({...data, firstName: 'first'});
    const setLastName = data => ({...data, lastName: 'last'});

    const r1 = Utils.maybe(setFirstName, null);
    expect(r1).toEqual(Utils.nothing);

    const r2 = Utils.maybe(setFirstName, {})
    expect(r2).toEqual({firstName: 'first'})

    const r3 = fp.flow(Utils.maybe(setFirstName), Utils.maybe(setLastName))({})
    expect(r3).toEqual({firstName: 'first', lastName: 'last'})
  });
});

