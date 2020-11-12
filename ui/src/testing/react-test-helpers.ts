import {ReactWrapper} from 'enzyme';
import {act} from 'react-dom/test-utils';

// This file is necessary because angular imports complain if there
// is no zone, regardless of whether the imports are used.
// The error is from:
//   import {ComponentFixture, tick} from '@angular/core/testing';
// And fails with:
//   ReferenceError: Zone is not defined
export async function waitOneTickAndUpdate(wrapper: ReactWrapper) {
  await new Promise(setImmediate).then(() => wrapper.update());
}

// Invokes react "act" in order to handle async component updates: https://reactjs.org/docs/testing-recipes.html#act
// This code waits for all updates to complete.
// There is probably a better way to handle this - but it may mean not using enzyme
export const handleUseEffect = async(component) => {
  await act(async() => {
    await Promise.resolve(component); // Wait for the component to finish rendering (mount returns a promise)
    await new Promise(resolve => setImmediate(resolve)); // Wait for all outstanding requests to complete
  });
};
