import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {
  updateAndTick
} from 'testing/test-helpers';
import {EditComponent} from './component';
import * as ReactTestUtils from 'react-dom/test-utils';

// Note that the description is slightly different from the component for easier test filtering.
describe('EditIconComponent', () => {

  let fixture: ComponentFixture<EditComponent>;
  const styleAttribute = 'style';

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      declarations: [EditComponent]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(EditComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('should change style on mouse over', fakeAsync(() => {
    updateAndTick(fixture);
    const icon = fixture.debugElement.nativeElement.querySelector('svg');
    const style = icon.getAttribute(styleAttribute);
    ReactTestUtils.Simulate.mouseOver(icon);
    updateAndTick(fixture);
    const hoverStyle = icon.getAttribute(styleAttribute);

    expect(style).not.toEqual(hoverStyle);
    expect(hoverStyle).toContain('fill');
  }));

  it('should change style on mouse out', fakeAsync(() => {
    updateAndTick(fixture);
    const icon = fixture.debugElement.nativeElement.querySelector('svg');
    ReactTestUtils.Simulate.mouseOver(icon);
    updateAndTick(fixture);
    const hoverStyle = icon.getAttribute(styleAttribute);

    ReactTestUtils.Simulate.mouseLeave(icon);
    updateAndTick(fixture);
    const mouseOutStyle = icon.getAttribute(styleAttribute);

    expect(mouseOutStyle).not.toEqual(hoverStyle);
    expect(mouseOutStyle).not.toContain('fill');
  }));

});
