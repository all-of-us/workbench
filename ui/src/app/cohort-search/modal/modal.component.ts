import {select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';

import {wizardOpen} from '../redux';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  @select(wizardOpen) open$: Observable<boolean>;
  open = false;

  ngOnInit() {
    this.subscription = this.open$.subscribe(open => this.open = open);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
