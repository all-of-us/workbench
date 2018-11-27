import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';

/**
 * A Floating Action Button constisting of button content and text which slides
 * out on hover.
 *
 * This button has the following limitations currently:
 * - Transitions are smoothest for ~200px width text. Text which is much longer
 *   than this may be truncated, at which point this approach should be
 *   revisited e.g. by supporting client-specified widths or dynamically
 *   text-width off-screen.
 * - The inactive FAB size is 1.5rem width/height; the FAB content will need to
 *   be manually sized accordingly (and centered).
 */
@Component({
  selector: 'app-sliding-fab',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class SlidingFabComponent {
  @Output('submit') submit = new EventEmitter<void>();
  @Input('expanded') expanded = '';
  @Input('disable') disable = false;
  hovering = false;

  attemptSubmit() {
    if (this.disable) {
      return;
    }
    this.submit.emit();
  }
}
