import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-card',
  templateUrl: './component.html',
  styleUrls: ['../../styles/cards.css']
})
export class CardComponent {
  @Input() card: any;
}

