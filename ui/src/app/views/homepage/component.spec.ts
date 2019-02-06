import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';

import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkbenchAccessTasksComponent, HomepageComponent} from 'app/views/homepage/component';
import {QuickTourModalComponent} from 'app/views/quick-tour-modal/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {ResourceCardComponent, ResourceCardMenuComponent} from 'app/views/resource-card/component';

import {ExpandComponent} from 'app/icons/expand/component';
import {ScrollComponent} from 'app/icons/scroll/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {ProfileService} from 'generated';
import {CohortsService} from 'generated/api/cohorts.service';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {UserMetricsService} from 'generated/api/userMetrics.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch/api';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {findElementsReact, simulateClick, updateAndTick} from 'testing/test-helpers';

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
        ExpandComponent,
        ShrinkComponent,
        WorkbenchAccessTasksComponent
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
      registerApiClient(ProfileApi, new ProfileApiStub());
    });
  }));

  const loadProfileWithPageVisits = (p: any) => {
    profileStub.profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      pageVisits: p.pageVisits
    };
  };

  const loadProfileWithNihUsername = (p: any) => {
    profileStub.profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      linkedNihUsername: p.linkedNihUsername
    };
  };

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

  it('should show the era commons linking page if the user has no nih username',
    fakeAsync(() => {
      updateAndTick(fixture);
      updateAndTick(fixture);
      expect(findElementsReact(fixture, '[data-test-id="Login"]')
        [0].innerText).toEqual('LOGIN');
    }));

  it('should not show the era commons linking page if user has an nih username',
    fakeAsync(() => {
      loadProfileWithNihUsername({linkedNihUsername: 'testusername'});
      updateAndTick(fixture);
      updateAndTick(fixture);
      expect(findElementsReact(fixture, '[data-test-id="Login"]')
        .length).toEqual(0);
    }));

});
