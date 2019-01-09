import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {updateAndTick} from '../../../testing/test-helpers';
import {EditComponent} from './component';

describe('EditComponent', () => {

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

});
