import {TestBed, async} from '@angular/core/testing';
import {Title} from '@angular/platform-browser';
import {CohortBuilderPlaceholderComponent} from 'app/views/cohort-builder-placeholder/component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {UserService} from 'app/services/user.service';
import {CohortsService} from 'generated';

describe('CohortBuilderPlaceholderComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule
      ],
      declarations: [
        CohortBuilderPlaceholderComponent
      ],
      providers: [
        { provide: UserService, useValue: {} },
        { provide: CohortsService, useValue: {} }
      ] }).compileComponents();
  }));

  it('should create the component', async(() => {
    const fixture = TestBed.createComponent(CohortBuilderPlaceholderComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

});
