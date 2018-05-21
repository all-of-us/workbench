import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';


@Component({
  selector: 'app-detail-all-events',
  templateUrl: './detail-all-events.component.html',
  styleUrls: ['./detail-all-events.component.css']
})
export class DetailAllEventsComponent extends DetailTabTableComponent {
  /* These must be listed here b/c Angular component inheritance does not
   * support decorators :/
   */
  @Input() tabname;
  @Input() columns;
  @Input() filterType;
  @Input() reverseEnum;

  @Output() onRowClick = new EventEmitter();
  onClick(datum) { this.onRowClick.emit(datum); }
}
