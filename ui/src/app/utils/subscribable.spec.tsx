import { atom } from './subscribable';

export const delay = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

const flagPromise = () => {
  let setter;
  const promise = new Promise((resolve) => {
    setter = value => value && resolve();
  });
  return {promise, setter};
};

const makeSubscribeFn = (assertions) => {
  const {promise, setter} = flagPromise();
  const subscribeFn = (...args) => new Promise((resolve) => {
    assertions.forEach(assertion => assertion(...args));
    setter(true);
    resolve();
  });

  return [() => promise, subscribeFn];
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

  it('should call a multiple subscribed functions', async() => {
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

  // it('should not call an unsubscribed function', async() => {
  //   const testAtom = atom({value: 1});
  //   const assertions = [
  //     newValue => expect(newValue).toEqual({value: 10}),
  //     ({}, oldValue) => expect(oldValue).toEqual({value: 20})
  //   ];

  //   const [complete1, subscribeFn1] = makeSubscribeFn(assertions);
  //   const [complete2, subscribeFn2] = makeSubscribeFn(assertions);
  //   const [complete3, subscribeFn3] = makeSubscribeFn(assertions);

  //   testAtom.set({value: 20});
  //   testAtom.subscribe(subscribeFn1);
  //   const {unsubscribe} = testAtom.subscribe(subscribeFn2);
  //   testAtom.subscribe(subscribeFn3);
  //   testAtom.set({value: 10});

  //   unsubscribe();

  //   await complete1();
  //   await complete2();
  //   await complete3();
  // });

  // it('should not call any functions when all have unsubscribed', async() => {
  //   const failedFunctionSpy = spyOn(functionStub, 'failedFunction').and.callThrough();
  //   await apiCallWithGatewayTimeoutRetries(() => functionStub.failedFunction(), 3, 1).catch(() => {});
  //   expect(failedFunctionSpy).toHaveBeenCalledTimes(4);
  // });

});
