import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';

import {QuickTourModalComponent} from './component';

import {simulateClick, updateAndTick} from 'testing/test-helpers';

describe('QuickTourModalComponent', () => {
  let fixture: ComponentFixture<QuickTourModalComponent>;
  let de: DebugElement;
  const panelTitles = ['Introduction', 'Workspaces', 'Cohorts', 'Concepts', 'Notebooks'];
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [
        QuickTourModalComponent
      ],
      providers: []
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(QuickTourModalComponent);
      // make sure modal is displaying
      fixture.componentInstance.learning = true;
      updateAndTick(fixture);
      de = fixture.debugElement;
    });
  }));

  it('should render', fakeAsync(() => {
    expect(fixture).toBeTruthy();
  }));

  it('should open in Intro', fakeAsync(() => {
    const panelTitle = de.query(By.css('.panel-title'));
    expect(panelTitle.nativeElement.innerText).toMatch('Intro');
  }));

  it('should move forward and backward with next and previous clicks', fakeAsync(() => {
    const nextButton = de.query(By.css('#next'));
    const prevButton = de.query(By.css('#previous'));
    const panelTitle = de.query(By.css('.panel-title'));
    simulateClick(fixture, nextButton);
    updateAndTick(fixture);
    const nextPanelTitle = panelTitles[1].toString();
    expect(panelTitle.nativeElement.innerText).toMatch(nextPanelTitle);
    simulateClick(fixture, prevButton);
    updateAndTick(fixture);
    const origPanelTitle = panelTitles[0].toString();
    expect(panelTitle.nativeElement.innerText).toMatch(origPanelTitle);
  }));

  it('should move to appropriate panel when clicked in breadcrumbs', fakeAsync(() => {
    const circleToSelect = de.query(By.css('#Cohorts'));
    simulateClick(fixture, circleToSelect);
    updateAndTick(fixture);
    const panelTitle = de.query(By.css('.panel-title'));
    expect(panelTitle.nativeElement.innerText).toMatch('Cohorts');
  }));

});
