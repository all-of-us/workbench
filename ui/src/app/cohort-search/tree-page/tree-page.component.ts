import {Component, OnInit} from '@angular/core';

import {NodeComponent} from '../node/node.component';

@Component({
  selector: 'crit-tree-page',
  templateUrl: './tree-page.component.html',
  styleUrls: ['./tree-page.component.css']
})
export class TreePageComponent extends NodeComponent implements OnInit {
  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
  }
}
