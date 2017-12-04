import {Component, DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';

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

export function queryByCss(
    fixture: ComponentFixture<Component>,
    css: string) {
  return fixture.debugElement.query(By.css(css));
}

export function queryAllByCss(
    fixture: ComponentFixture<Component>,
    css: string) {
  return fixture.debugElement.queryAll(By.css(css));
}

export function simulateEvent(
  fixture: ComponentFixture<Component>,
  element: DebugElement,
  eventType: string) {
    element.triggerEventHandler(eventType, null);
    updateAndTick(fixture);
}

export function simulateClick(
  fixture: ComponentFixture<Component>,
  element: DebugElement) {
    simulateEvent(fixture, element, 'click');
  }
