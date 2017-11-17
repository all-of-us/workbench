import {Component, DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';

export function updateAndTick(fixture: any) {
  fixture.detectChanges();
  tick();
}

export function simulateInput(
    fixture: ComponentFixture<Component>,
    element: DebugElement,
    text: string) {
  element.nativeNode.value = text;
  element.nativeNode.dispatchEvent(new Event('input'));
  updateAndTick(fixture);
}
