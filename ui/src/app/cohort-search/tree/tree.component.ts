import {Component, OnInit} from '@angular/core';

import {NodeComponent} from '../node/node.component';

@Component({
  selector: 'crit-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css']
})
export class TreeComponent extends NodeComponent implements OnInit {
  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
  }
}
