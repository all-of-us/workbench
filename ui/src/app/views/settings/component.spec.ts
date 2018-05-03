import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {SettingsComponent} from 'app/views/settings/component';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {ClusterService} from 'generated';


describe('SettingsComponent', () => {
  let fixture: ComponentFixture<SettingsComponent>;
  let clusterService: ClusterServiceStub;

  beforeEach(fakeAsync(() => {
    clusterService = new ClusterServiceStub();
    TestBed.configureTestingModule({
      declarations: [
        SettingsComponent
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      providers: [
        { provide: ClusterService, useValue: clusterService },
      ]}).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(SettingsComponent);
    // Trigger ngOnInit, tick() the promise, then update again to undisable the
    // "reset" button.
    updateAndTick(fixture);
    updateAndTick(fixture);
  }));

  it('should open the cluster reset modal', fakeAsync(() => {
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
    simulateClick(fixture, de.query(By.css('.notebook-settings button')));
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(1);
  }));

  it('should delete cluster on reset', fakeAsync(() => {
    const spy = spyOn(clusterService, 'deleteCluster').and.callThrough();
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.notebook-settings button')));
    simulateClick(fixture, de.query(By.css('.reset-modal-button')));
    updateAndTick(fixture);

    expect(spy).toHaveBeenCalledWith(
      clusterService.cluster.clusterNamespace,
      clusterService.cluster.clusterName);
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
  }));

  it('should only close modal on cancel', fakeAsync(() => {
    const spy = spyOn(clusterService, 'deleteCluster').and.callThrough();
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.notebook-settings button')));
    simulateClick(fixture, de.query(By.css('.cancel-modal-button')));

    expect(spy).not.toHaveBeenCalled();
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
  }));
});
