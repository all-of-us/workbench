import { Component, OnInit } from '@angular/core';
import { TreeService } from '../services/tree.service';

import { Router } from '@angular/router';


@Component({
  selector: 'app-survey',
  templateUrl: './survey.component.html',
  styleUrls: ['./survey.component.css']
})
export class SurveyComponent implements OnInit {
  tree_nodes: Array<any> = [];
  analysis;
  cardData
  constructor(
    private treeService: TreeService,
    private router: Router,
  ) {
    // this.treeService.getSurveySections().subscribe(results => {
    //   //
    //   this.tree_nodes = results.data
    // })


  }

  ngOnInit() {
  }


  routeTo(id) {
    let link = ['/survey', id];
    this.router.navigate(link);
  }

}
