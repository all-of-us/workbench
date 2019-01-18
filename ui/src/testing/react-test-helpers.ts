import {ReactWrapper} from 'enzyme';

// This file is necessary because angular imports complain if there
// is no zone, regardless of whether the imports are used.
// The error is from:
//   import {ComponentFixture, tick} from '@angular/core/testing';
// And fails with:
//   ReferenceError: Zone is not defined
export async function completeApiCall(wrapper: ReactWrapper) {
  await new Promise(setImmediate).then(() => wrapper.update());
}
