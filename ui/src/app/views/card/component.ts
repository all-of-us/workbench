import {Component, Input} from '@angular/core';
import {FileDetail, RecentResource} from '../../../generated';

@Component ({
  selector : 'app-card',
  styleUrls: [
    '../../styles/template.css'],
  templateUrl: './component.html'
})

export class CardComponent {
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
     link: 'renameThis(notebook)'},
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
   this.actions = this.actionList.filter(action =>  action.type === this.type);
  }

  renameThis(notebook: FileDetail): void {
    console.log('in rename');
  }
}
