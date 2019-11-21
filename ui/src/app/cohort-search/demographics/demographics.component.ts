import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

import {cdrVersionStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {mapParameter, typeToTitle} from 'app/cohort-search/utils';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {AttrName, CriteriaType, DomainType, Operator} from 'generated/fetch';

const minAge = 18;
const maxAge = 120;

/*
 * Sorts a plain JS array of plain JS objects first by a 'count' key and then
 * by a 'name' key
 */
function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
        ? (critA.name > critB.name ? 1 : -1)
        : diff;
}

@Component({
  selector: 'crit-list-demographics',
  templateUrl: './demographics.component.html',
    // Buttons styles picked up from parent (wizard.ts)
  styleUrls: [
    './demographics.component.css',
    '../../styles/buttons.css',
  ]
})
export class DemographicsComponent implements OnInit, OnDestroy {
  readonly criteriaType = CriteriaType;
  readonly minAge = minAge;
  readonly maxAge = maxAge;
  loading = false;
  calculating = false;
  subscription = new Subscription();
  selectedNode: any;

    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    deceased: new FormControl(),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

  ageNode = {
    hasAncestorData: false,
    attributes: [],
    code: '',
    domainId: DomainType.PERSON,
    group: false,
    name: 'Age',
    parameterId: 'age-param',
    isStandard: true,
    type: CriteriaType.AGE,
    value: ''
  };
  deceasedNode;

  nodes = [];
  selections: Array<any>;
  count: any;
  wizard: any;
  cdrCount: number;

  ngOnInit() {
    wizardStore.subscribe(wizard => this.wizard = wizard);
    selectionsStore.subscribe(selections => {
      this.selections = selections;
      if (this.wizard && this.wizard.type !== CriteriaType.AGE) {
        this.calculate();
      }
    });
    this.cdrCount = cdrVersionStore.getValue() ? cdrVersionStore.getValue().numParticipants : null;
    if (this.wizard.type === CriteriaType.AGE) {
      this.initAgeControls();
      this.initDeceased();
      this.initAgeRange();
      this.loadNodesFromApi(CriteriaType[CriteriaType.DECEASED]);
    } else {
      this.loadNodesFromApi();
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi(type?: string) {
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    type = type || this.wizard.type;
    /*
     * Each subtype's possible criteria is loaded via the API.  Race and Gender
     * criteria nodes become options in their respective dropdowns; deceased
     * and age are used as templates for constructing relevant search
     * parameters.  Upon load we sort them by count, then by name.
     */
    this.loading = true;
    cohortBuilderApi().getCriteriaBy(cdrid, DomainType[DomainType.PERSON], type).then(response => {
      const items = response.items.filter(item => item.parentId !== 0 || type === CriteriaType[CriteriaType.DECEASED]);
      if (type !== CriteriaType[CriteriaType.AGE]) {
        items.sort(sortByCountThenName);
      }
      const nodes = items.map(node => {
        if (type !== CriteriaType[CriteriaType.AGE]) {
          node['parameterId'] = type === CriteriaType[CriteriaType.DECEASED] ? 'param-dec' :
            `param${node.conceptId || node.code}`;
        }
        return node;
      });
      this.loadOptions(nodes, type);
    });
  }

  loadOptions(nodes: any, type: string) {
    const params = this.wizard.item.searchParameters;
    if (type === CriteriaType[CriteriaType.DECEASED]) {
      const attributes = params.length && params[0].type === CriteriaType.AGE
        ? params[0].attributes
        : [{
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [minAge.toString(), maxAge.toString()]
        }];
      this.selectedNode = {
        ...this.ageNode,
        name: `Age In Range ${minAge} - ${maxAge}`,
        attributes,
      };
      if (!params.length) {
        this.count = this.cdrCount;
        const wizard = this.wizard;
        wizard.item.searchParameters.push(this.selectedNode);
        const selections = [this.ageNode.parameterId, ...this.selections];
        selectionsStore.next(selections);
        wizardStore.next(wizard);
      }
      this.deceasedNode = nodes[0];
    } else {
      this.nodes = nodes;
      if (params.length) {
        this.calculate(true);
      }
    }
    this.loading = false;
  }

    /*
      * We want the two inputs to mirror the slider, so here we're wiring all
      * three inputs together using the valueChanges Observable and the
      * emitEvent option.  Setting emitEvent to false will prevent the other
      * Observables from firing when a control is updated this way, hence
      * preventing any infinite update cycles.
      */
  initAgeControls() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    this.subscription.add(this.ageRange.valueChanges.subscribe(([lo, hi]) => {
      min.setValue(lo, {emitEvent: false});
      max.setValue(hi, {emitEvent: false});
      this.count = null;
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
        this.count = null;
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
        this.count = null;
      }
    }));
  }

  checkMax() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (max.value < min.value) {
      max.setValue(min.value);
    }
  }

  checkMin() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (min.value > max.value) {
      min.setValue(max.value);
    } else if (min.value < this.minAge) {
      min.setValue(this.minAge);
    }
  }

  initAgeRange() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    const params = this.wizard.item.searchParameters;
    if (params.length && params[0].type === CriteriaType.AGE) {
      const range = params[0].attributes[0].operands.map(op => parseInt(op, 10));
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
    }
    // Get the item count if editing an existing item
    if (this.wizard && this.wizard.count) {
      this.count = this.wizard.count;
    }

    const ageDiff = this.ageRange.valueChanges
      .debounceTime(250)
      .distinctUntilChanged()
      .map(([lo, hi]) => {
        if (lo === minAge && hi === maxAge) {
          this.count = this.cdrCount;
        }
        const attr = {
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [lo.toString(), hi.toString()]
        };
        return {
          ...this.ageNode,
          name: `Age In Range ${lo} - ${hi}`,
          attributes: [attr],
        };
      }).subscribe(newNode => {
        const {parameterId} = this.selectedNode;
        this.selectedNode = newNode;
        const wizard = this.wizard;
        wizard.item.searchParameters = [this.selectedNode];
        wizardStore.next(wizard);
        if (parameterId !== newNode.parameterId) {
          const selections = [parameterId];
          selectionsStore.next(selections);
        }
      });
    this.subscription.add(ageDiff);
  }

  initDeceased() {
    const existent = this.wizard.item.searchParameters.find(s => s.type === CriteriaType[CriteriaType.DECEASED]);
    if (existent !== undefined) {
      this.deceased.setValue(true);
      this.count = this.wizard.count;
    }
    this.subscription = this.deceased.valueChanges.subscribe(includeDeceased => {
      triggerEvent('Cohort Builder Search', 'Click', 'Demo - Age/Deceased - Deceased Box');
      const wizard = this.wizard;
      wizard.item.searchParameters = [includeDeceased ? this.deceasedNode : this.selectedNode];
      const selections = [includeDeceased ? this.deceasedNode.parameterId : this.selectedNode.parameterId];
      wizardStore.next(wizard);
      selectionsStore.next(selections);
      this.count = includeDeceased ? this.deceasedNode.count : null;
    });
  }

  selectOption = (opt: any) => {
    triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(opt.type)} - ${opt.name}`);
    if (!this.selections.includes(opt.parameterId)) {
      const wizard = this.wizard;
      wizard.item.searchParameters.push({...opt, name: `${typeToTitle(opt.type)} - ${opt.name}`});
      const selections = [...this.selections, opt.parameterId];
      wizardStore.next(wizard);
      selectionsStore.next(selections);
    }
  }

  calculate(init?: boolean) {
    this.count = 0;
    this.wizard.item.searchParameters.forEach(sp => {
      if (init) {
        const node = this.nodes.find(n => n.conceptId === sp.conceptId);
        if (node) {
          sp.count = node.count;
        }
      }
      this.count += sp.count;
    });
  }

  calculateAge() {
    this.calculating = true;
    const cdrVersionId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const request = {
      excludes: [],
      includes: [{
        items: [{
          type: DomainType[DomainType.PERSON],
          searchParameters: [mapParameter(this.selectedNode)],
          modifiers: []
        }],
        temporal: false
      }]
    };
    cohortBuilderApi().countParticipants(cdrVersionId, request).then(response => {
      this.count = response;
      this.calculating = false;
    }, (err) => {
      console.error(err);
      this.calculating = false;
    });
  }

  get noSexData() {
    return !this.loading && this.wizard.type === CriteriaType.SEX && this.nodes.length === 0;
  }

  get showPreview() {
    return !this.loading && (this.selections && this.selections.length);
  }
}
