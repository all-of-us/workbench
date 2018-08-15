import {APP_BASE_HREF} from '@angular/common';
import {Component, Input} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';


import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';

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
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
