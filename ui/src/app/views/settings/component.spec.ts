import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {SettingsComponent} from 'app/views/settings/component';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';

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
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }));

  it('should open the cluster reset modal', fakeAsync(() => {
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
    de.query(By.css('.notebook-settings button')).nativeElement.click();
    fixture.detectChanges();
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(1);
  }));

  it('should delete cluster on reset', fakeAsync(() => {
    const spy = spyOn(clusterService, 'deleteCluster').and.callThrough();
    const de = fixture.debugElement;
    de.query(By.css('.notebook-settings button')).nativeElement.click();
    fixture.detectChanges();
    de.query(By.css('.reset-modal-button')).nativeElement.click();
    tick();
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledWith(
      clusterService.cluster.clusterNamespace,
      clusterService.cluster.clusterName);
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
  }));

  it('should only close modal on cancel', fakeAsync(() => {
    const spy = spyOn(clusterService, 'deleteCluster').and.callThrough();
    const de = fixture.debugElement;
    de.query(By.css('.notebook-settings button')).nativeElement.click();
    fixture.detectChanges();
    de.query(By.css('.cancel-modal-button')).nativeElement.click();
    tick();
    fixture.detectChanges();

    expect(spy).not.toHaveBeenCalled();
    expect(de.queryAll(By.css('.modal-body')).length).toEqual(0);
  }));
});
