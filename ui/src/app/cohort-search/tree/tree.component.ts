import {Component, OnInit} from '@angular/core';

import {NodeComponent} from '../node/node.component';

/*
 * The TreeComponent bootstraps the criteria tree; it has no display except for
 * a list of children (and a loading spinner), and does not defer loading those
 * children until "expanded" - expansion is basically its default state.
 */
@Component({
  selector: 'crit-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css']
})
export class TreeComponent extends NodeComponent implements OnInit {
  _type: string;

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
    this._type = this.node.get('type', '');
  }

  showSearch() {
    return this.node.get('type') === 'visit' || this.node.get('type') === 'drug';
  }
}
