import {APP_BASE_HREF} from '@angular/common';
import {Component, Input} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {randomString} from 'app/utils/index';

import {
  BugReportService,
  ProfileService
} from 'generated';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';

import {BugReportComponent} from 'app/views/bug-report/component';
import {ProfilePageComponent} from 'app/views/profile-page/component';
/* tslint:disable */
// We need to disable tslint so it does not complain about the selector we use for the mock.
@Component({
  selector: 'ngx-charts-pie-chart',
  template: '<p>Mock Product Editor Component</p>'
})
class MockPieChartComponent {
  @Input('view') view: any;
  @Input('scheme') scheme: any;
  @Input('results') results: any;
  @Input('doughnut') doughnut: boolean;
  @Input('tooltipDisabled') tooltipDisabled: boolean;
}
/* tslint:enable */

describe('ProfilePageComponent', () => {
  let fixture: ComponentFixture<ProfilePageComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        IconsModule,
        ClarityModule.forRoot(),
        BrowserAnimationsModule,
      ],
      declarations: [
        BugReportComponent,
        ProfilePageComponent,
        MockPieChartComponent
      ],
      providers: [
        {provide: BugReportService, useValue: new BugReportServiceStub()},
        {provide: APP_BASE_HREF, useValue: '/my/app'},
        {provide: ProfileService, useValue: new ProfileServiceStub()},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        {provide: SignInService, useValue: new SignInServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(ProfilePageComponent);
      tick();
      updateAndTick(fixture);
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('handles long given name errors', fakeAsync(() => {
    simulateInput(
      fixture, fixture.debugElement.query(By.css('#givenName')), randomString(81));
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.givenNameValid).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#givenNameInvalid'))).toBeTruthy();
  }));

  it('handles long family name errors', fakeAsync(() => {
    simulateInput(
      fixture, fixture.debugElement.query(By.css('#familyName')), randomString(81));
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.familyNameValid).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#familyNameInvalid'))).toBeTruthy();
  }));

  it('handles long organization errors', fakeAsync(() => {
    simulateInput(
      fixture, fixture.debugElement.query(By.css('#organization')), randomString(256));
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.organizationValid).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#organizationInvalid'))).toBeTruthy();
  }));

  it('handles long current position errors', fakeAsync(() => {
    simulateInput(
      fixture, fixture.debugElement.query(By.css('#currentPosition')), randomString(256));
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.currentPositionValid).toBeFalsy();
    expect(
      fixture.debugElement.query(By.css('#currentPositionInvalid'))).toBeTruthy();
  }));

  it('handles empty givenName field', fakeAsync(() => {
      simulateInput(
          fixture, fixture.debugElement.query(By.css('#givenName')), '');
      tick(300);
      updateAndTick(fixture);
      expect(fixture.componentInstance.givenNameNotEmpty).toBeFalsy();
      expect(fixture.debugElement.query(By.css('#givenNameEmpty'))).toBeTruthy();
  }));

  it('handles empty familyName field', fakeAsync(() => {
    simulateInput(
        fixture, fixture.debugElement.query(By.css('#familyName')), '');
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.familyNameNotEmpty).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#familyNameEmpty'))).toBeTruthy();
  }));

  it('handles empty current position field', fakeAsync(() => {
    simulateInput(
        fixture, fixture.debugElement.query(By.css('#currentPosition')), '');
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.currentPositionNotEmpty).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#currentPositionEmpty'))).toBeTruthy();
  }));

  it('handles empty organization field', fakeAsync(() => {
    simulateInput(
        fixture, fixture.debugElement.query(By.css('#organization')), '');
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.organizationNotEmpty).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#organizationEmpty'))).toBeTruthy();
  }));

  it('handles empty organization field', fakeAsync(() => {
    simulateInput(
        fixture, fixture.debugElement.query(By.css('#areaOfResearch')), '');
    tick(300);
    updateAndTick(fixture);
    expect(fixture.componentInstance.currentResearchNotEmpty).toBeFalsy();
    expect(fixture.debugElement.query(By.css('#currentResearchEmpty'))).toBeTruthy();
  }));

});
