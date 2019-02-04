import {DebugElement} from '@angular/core';
import {ComponentFixture, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import * as ReactTestUtils from 'react-dom/test-utils';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {BreadcrumbComponent} from 'app/views/breadcrumb/component';
import {BugReportComponent} from 'app/views/bug-report/component';
import {RoutingSpinnerComponent} from 'app/views/routing-spinner/component';
import {SignedInComponent} from 'app/views/signed-in/component';
import {BugReportServiceStub} from './stubs/bug-report-service-stub';
import {ErrorHandlingServiceStub} from './stubs/error-handling-service-stub';
import {ProfileStorageServiceStub} from './stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from './stubs/server-config-service-stub';
import {SignInServiceStub} from './stubs/sign-in-service-stub';

import {BugReportService} from 'generated';

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

export function findElementsReact<C>(
  fixture: ComponentFixture<C>,
  selector: string
) {
  return [].slice.call(fixture.debugElement.nativeElement.querySelectorAll(selector));
}

export const signedInDependencies = {
  imports: [
    ClarityModule.forRoot(),
    FormsModule,
    RouterTestingModule,
  ],
  declarations: [
    BreadcrumbComponent,
    BugReportComponent,
    SignedInComponent,
    RoutingSpinnerComponent
  ],
  providers: [
    {provide: BugReportService, useValue: new BugReportServiceStub()},
    {provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub()},
    {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
    {
      provide: ServerConfigService,
      useValue: new ServerConfigServiceStub({
        gsuiteDomain: 'fake-research-aou.org'
      })
    },
    {provide: SignInService, useValue: new SignInServiceStub()},
  ]
};
