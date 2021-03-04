

const jsmain = () => {
  const WrapperUtil = fn => {
    return (...args) => {
      try {
        const result = fn(...args);
        console.log('Wrapped Fn with args', ...args, 'result: ', result);
        return result;
      } catch (error) {
        console.log('Error', error);
      }
    };
  };

  console.log('-------- START JS MAIN -----------');
  const wrappedFn = WrapperUtil((a, b) => a + b);
  wrappedFn(2, 2);
  console.log('-------- END JS MAIN -------------');
};
jsmain();

console.log('\n\n');

const tsmain = () => {

  // type wrappedGen

  const WrapperUtil = fn => {
    return (...args) => {
      try {
        const result = fn(...args);
        console.log('Wrapped Fn with args', ...args, 'result: ', result);
        return result;
      } catch (error) {
        console.log('Error', error);
      }
    };
  };

  console.log('-------- START TS MAIN -----------');
  const wrappedFn = WrapperUtil((a, b) => a + b);
  wrappedFn(2, 2);
  console.log('-------- END TS MAIN -------------');
};
tsmain();
