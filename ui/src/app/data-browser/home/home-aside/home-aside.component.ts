import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-home-aside',
  templateUrl: './home-aside.component.html',
  styleUrls: ['./home-aside.component.css']
})
export class HomeAsideComponent {
  ehr;
  ppi;
  selectedRow;
  @Input() homeData;
  @Output() infoEmit = new EventEmitter();
  @Output() anchorEmit = new EventEmitter();

  // Save selected index of item they clicked
  // and send selected item to parent
  passData(item, index) {
    this.selectedRow = index;
    this.infoEmit.emit(item);
  }

  passAnchor(item) {
    this.anchorEmit.emit(item);
  }
}
