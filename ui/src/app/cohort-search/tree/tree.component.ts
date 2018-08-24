import {Component, EventEmitter, OnChanges, OnInit, Output} from '@angular/core';
import {DomainType} from 'generated';
import {CRITERIA_TYPES} from '../constant';
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
     testOptionChange= false;
     name: string;

  ngOnChanges() {
    super.ngOnInit();
  }

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
  }

  showSearch() {
    return this.node.get('type') === DomainType.VISIT
      || this.node.get('type') === DomainType.DRUG
      || this.node.get('type') === DomainType.CONDITION
      || this.node.get('type') === CRITERIA_TYPES.ICD9
      || this.node.get('type') === CRITERIA_TYPES.ICD10
      || this.node.get('type') === CRITERIA_TYPES.MEAS;
  }

  showDropDown() {
    return this.node.get('type') === DomainType.CONDITION || this.node.get('type') === CRITERIA_TYPES.ICD9 || this.node.get('type') === CRITERIA_TYPES.ICD10;
  }

  optionChange(flag) {
    if (flag) {
        this.name = flag;
        this.testOptionChange = true;
        setTimeout(() => super.loadChildren(true));
        }
    }

}
