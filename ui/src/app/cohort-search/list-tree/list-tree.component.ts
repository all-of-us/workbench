import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {DomainType, TreeType} from 'generated';

/*
 * The TreeComponent bootstraps the criteria tree; it has no display except for
 * a list of children (and a loading spinner), and does not defer loading those
 * children until "expanded" - expansion is basically its default state.
 */
@Component({
  selector: 'crit-list-tree',
  templateUrl: './list-tree.component.html',
  styleUrls: ['./list-tree.component.css']
})
export class ListTreeComponent extends ListNodeComponent implements OnInit, OnChanges {
  @Input() back: Function;
  _type: string;
  name: string;

  ngOnChanges() {
    if (this.node.type === TreeType[TreeType.ICD9]
      || this.node.type === TreeType[TreeType.ICD10]
      || this.node.type === TreeType[TreeType.CPT]
      || this.node.type === TreeType[TreeType.SNOMED]) {
      super.ngOnInit();
    }
  }

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
    this._type = this.node.domainId;
  }

  get showSearch() {
    return this.node.domainId !== DomainType[DomainType.PERSON];
  }

  get showHeader() {
    return this.node.domainId !== DomainType.PHYSICALMEASUREMENT
      && this.node.domainId !== DomainType.SURVEY
      && this.node.domainId !== DomainType.VISIT;
  }

  get showDropDown() {
    return false;
    // return !this.isEmpty && this.node.type !== TreeType[TreeType.SNOMED];
  }

  get isEmpty() {
    return !this.loading && (this.empty || this.error);
  }

  optionChange(flag) {
    if (flag) {
      this._type = flag;
      setTimeout(() => super.loadChildren(true));
    }
  }
}
