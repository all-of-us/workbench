import {Component, Input, OnInit} from '@angular/core';
import {FileDetail, RecentResource} from '../../../generated';

@Component ({
  selector : 'app-card',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/template.css',
    './component.css'],
  templateUrl: './component.html'
})

export class CardComponent implements OnInit {
  actionList = [];
  type: string;
  @Input('card')
  card: RecentResource;
  actions = [];

  constructor(
  ) {
   this.actionList = [{
     type: 'notebook',
     class: 'pencil',
     link: this.renameThis(this.card),
   text: 'Rename'}, {
     type: 'notebook',
     class: 'copy',
     link: this.renameThis(this.card),
     text: 'Clone'}, {
     type: 'notebook',
     class: 'trash',
     link: this.renameThis(this.card),
     text: 'Delete'},
     {
       type: 'cohort',
       class: 'copy',
       link: 'renameThis(notebook)'},
     {
       type: 'cohort',
       class: 'pencil',
       link: 'renameThis(notebook)'}
   ];
   this.type = this.card && this.card.notebook == null ? 'cohort' : 'notebook';
  }

  renameThis(notebook: RecentResource): void {
    console.log('in rename');
  }
  ngOnInit() {
    this.actions = this.actionList.filter(elem =>  elem.type === this.type);
  }
}
