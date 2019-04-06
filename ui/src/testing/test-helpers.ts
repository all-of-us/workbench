import {DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';

import * as ReactTestUtils from 'react-dom/test-utils';

/** Modal usage requires the existence of a global div. */
export function setupModals<C>(fixture: ComponentFixture<C>) {
  const popupRoot = document.createElement('div');
  popupRoot.id = 'popup-root';
  fixture.nativeElement.appendChild(popupRoot);
}

export function updateAndTick<C>(fixture: ComponentFixture<C>) {
  fixture.detectChanges();
  tick();
}

export function simulateInput<C>(
  fixture: ComponentFixture<C>,
  element: DebugElement,
  text: string) {
  element.nativeNode.value = text;
  element.nativeNode.dispatchEvent(new Event('input'));
  updateAndTick(fixture);
}

export function simulateEvent<C>(
  fixture: ComponentFixture<C>,
  element: DebugElement,
  eventType: string) {
  element.triggerEventHandler(eventType, null);
  updateAndTick(fixture);
}

export function simulateClick<C>(
  fixture: ComponentFixture<C>,
  element: DebugElement) {
  simulateEvent(fixture, element, 'click');
}

export function simulateInputReact<C>(
  fixture: ComponentFixture<C>,
  selector: string,
  text: string
) {
  const el = fixture.debugElement.nativeElement.querySelector(selector);
  el.value = text;
  ReactTestUtils.Simulate.change(el);
  updateAndTick(fixture);
}

export function simulateClickReact<C>(
  fixture: ComponentFixture<C>,
  selector: string
) {
  const el = fixture.debugElement.nativeElement.querySelector(selector);
  ReactTestUtils.Simulate.click(el);
  updateAndTick(fixture);
}

export function simulateMultipleElementClickReact<C>(
  fixture: ComponentFixture<C>,
  selector: string,
  index: number
) {
  const el = findElementsReact(fixture, selector)[index];
  ReactTestUtils.Simulate.click(el);
  updateAndTick(fixture);
}

export function findElementsReact<C>(
  fixture: ComponentFixture<C>,
  selector: string
) {
  return [].slice.call(fixture.debugElement.nativeElement.querySelectorAll(selector));
}
