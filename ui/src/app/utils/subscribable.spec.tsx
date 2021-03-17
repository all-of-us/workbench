import { atom } from './subscribable';

export const delay = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

const promiseComplete = () => {
  let resolveHandler;
  const promise = new Promise<void>((resolve) => {
    resolveHandler = value => value && resolve();
  });
  return {promise, resolveHandler};
};

const makeSubscribeFn = (assertions): [() => Promise<void>, (newValue: any, oldValue: any) => Promise<{}>] => {
  const {promise, resolveHandler} = promiseComplete();
  const promiseHandler = (): Promise<void> => promise;

  const subscribeFn = (newValue, oldValue) => new Promise((resolve) => {
    assertions.forEach(assertion => assertion(newValue, oldValue));
    resolveHandler(true);
    resolve(true);
  });

  return [promiseHandler, subscribeFn];
};

describe('atom', () => {

  it('should initialize with a value', () => {
    const testAtom = atom({value: 1});
    expect(testAtom.get()).toEqual({value: 1});
  });

  it('should set the value', () => {
    const testAtom = atom({value: 1});
    testAtom.set({value: 5});
    expect(testAtom.get()).toEqual({value: 5});
  });

  it('should call a single subscribed function', async() => {
    const testAtom = atom({value: 1});
    const [complete, subscribeFn] = makeSubscribeFn([
      newValue => expect(newValue).toEqual({value: 10}),
      ({}, oldValue) => expect(oldValue).toEqual({value: 20})
    ]);

    testAtom.set({value: 20});
    testAtom.subscribe(subscribeFn);
    testAtom.set({value: 10});

    await complete();
  });

  it('should call a multiple subscribed functions with new and old values', async() => {
    const testAtom = atom({value: 1});
    const assertions = [
      newValue => expect(newValue).toEqual({value: 10}),
      ({}, oldValue) => expect(oldValue).toEqual({value: 20})
    ];

    const [complete1, subscribeFn1] = makeSubscribeFn(assertions);
    const [complete2, subscribeFn2] = makeSubscribeFn(assertions);
    const [complete3, subscribeFn3] = makeSubscribeFn(assertions);

    testAtom.set({value: 20});
    testAtom.subscribe(subscribeFn1);
    testAtom.subscribe(subscribeFn2);
    testAtom.subscribe(subscribeFn3);
    testAtom.set({value: 10});

    await complete1();
    await complete2();
    await complete3();
  });

  it('should not call an unsubscribed function', async() => {
    const testAtom = atom({value: 1});
    let numberOfCalledSubscriptions = 0;
    const subscriber = [() => numberOfCalledSubscriptions += 1];

    const [complete1, subscribeFn1] = makeSubscribeFn(subscriber);
    const [, subscribeFn2] = makeSubscribeFn(subscriber);
    const [complete3, subscribeFn3] = makeSubscribeFn(subscriber);

    testAtom.set({value: 20});
    testAtom.subscribe(subscribeFn1);

    const sub2 = testAtom.subscribe(subscribeFn2);
    sub2.unsubscribe();

    testAtom.subscribe(subscribeFn3);
    testAtom.set({value: 10});

    await complete1();
    await complete3();
    await delay(50);

    expect(numberOfCalledSubscriptions).toBe(2);
  });

  it('should not call any functions when all have unsubscribed', async() => {
    const testAtom = atom({value: 1});
    let numberOfCalledSubscriptions = 0;
    const subscriber = [() => numberOfCalledSubscriptions += 1];

    const [, subscribeFn1] = makeSubscribeFn(subscriber);
    const [, subscribeFn2] = makeSubscribeFn(subscriber);
    const [, subscribeFn3] = makeSubscribeFn(subscriber);

    testAtom.set({value: 20});

    const sub1 = testAtom.subscribe(subscribeFn1);
    const sub2 = testAtom.subscribe(subscribeFn2);
    const sub3 = testAtom.subscribe(subscribeFn3);

    sub1.unsubscribe();
    sub2.unsubscribe();
    sub3.unsubscribe();

    testAtom.set({value: 10});

    await delay(50);
    expect(numberOfCalledSubscriptions).toBe(0);
  });

  it('should call remaining subscribers when one throws', async() => {
    const testAtom = atom({value: 1});
    let numberOfCalledSubscriptions = 0;
    const subscriber = [() => numberOfCalledSubscriptions += 1];

    const [, subscribeFn2] = makeSubscribeFn(subscriber);
    const [, subscribeFn3] = makeSubscribeFn(subscriber);

    testAtom.subscribe(() => {throw new Error('error'); });
    testAtom.subscribe(subscribeFn2);
    testAtom.subscribe(subscribeFn3);
    testAtom.set({value: 10});

    await delay(50);
    expect(numberOfCalledSubscriptions).toBe(2);
  });
});
