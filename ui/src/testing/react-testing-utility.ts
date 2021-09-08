import {ComponentFixture} from '@angular/core/testing';
import * as ReactTestUtils from 'react-dom/test-utils';
import {updateAndTick} from './test-helpers';

export function simulateInput<C>(
  fixture: ComponentFixture<C>,
  selector: string,
  text: string
) {
  const el = fixture.debugElement.nativeElement.querySelector(selector);
  el.value = text;
  ReactTestUtils.Simulate.change(el);
  updateAndTick(fixture);
}

export function simulateClick<C>(
  fixture: ComponentFixture<C>,
  selector: string
) {
  const el = fixture.debugElement.nativeElement.querySelector(selector);
  ReactTestUtils.Simulate.click(el);
  updateAndTick(fixture);
}

export function simulateClickNthElement<C>(
  fixture: ComponentFixture<C>,
  selector: string,
  index: number
) {
  const el = findElements(fixture, selector)[index];
  ReactTestUtils.Simulate.click(el);
  updateAndTick(fixture);
}

export function findElements<C>(
  fixture: ComponentFixture<C>,
  selector: string
) {
  return [].slice.call(fixture.debugElement.nativeElement.querySelectorAll(selector));
}
