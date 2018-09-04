import {Component, Input} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CohortsService} from '../../../generated';
import {WorkspaceData} from '../../resolvers/workspace';

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
  @Input('header')
  header: string;

  constructor(
  ) {
   this.actionList = [{
     notebook: {
       class: 'copy',
       link: 'query'}
   }];
   this.type = 'notebook';
  }
  ngOnInit() {
    this.actions = this.actionList.filter(elem =>  elem.type === this.type);
  }
}
