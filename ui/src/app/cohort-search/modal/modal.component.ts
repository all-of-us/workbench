import {select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {
  CohortSearchActions,
  wizardOpen,
} from '../redux';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  @select(wizardOpen) open$: Observable<boolean>;
  open = false;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.open$
      .filter(open => !!open)
      .subscribe(open => this.open = open);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancel() {
    this.open = false;
    this.actions.cancelWizard();
  }

  finish() {
    this.open = false;
    this.actions.finishWizard();
  }
}
