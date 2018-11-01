import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Observable} from 'rxjs/Observable';

import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';

import {RouterTestingModule} from '@angular/router/testing';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import {SignInService} from 'app/services/sign-in.service';
import {CohortsService} from 'generated/api/cohorts.service';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {UserMetricsService} from 'generated/api/userMetrics.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {EditModalComponent} from 'app/views/edit-modal/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent} from 'app/views/resource-card/component';

import {LeftScrollLightComponent} from 'app/icons/left-scroll-light/component';
import {LeftScrollComponent} from 'app/icons/left-scroll/component';
import {RightScrollLightComponent} from 'app/icons/right-scroll-light/component';
import {RightScrollComponent} from 'app/icons/right-scroll/component';


describe('RecentWorkComponent', () => {
  let fixture: ComponentFixture<RecentWorkComponent>;
  let userMetricsSpy: jasmine.SpyObj<UserMetricsService>;
  beforeEach(fakeAsync(() => {
    const spy = jasmine.createSpyObj('UserMetricsService', ['getUserRecentResources']);
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
        LeftScrollLightComponent,
        RightScrollComponent,
        RightScrollLightComponent,
        ResourceCardComponent,
        ConfirmDeleteModalComponent,
        RenameModalComponent,
        EditModalComponent,
      ],
      providers: [
        {provide: CohortsService, useValue: new CohortsServiceStub()},
        {provide: ConceptSetsService },
        {provide: SignInService, useValue: new SignInServiceStub()},
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
        {provide: UserMetricsService, useValue: spy},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(RecentWorkComponent);
      userMetricsSpy = TestBed.get(UserMetricsService);
      tick();
      // Standard window size for this test suite.  should load 4 cards by default
      fixture.nativeElement.style.width = '1024px';
    });
  }));

  it('should render', fakeAsync(() => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of([]));
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  // test that it displays related resources when handed workspace
  it('should display related resources when handed workspace', fakeAsync(() => {
    fixture.debugElement.componentInstance.workspace = WorkspacesServiceStub.stubWorkspace();
    fixture.debugElement.componentInstance.route.snapshot.data.workspace =
      WorkspacesServiceStub.stubWorkspaceData();
    tick();
    updateAndTick(fixture);
    updateAndTick(fixture);
    const cardsOnPage = fixture.debugElement.queryAll(By.css('.card'));
    expect(cardsOnPage.length).toEqual(3);
    const cardNames = fixture.debugElement.queryAll(By.css('.name'))
      .map((card) => card.nativeElement.innerText);
    expect(cardNames).toEqual(['sample name', 'sample name 2', 'mockFile']);
  }));

  // test that it displays 4 most recent resources from UserMetrics cache
  it('should display recent work', fakeAsync(() => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of(stubRecentResources(5)));
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const cardsOnPage = de.queryAll(By.css('.card'));
    const cardNames = de.queryAll(By.css('.name')).map((card) => card.nativeElement.innerText);
    expect(cardsOnPage.length).toEqual(4);
    // should match LAST 4, and NOT include the "oldest"
    expect(cardNames).toEqual(
        ['mockFile5', 'mockFile4', 'mockFile3', 'mockFile2']);
  }));

  // it should not render the component at all if user has no cache
  it('should not render if no cache', fakeAsync(() => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of([]));
    updateAndTick(fixture);
    const recentWork = fixture.debugElement.queryAll(By.css('.recent-work'));
    expect(recentWork.length).toEqual(0);
  }));

  // neither scroll indicator should show if cache < 5
  it('should not render either scroll indicator if cache fewer than 4', fakeAsync(() => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of(stubRecentResources(4)));
    updateAndTick(fixture);
    const scrolls = fixture.debugElement.queryAll(By.css('.scroll-indicator'));
    expect(scrolls.length).toEqual(0);
  }));

  // right scroll should appear (but no left scroll) if cache > 4
  it('should render scroll indicators correctly if cache greater than 3', fakeAsync(() => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of(stubRecentResources(5)));
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
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of(stubRecentResources(5)));
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const rightScroll = () => de.query(By.css('#right-scroll'));
    const leftScroll = () => de.query(By.css('#left-scroll'));
    const nameQuery = () => de.queryAll(By.css('.name'))
      .map((card) => card.nativeElement.innerText.trim());
    simulateClick(fixture, rightScroll());
    updateAndTick(fixture);
    // should have scrolled right so should be FIRST 4 and NOT last
    expect(nameQuery())
      .toEqual(['mockFile4', 'mockFile3', 'mockFile2', 'mockFile1']);
    // right scroll should not be present and left present
    expect(rightScroll()).toBe(null);
    expect(leftScroll()).not.toBe(null);
    simulateClick(fixture, leftScroll());
    updateAndTick(fixture);
    // all should be returned to orig state
    expect(nameQuery())
      .toEqual(['mockFile5', 'mockFile4', 'mockFile3', 'mockFile2']);
    expect(rightScroll()).not.toBe(null);
    expect(leftScroll()).toBe(null);
  }));

  // test that when we resize the window, the component resizes too
  //    when the window gets smaller, should show less cards
  //    when the window gets larger, should see more cards
  it('should resize when screen resizes', fakeAsync( () => {
    userMetricsSpy.getUserRecentResources.and.returnValue(Observable.of(stubRecentResources(5)));
    updateAndTick(fixture);
    const de = fixture.debugElement;
    const cardsOnPage = de.queryAll(By.css('.card'));
    expect(cardsOnPage.length).toEqual(4);

    // Make it small - should show 3 cards
    fixture.nativeElement.style.width = '800px';
    window.dispatchEvent(new Event('resize'));
    updateAndTick(fixture);
    const deLess = fixture.debugElement;
    const lessCards = deLess.queryAll(By.css('.card'));
    expect(lessCards.length).toEqual(3);

    // Now make it big - should show 5 cards
    fixture.nativeElement.style.width = '1200px';
    window.dispatchEvent(new Event('resize'));
    updateAndTick(fixture);
    const deMore = fixture.debugElement;
    const moreCards = deMore.queryAll(By.css('.card'));
    expect(moreCards.length).toEqual(5);
  }));
});

function stubRecentResources(numberOfResources: number) {
  const currentCache = [];
  while (numberOfResources > 0) {
    currentCache.push({
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
    });
    numberOfResources--;
  }
  return currentCache;
}
