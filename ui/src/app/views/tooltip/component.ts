import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-tooltip',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ToolTipComponent {
  @Input('direction') direction = 'right';

  findPosition(): void {
    const el = document.getElementById('toolTip');
    const bottom = el.getBoundingClientRect().bottom;
    const top = el.getBoundingClientRect().top;
    if (top > 0 || bottom > 0) {
      if (top < bottom) {
        this.direction = this.direction.replace('top', 'bottom');
      } else {
        this.direction = this.direction.replace('bottom', 'top');
      }
    }
  }
}
