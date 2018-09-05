import {Component, EventEmitter, OnChanges, OnInit, Output} from '@angular/core';
import {TreeType} from 'generated';
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
export class TreeComponent extends NodeComponent implements OnInit, OnChanges {
     _type: string;
     name: string;

    ngOnChanges() {
        if(this.node.get('type') === TreeType[TreeType.ICD9] || this.node.get('type') === TreeType[TreeType.ICD10]){
            super.ngOnInit();
        }
    }

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
    this._type = this.node.get('type', '');
  }

    showSearch() {
        return this.node.get('type') === TreeType[TreeType.VISIT]
            || this.node.get('type') === TreeType[TreeType.DRUG]
            || this.node.get('type') === TreeType[TreeType.MEAS]
            || this.node.get('type') === TreeType[TreeType.PM]
            || this.node.get('type') === TreeType[TreeType.CONDITION]
            || this.node.get('type') === TreeType[TreeType.ICD9]
            || this.node.get('type') === TreeType[TreeType.ICD10];
    }

  optionChange(flag) {
    if (flag) {
        this.name = flag;
        this.testOptionChange = true;
        setTimeout(() => super.loadChildren(true));
        }
    }

    showdropDown() {
        return this.node.get('type') === TreeType[TreeType.CONDITION];
    }
}
