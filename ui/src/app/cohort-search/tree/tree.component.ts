import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {DomainType} from 'generated';
import {CRITERIA_TYPES, DOMAIN_TYPES} from '../constant';
import {NodeComponent} from '../node/node.component';
import {NgRedux} from "@angular-redux/store";
import {CohortSearchActions, CohortSearchState} from "../redux";
import {Map} from "immutable";
// import {CohortSearchActions} from '../redux';

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
    readonly domainTypes = DOMAIN_TYPES;
    @Output() onOptionChange = new EventEmitter<string>();
  ngOnInit() {

    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
   // this._type = this.node.get('type', '');
  }

  showSearch() {
    return this.node.get('type') === DomainType.VISIT || this.node.get('type') === DomainType.DRUG
        || this.node.get('type') === DomainType.CONDITION;
  }

  showDropDown() {
   // console.log(this.node.get('type'));
      return this.node.get('type') === DomainType.CONDITION || CRITERIA_TYPES.ICD9;
  }
    optionChange(flag) {
      if(flag){

      }
// console.log(criteria.name);
//                 // type: criteria.type
//         this.onOptionChange.emit(criteria.name);
//         setTimeout(() => super.loadChildren(true));
//     //    check for launchwizard function
//     }

}
