import {
  Component,
  OnDestroy,
  OnInit,
  Input,
  Output,
  EventEmitter,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {needsAttributes} from '../utils';
import {
  CohortSearchActions,
  CohortSearchState,
  activeParameterList,
  isCriteriaLoading,
  criteriaChildren,
  criteriaError,
} from '../../redux';


@Component({
  selector: 'crit-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css'],
})
export class TreeComponent implements OnInit, OnDestroy {
  @Input() node;

  private loading: boolean;
  private error: any;
  private children: List<any>;
  private selections: List<any>;
  private subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const _parentId = this.node.get('id');

    const error$ = this.ngRedux.select(criteriaError(_type, _parentId));
    const loading$ = this.ngRedux.select(isCriteriaLoading(_type, _parentId));
    const children$ = this.ngRedux.select(criteriaChildren(_type, _parentId));
    const selections$ = this.ngRedux.select(activeParameterList);

    this.subscriptions = [
      error$.subscribe(value => this.error = value),
      loading$.subscribe(value => this.loading = value),
      children$.subscribe(value => this.children = value),
      selections$.subscribe(value => this.selections = value),
    ];
    this.actions.fetchCriteria(_type, _parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  get hasError() {
    return !(this.error === null || this.error === undefined);
  }

  /** Functions of the node's child nodes */
  paramId(node) {
    return `param${node.get('id')}`;
  }

  select(node) {
    if (needsAttributes(node)) {
      this.actions.setWizardFocus(node);
    } else {
      /*
       * Here we set the parameter ID to `param<criterion ID>` - this is
       * deterministic and avoids duplicate parameters for criterion which do
       * not require attributes.  Criterion which require attributes in order
       * to have a complete sense are given a unique ID based on the attribute
       */
      const param = node.set('parameterId', this.paramId(node));
      this.actions.addParameter(param);
    }
  }

  selectable(node) {
    return node.get('selectable', false);
  }

  isSelected(node) {
    const noAttr = !needsAttributes(node);
    const selectedIDs = this.selections.map(n => n.get('parameterId'));
    const selected = selectedIDs.includes(this.paramId(node));
    return noAttr && selected;
  }

  nonZeroCount(node) {
    return node.get('count', 0) > 0;
  }

  displayName(node) {
    const isDemo = node.get('type', '').match(/^DEMO.*/i);
    const nameIsCode = node.get('name', '') === node.get('code', '');
    return nameIsCode || isDemo
      ? ''
      : node.get('name', '');
  }

  displayCode(node) {
    return node.get('type', '').match(/^DEMO.*/i)
      ? node.get('name', '')
      : node.get('code', '');
  }
}
