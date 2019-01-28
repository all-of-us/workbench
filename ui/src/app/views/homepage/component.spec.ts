import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ExpandComponent} from 'app/icons/expand/component';
import {ShrinkComponent} from 'app/icons/shrink/component';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';

import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {EditModalComponent} from 'app/views/edit-modal/component';
import {HomepageComponent} from 'app/views/homepage/component';
import {QuickTourModalComponent} from 'app/views/quick-tour-modal/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent, ResourceCardMenuComponent} from 'app/views/resource-card/component';

import {ExpandComponent} from 'app/icons/expand/component';
import {ScrollComponent} from 'app/icons/scroll/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {ProfileService} from 'generated';
import {CohortsService} from 'generated/api/cohorts.service';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {UserMetricsService} from 'generated/api/userMetrics.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import * as React from 'react';
import * as ReactTestUtils from 'react-dom/test-utils';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';

import {simulateClick, updateAndTick} from 'testing/test-helpers';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  let profileStub: ProfileServiceStub;
  beforeEach(fakeAsync(() => {
    profileStub = new ProfileServiceStub();
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ReactiveFormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        HomepageComponent,
        RecentWorkComponent,
        QuickTourModalComponent,
        ResourceCardComponent,
        ScrollComponent,
        ResourceCardMenuComponent,
        ConfirmDeleteModalComponent,
        RenameModalComponent,
        EditModalComponent,
        ExpandComponent,
        ShrinkComponent
      ],
      providers: [
        {provide: CohortsService},
        {provide: ConceptSetsService, useVale: new ConceptSetsServiceStub()},
        {provide: ProfileService, useValue: profileStub},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {provide: WorkspacesService},
        {provide: UserMetricsService, useValue: new UserMetricsServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(HomepageComponent);
    });
  }));

  const loadProfileWithPageVisits = (p: any) => {
    profileStub.profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      pageVisits: p.pageVisits
    };
  };

  // From https://stackoverflow.com/questions/36434002/
  //        new-compilation-errors-with-react-addons-test-utils
  function renderIntoDocument(reactEl: React.ReactElement<{}>) {
    return ReactTestUtils.renderIntoDocument(reactEl) as React.Component<{}, {}>;
  }

  it('should render', fakeAsync(() => {
    loadProfileWithPageVisits({pageVisits: [{page: 'homepage'}]});
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('should display quick tour when clicked', fakeAsync(() => {
    loadProfileWithPageVisits({pageVisits: [{page: 'homepage'}]});
    updateAndTick(fixture);
    expect(fixture.debugElement.query(By.directive(QuickTourModalComponent))).toBeFalsy();
    simulateClick(fixture, fixture.debugElement.query(By.css('#learn')));
    expect(fixture.debugElement.query(By.directive(QuickTourModalComponent))).toBeTruthy();
  }));

  it('should display quick tour on first visit', fakeAsync(() => {
    updateAndTick(fixture);
    updateAndTick(fixture); // not clear why, but we need both of these
    expect(fixture.debugElement.query(By.directive(QuickTourModalComponent))).toBeTruthy();
  }));

  it('should not auto display quick tour if not first visit', fakeAsync(() => {
    loadProfileWithPageVisits({pageVisits: [{page: 'homepage'}]});
    updateAndTick(fixture);
    expect(fixture.debugElement.query(By.directive(QuickTourModalComponent))).toBeFalsy();
  }));

});
