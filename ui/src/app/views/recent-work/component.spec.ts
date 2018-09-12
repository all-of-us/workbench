import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {RouterTestingModule} from '@angular/router/testing';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import {SignInService} from 'app/services/sign-in.service';
import {CohortsService} from 'generated/api/cohorts.service';
import {UserMetricsService} from 'generated/api/userMetrics.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {CohortEditModalComponent} from 'app/views/cohort-edit-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent} from 'app/views/resource-card/component';

import {LeftScrollComponent} from 'app/icons/left-scroll/component';
import {RightScrollComponent} from 'app/icons/right-scroll/component';

import {RecentResourceResponse} from 'generated/model/recentResourceResponse';

class UserMetricsLoadedStub extends UserMetricsServiceStub {
  constructor(private numberOfResources: number) {
    super();
  }

  getUserRecentResources(extraHttpRequestParams?: any): Observable<RecentResourceResponse> {
    return new Observable<RecentResourceResponse>(observer => {
      setTimeout(() => {
        const resources = stubRecentResources(this.numberOfResources);
        observer.next(resources);
        observer.complete();
      }, 0);
    });
  }
}

describe('RecentWorkComponent', () => {
  let fixture: ComponentFixture<RecentWorkComponent>;
  let recentWorkComponent: RecentWorkComponent;
  let metricStub: UserMetricsServiceStub;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ReactiveFormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        RecentWorkComponent,
        LeftScrollComponent,
        RightScrollComponent,
        ResourceCardComponent,
        ConfirmDeleteModalComponent,
        RenameModalComponent,
        CohortEditModalComponent,
      ],
      providers: [
        {provide: CohortsService, useValue: new CohortsServiceStub()},
        {provide: SignInService, useValue: new SignInServiceStub()},
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
        {provide: UserMetricsService, useValue: new UserMetricsServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(RecentWorkComponent);
      recentWorkComponent = fixture.componentInstance;
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  // test that it displays 3 most recent resources from UserMetrics cache
  it('should display recent work', fakeAsync(() => {
    metricStub = new UserMetricsLoadedStub(4);
    recentWorkComponent.setUserMetricsService(metricStub);
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const cardsOnPage = de.queryAll(By.css('.card'));
    const cardNames = de.queryAll(By.css('.name')).map((card) => card.nativeElement.innerText);
    expect(cardsOnPage.length).toEqual(3);
    // should match LAST 3, and NOT include the "oldest"
    expect(cardNames).toEqual(['mockFile4.ipynb', 'mockFile3.ipynb', 'mockFile2.ipynb']);
  }));

  // it should not render the component at all if user has no cache
  it('should not render if no cache', fakeAsync(() => {
    updateAndTick(fixture);
    const recentWork = fixture.debugElement.queryAll(By.css('.recent-work'));
    expect(recentWork.length).toEqual(0);
  }));

  // neither scroll indicator should show if cache < 4
  it('should not render either scroll indicator if cache fewer than 4', fakeAsync(() => {
    metricStub = new UserMetricsLoadedStub(3);
    recentWorkComponent.setUserMetricsService(metricStub);
    updateAndTick(fixture);
    const scrolls = fixture.debugElement.queryAll(By.css('.scroll-indicator'));
    expect(scrolls.length).toEqual(0);
  }));

  // right scroll should appear (but no left scroll) if cache > 3
  it('should render scroll indicators correctly if cache greater than 3', fakeAsync(() => {
    metricStub = new UserMetricsLoadedStub(4);
    recentWorkComponent.setUserMetricsService(metricStub);
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const leftScroll = de.queryAll(By.css('#left-scroll'));
    const rightScroll = de.queryAll(By.css('#right-scroll'));
    expect(leftScroll.length).toEqual(0);
    expect(rightScroll.length).toEqual(1);
  }));

  // test that component scrolls correctly
  //    moves down list on right scroll click
  //    right scroll disappears and left appears
  //    moves up list on left scroll click
  //    left scroll disappears and right appears
  it('should scroll correctly', fakeAsync(() => {
    metricStub = new UserMetricsLoadedStub(4);
    recentWorkComponent.setUserMetricsService(metricStub);
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const rightScroll = () => de.query(By.css('#right-scroll'));
    const leftScroll = () => de.query(By.css('#left-scroll'));
    const nameQuery = () => de.queryAll(By.css('.name'))
      .map((card) => card.nativeElement.innerText.trim());
    simulateClick(fixture, rightScroll());
    updateAndTick(fixture);
    // should have scrolled right so should be FIRST 3 and NOT last
    expect(nameQuery())
      .toEqual(['mockFile3.ipynb', 'mockFile2.ipynb', 'mockFile1.ipynb']);
    // right scroll should not be present and left present
    expect(rightScroll()).toBe(null);
    expect(leftScroll()).not.toBe(null);
    simulateClick(fixture, leftScroll());
    updateAndTick(fixture);
    // all should be returned to orig state
    expect(nameQuery())
      .toEqual(['mockFile4.ipynb', 'mockFile3.ipynb', 'mockFile2.ipynb']);
    expect(rightScroll()).not.toBe(null);
    expect(leftScroll()).toBe(null);
  }));
});

function stubRecentResources(numberOfResources: number) {
  const currentCache = [];
  while (numberOfResources > 0) {
    const currentResource = {
      workspaceId: numberOfResources,
      workspaceNamespace: 'defaultNamespace' + numberOfResources,
      workspaceFirecloudName: 'defaultFirecloudName' + numberOfResources,
      permission: 'Owner',
      notebook: {
        'name': 'mockFile' + numberOfResources + '.ipynb',
        'path': 'gs://bucket/notebooks/mockFile.ipynb',
        'lastModifiedTime': 100
      },
      lastModified: Date.now()
    };
    currentCache.push(currentResource);
    numberOfResources--;
  }
  return currentCache;
}
