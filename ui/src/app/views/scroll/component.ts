import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-scroller',
  templateUrl: './component.html',
  styleUrls: ['./component.css', '../../styles/cards.css']
})

export class ScrollComponent {
  @Input() cards: any[] = [];
  index: number;

  constructor() {
    this.index = 0;
  }

  moveLeft() {
    if (this.index !== 0) {
      this.index--;
    }
  }

  moveRight() {
    if (this.cards && this.index !== this.cards.length) {
      this.index++;
    }

  }
}

