import {select} from '@angular-redux/store';
import {Component, OnInit} from '@angular/core';

import {NodeComponent} from '../node/node.component';
import {activeParameterList} from '../redux';
import {typeToTitle} from '../utils';

@Component({
  selector: 'crit-tree-page',
  templateUrl: './tree-page.component.html',
  styleUrls: ['./tree-page.component.css']
})
export class TreePageComponent extends NodeComponent implements OnInit {
  @select(activeParameterList) criteriaList$;
  /* Functions of SearchParameters */

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
  }

  get selectionTitle() {
    const ctype = this.node.get('type', '');
    const title = typeToTitle(ctype);
    return title
      ? `Selected ${title} Codes`
      : 'No Selection';
  }
}
