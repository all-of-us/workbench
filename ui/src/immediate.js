



async function immediatePromise(v) {
  return new Promise(resolve => setImmediate(resolve));
}

async function timeoutPromise() {
  return new Promise(resolve => setTimeout(resolve, 0));
}

function immediate(v) {
  setImmediate(() => console.log('Immediate: ', v))
}

function timeout(v) {
  setTimeout(() => console.log('Timeout: ', v), 0)
}

async function main () {
  console.log('Start Run'.padStart(25, '-').padEnd(50, '-'))
  for (let i = 0; i < 10; i++) {
    // You can play around with different combinations of timeout/immediate to see the different behaviors
    // In general immediate is not deterministic when combined with timeout
    timeout(i)
    immediate(i)
    // await timeoutPromise() 
    // await immediatePromise()
    // console.log('After Immediate: ', i)
  }
}

main();
