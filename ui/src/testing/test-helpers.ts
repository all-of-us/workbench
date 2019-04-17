import {DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';

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
