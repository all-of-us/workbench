import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HighlightSearchComponent } from './highlight-search.component';

describe('HighlightSearchComponent', () => {
  let component: HighlightSearchComponent;
  let fixture: ComponentFixture<HighlightSearchComponent>;
  beforeEach(async(() => {
    TestBed.configureTestingModule({
        declarations: [ HighlightSearchComponent ]
      })
      .compileComponents();
  }));
  beforeEach(() => {
    fixture = TestBed.createComponent(HighlightSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  it('highlight_split_test', () => {
    component = new HighlightSearchComponent();
    component.matchString = new RegExp('lung|disorder');
    const matchedWords = component.highlight('lung_enlargement_with_another_disorder');
    expect(matchedWords).toContain('lung');
    expect(matchedWords).toContain('disorder');
  });
});
