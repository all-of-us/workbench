import {TestBed, async} from '@angular/core/testing';
import {Title} from '@angular/platform-browser';
import {AppComponent} from 'app/views/app/component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {SignInService} from 'app/services/sign-in.service';
import {CohortsService} from 'generated';

describe('AppComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule
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
