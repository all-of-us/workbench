import {TestBed, async} from '@angular/core/testing';
import {Title} from '@angular/platform-browser';
import {AppComponent} from 'app/views/app/app.component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {SignInService} from 'app/services/sign-in.service';
import {CohortsService} from 'generated';
import {ClarityModule} from 'clarity-angular';

describe('AppComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AppComponent
      ],
      providers: [
        { provide: SignInService, useValue: {} },
        { provide: CohortsService, useValue: {} }
      ] }).compileComponents();
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

});
