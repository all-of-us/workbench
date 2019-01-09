import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {
  simulateEvent,
  updateAndTick
} from 'testing/test-helpers';
import {EditComponent} from './component';

// Note that the description is slightly different from the component for easier test filtering.
describe('EditIconComponent', () => {

  let fixture: ComponentFixture<EditComponent>;

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

  // TODO: Figure out how to get the style out of the hovered element.
  it('should change style on hover', fakeAsync(() => {
    simulateEvent(fixture, fixture.debugElement, 'onmouseover');
    // let thing = fixture.debugElement.query(By.css('icon'));
    // console.log(JSON.stringify(thing));
    // console.log(fixture.debugElement.childNodes[0]);
    expect(fixture).toBeTruthy();
  }));

});
