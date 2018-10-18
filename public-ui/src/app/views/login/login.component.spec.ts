import {Component, DebugElement, Injectable} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  ActivatedRouteSnapshot,
  Resolve,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {SignInService} from 'app/services/sign-in.service';
import {LoginComponent} from 'app/views/login/login.component';
import {PageTemplateSignedOutComponent} from 'app/views/page-template-signed-out/page-template-signed-out.component';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {updateAndTick} from 'testing/test-helpers';


@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}

@Component({
  selector: 'app-fake-root',
  template: '<div class="fake-root"></div>'
})
class FakeRootComponent {}

@Component({
  selector: 'app-fake-other',
  template: '<div class="fake-other"></div>'
})
class FakeOtherComponent {}

@Component({
  selector: 'app-invitation-key',
  template: '<div class="fake-create-account"></div>'
})
class FakeCreateAccountComponent {}

@Injectable()
class ErrorResolver implements Resolve<void> {
  constructor() {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<void> {
    return Promise.reject({
      status: 500,
      title: 'server exploded'
    });
  }
}

describe('LoginComponent', () => {
  let fixture: ComponentFixture<FakeAppComponent>;
  let de: DebugElement;
  let router: Router;
  let signInService: SignInService;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        FakeAppComponent,
        FakeRootComponent,
        FakeOtherComponent,
        LoginComponent,
        FakeCreateAccountComponent,
        PageTemplateSignedOutComponent
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule.withRoutes([
          {path: 'login', component: LoginComponent},
          {path: '', component: FakeRootComponent},
          {path: 'ok', component: FakeOtherComponent},
          {
            path: 'error',
            component: FakeOtherComponent,
            resolve: {err: ErrorResolver}
          }
        ]),
        ClarityModule.forRoot()
      ],
      providers: [
        { provide: ErrorResolver },
        { provide: SignInService, useValue: new SignInServiceStub() },
      ]
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(FakeAppComponent);
    de = fixture.debugElement;
    signInService = TestBed.get(SignInService);
    router = TestBed.get(Router);
  }));

  it('should render LoginComponent initially', fakeAsync(() => {
    router.navigateByUrl('/login');
    tick();
    fixture.detectChanges();

    expect(de.queryAll(By.css('app-signed-out')).length).toEqual(1);
  }));

  it('should stay on LoginComponent on sign out detection', fakeAsync(() => {
    router.navigateByUrl('/login');
    tick();
    fixture.detectChanges();

    signInService.signOut();
    fixture.detectChanges();

    expect(de.queryAll(By.css('app-signed-out')).length).toEqual(1);
  }));

  it('should navigate to root on sign in', fakeAsync(() => {
    router.navigateByUrl('/login');
    tick();
    fixture.detectChanges();

    signInService.signIn();
    tick();
    fixture.detectChanges();

    expect(de.queryAll(By.css('app-fake-root')).length).toEqual(1);
  }));
});
