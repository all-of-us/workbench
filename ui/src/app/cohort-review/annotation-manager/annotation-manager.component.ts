import {Component, ViewChild} from '@angular/core';

import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-annotation-manager',
  templateUrl: './annotation-manager.component.html',
  styleUrls: ['./annotation-manager.component.css']
})
export class AnnotationManagerComponent {
  @ViewChild('modal') modal;

  constructor(private state: ReviewStateService) {}

  close() {
    this.state.isEditingAnnotations.next(false);
    this.modal.close();
  }

  get twoThirds() {
    return `${Math.floor((window.innerHeight / 3) * 2)}px`;
  }
}
