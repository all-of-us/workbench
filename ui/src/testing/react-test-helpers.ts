import {ReactWrapper} from 'enzyme';


// This file is necessary because angular imports complain if there
// is no zone, regardless of whether the imports are used.
export async function completeApiCall(wrapper: ReactWrapper) {
  await new Promise(setImmediate).then(() => wrapper.update());
  await new Promise(setImmediate).then(() => wrapper.update());
}
