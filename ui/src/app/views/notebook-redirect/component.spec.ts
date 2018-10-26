import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Response, ResponseOptions} from '@angular/http';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, convertToParamMap} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {AsyncSubject} from 'rxjs/AsyncSubject';
import {Observable} from 'rxjs/Observable';

import {Kernels} from 'app/utils/notebook-kernels';
import {NotebookRedirectComponent} from 'app/views/notebook-redirect/component';
import {environment} from 'environments/environment';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {JupyterServiceStub} from 'testing/stubs/jupyter-service-stub';
import {LeoClusterServiceStub} from 'testing/stubs/leo-cluster-service-stub';
import {NotebooksServiceStub} from 'testing/stubs/notebooks-service-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterService,
  ClusterStatus
} from 'generated';
import {
  ClusterService as LeoClusterService,
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

class BlockingNotebooksStub extends NotebooksServiceStub {
  private blocker = new AsyncSubject<null>();

  public block() {
    this.blocker = new AsyncSubject<null>();
  }

  public release() {
    this.blocker.next(null);
    this.blocker.complete();
  }

  public setCookieWithHttpInfo(
      googleProject: string, clusterName: string,
      extraHttpRequestParams?: any): Observable<Response> {
    return this.blocker.flatMap(() => {
      return super.setCookieWithHttpInfo(
        googleProject, clusterName, extraHttpRequestParams);
    });
  }
}

class BlockingClusterStub extends ClusterServiceStub {
  private blocker = new AsyncSubject<null>();

  public block() {
    this.blocker = new AsyncSubject<null>();
  }

  public release() {
    this.blocker.next(null);
    this.blocker.complete();
  }

  localize(projectName: string, clusterName: string,
      req: ClusterLocalizeRequest, extraHttpRequestParams?: any): Observable<{}> {
    return this.blocker.flatMap(() => {
      return super.localize(projectName, clusterName, req, extraHttpRequestParams);
    });
  }
}

describe('NotebookRedirectComponent', () => {
  let fixture: ComponentFixture<NotebookRedirectComponent>;
  let blockingClusterStub: BlockingClusterStub;
  let blockingNotebooksStub: BlockingNotebooksStub;

  beforeEach(fakeAsync(() => {
    blockingClusterStub = new BlockingClusterStub();
    blockingClusterStub.cluster.status = ClusterStatus.Creating;
    blockingNotebooksStub = new BlockingNotebooksStub();

    TestBed.configureTestingModule({
      declarations: [
        NotebookRedirectComponent
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      providers: [
        { provide: ClusterService, useFactory: () => blockingClusterStub },
        { provide: LeoClusterService, useValue: new LeoClusterServiceStub() },
        { provide: NotebooksService, useFactory: () => blockingNotebooksStub },
        { provide: JupyterService, useValue: new JupyterServiceStub() },
        { provide: ActivatedRoute, useValue: {
          snapshot: {
            params: {
              'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
              'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
            },
            queryParamMap: convertToParamMap({
              'notebook-name': 'blah blah',
              'kernelType': Kernels.R
            }),
            data: {
              creating: true
            }
          }
        }},
      ]}).compileComponents().then(() => {
      fixture = TestBed.createComponent(NotebookRedirectComponent);
      blockingClusterStub.release();
      blockingNotebooksStub.release();
    });
  }));

  function spinnerText() {
    return fixture.debugElement.query(By.css('.spinner-text'))
      .nativeElement.textContent;
  }


  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentRef).toBeTruthy();
    // Tears down the retrying subscription.
    fixture.destroy();
  }));

  it('should redirect', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentInstance.leoUrl).toBeFalsy();
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(10000);
    updateAndTick(fixture);
    expect(fixture.componentInstance.leoUrl).toMatch(environment.leoApiUrl);
  }));

  it('should display "Initializing" until ready', fakeAsync(() => {
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Initializing');

    tick(10000);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Initializing');

    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(10000);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));

  it('should display "Resuming" until resumed', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Stopped;
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Resuming');

    tick(10000);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Resuming');

    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(10000);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));

  it('should display "Authenticating" while setting cookies', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingNotebooksStub.block();
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Authenticating');

    blockingNotebooksStub.release();
    tick();
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));

  it('should display "Creating" while creating a new notebook', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingClusterStub.block();
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Creating');

    blockingClusterStub.release();
    tick();
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));

  it('should display "Copying" while localizing', fakeAsync(() => {
    updateAndTick(fixture);

    fixture.componentInstance.notebookName = 'foo.ipynb';
    fixture.componentInstance.creating = false;
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingClusterStub.block();
    tick(10000);
    fixture.detectChanges();
    expect(spinnerText()).toContain('Copying');

    blockingClusterStub.release();
    tick();
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));

  it('should display iframe on redirect', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.i-frame'))).toBeTruthy();
  }));


  it('should properly display notebooks names', fakeAsync(() => {
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.title')).nativeElement.textContent)
      .toMatch('blah blah');
    fixture.destroy();
  }));
});
