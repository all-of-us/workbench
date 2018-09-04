import {Component, Input} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CohortsService} from '../../../generated';
import {WorkspaceData} from '../../resolvers/workspace';

@Component ({
  selector : 'app-card',
  styleUrls: [
    '../../styles/template.css'],
  templateUrl: './component.html'
})

export class CardComponent {
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
}
