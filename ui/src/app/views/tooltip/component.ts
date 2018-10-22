import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-tooltip',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ToolTipComponent {
  @Input('direction') direction = 'right';

  findPosition(element): void {
    const el = document.getElementById('toolTip');
    const bottom = el.getBoundingClientRect().bottom;
    if (bottom <= 0) {
      this.direction = this.direction.replace('top', 'bottom');
    } else {
      this.direction = this.direction.replace('bottom', 'top');
    }
  }
}
