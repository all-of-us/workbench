// UI Component framing the overall app (title and nav).
// Content is in other Components.

import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import 'clarity-icons';
@Component({
  selector: 'app-error-handler',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class ErrorHandlerComponent implements OnInit {
  notifyFiveHundred = false;

  constructor() {}

  ngOnInit(): void {
    window['handleFiveHundred'] = () => {
      this.notifyFiveHundred = true;
      setTimeout(() => {
        this.notifyFiveHundred = false;
      }, 10000);
    };
  }
  closeError(): void {
    this.notifyFiveHundred = false;
  }
}
