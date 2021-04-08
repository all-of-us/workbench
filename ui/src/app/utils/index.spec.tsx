import * as Utils from 'app/utils';


describe('Data Access Panel', () => {
  it('Should handle conditionals and defaults correctly', async() => {
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
  })
});

