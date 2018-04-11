import {DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';

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

export function queryByCss<C>(
    fixture: ComponentFixture<C>,
    css: string) {
  return fixture.debugElement.query(By.css(css));
}

export function queryAllByCss<C>(
    fixture: ComponentFixture<C>,
    css: string) {
  return fixture.debugElement.queryAll(By.css(css));
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
