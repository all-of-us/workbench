import {APP_BASE_HREF} from '@angular/common';
import {Component, DebugElement, Input} from '@angular/core';
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
} from '../../../testing/test-helpers';

import {ProfileStorageService} from '../../services/profile-storage.service';
import {ServerConfigService} from '../../services/server-config.service';
import {SignInService} from '../../services/sign-in.service';

import {BugReportComponent} from '../bug-report/component';
import {ProfilePageComponent} from '../profile-page/component';
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
class ProfilePage {
  fixture: ComponentFixture<ProfilePageComponent>;
  component: ProfilePageComponent;
  givenNameField: DebugElement;
  familyNameField: DebugElement;
  organizationField: DebugElement;
  currentPositionField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(ProfilePageComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    this.givenNameField = this.fixture.debugElement.query(By.css('#givenName'));
    this.familyNameField = this.fixture.debugElement.query(By.css('#familyName'));
    this.organizationField = this.fixture.debugElement.query(By.css('#organization'));
    this.currentPositionField = this.fixture.debugElement.query(By.css('#currentPosition'));
  }
}

describe('ProfilePageComponent', () => {
  let page: ProfilePage;
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
      page = new ProfilePage(TestBed);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(page.fixture);
    expect(page.fixture).toBeTruthy();
  }));

  it('handles long given name errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.givenNameField, randomString(81));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.component.givenNameValid).toBeFalsy();
  }));

  it('handles long family name errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.familyNameField, randomString(81));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.component.familyNameValid).toBeFalsy();
  }));

  it('handles long organization errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.organizationField, randomString(256));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.component.organizationValid).toBeFalsy();
  }));

  it('handles long current position errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.currentPositionField, randomString(256));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.component.currentPositionValid).toBeFalsy();
  }));

});
