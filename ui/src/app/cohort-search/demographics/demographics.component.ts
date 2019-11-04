import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {typeToTitle} from 'app/cohort-search/utils';
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
  subscription = new Subscription();
  selectedNode: any;
  ageClicked = false;


    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    deceased: new FormControl(),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

    /* Storage for the demographics options (fetched via the API) */
  ageNode: any;
  ageNodes: Array<any>;
  ageCount: number;
  deceasedNode;

  nodes = [];
  selections: Array<any>;
  count: any;
  wizard: any;

  ngOnInit() {
    wizardStore.subscribe(wizard => this.wizard = wizard);
    selectionsStore.subscribe(selections => {
      this.selections = selections;
      if (this.wizard && this.wizard.type !== CriteriaType.AGE) {
        this.calculate();
      }
    });
    if (this.wizard.type === CriteriaType.AGE) {
      this.initAgeControls();
      this.initDeceased();
      this.initAgeRange();
      this.loadNodesFromApi(CriteriaType[CriteriaType.DECEASED]);
    }
    this.loadNodesFromApi();
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
     * and age are used as templates for constructing relevant seach
     * parameters.  Upon load we immediately map the criteria to immutable
     * objects complete with deterministically generated `parameterId`s and
     * sort them by count, then by name.
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
    switch (type) {
      /* Age and Deceased are single nodes we use as templates */
      case CriteriaType[CriteriaType.AGE]:
        this.ageNode = nodes[0];
        this.ageNodes = nodes;
        this.calculateAgeCount();
        const attr = {
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [minAge.toString(), maxAge.toString()]
        };
        const paramId = `age-param${this.ageNode.id}`;
        this.selectedNode = {
          ...this.ageNode,
          name: `Age In Range ${minAge} - ${maxAge}`,
          parameterId: paramId,
          attributes: [attr],
        };
        if (!this.wizard.item.searchParameters.length) {
          const wizard = this.wizard;
          wizard.item.searchParameters.push(this.selectedNode);
          const selections = [paramId, ...this.selections];
          selectionsStore.next(selections);
          wizardStore.next(wizard);
        }
        break;
      case CriteriaType[CriteriaType.DECEASED]:
        this.deceasedNode = nodes[0];
        break;
      default:
        this.nodes = nodes;
        if (this.wizard.item.searchParameters.length) {
          this.calculate(true);
        }
        this.loading = false;
    }
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
      setTimeout(() => this.centerAgeCount(), 300);
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
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

    /*
     * The next four initialization methods do the following: if a value exists
     * for that subtype already (i.e. we're editing), set that value on the
     * relevant form control.  Also set up a subscriber to the observable stream
     * coming from that control's `valueChanges` that will fire ADD_PARAMETER or
     * REMOVE_PARAMETER events as appropriate.
     *
     * The exact ordering of these operations is slightly different per type.
     * For race and gender, we watch the valueChanges stream in pairs so that we
     * can generate added and removed lists, so they get their initialization
     * _after_ the change listener is attached (otherwise we would never detect
     * the _first_ selection, which would be dropped by `pairwise`).
     *
     * For Age, since the slider emits an event with every value, and there can
     * be many values very quickly, we debounce the event emissions by 1/4 of a
     * second.  Furthermore, we generate the correct parameter ID as a hash from
     * the given user input so that we can determine if the age range has changed
     * or not.
     *
     * (TODO: can we reduce all age criterion to 'between'?  Or should we be
     * determining different attributes (operators, really) be examining the
     * bounds and the diff between low and high?  And should we be generating the
     * parameterId by stringifying the attribute (which may be more stable than
     * using a hash?)
     */
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

    const ageDiff = this.ageRange.valueChanges
      .debounceTime(250)
      .distinctUntilChanged()
      .map(([lo, hi]) => {
        const attr = {
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [lo.toString(), hi.toString()]
        };
        const paramId = `age-param${this.ageNode.id}`;
        return {
          ...this.ageNode,
          name: `Age In Range ${lo} - ${hi}`,
          parameterId: paramId,
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
    const existent = this.wizard.item.searchParameters
      .find(s => s.type === CriteriaType[CriteriaType.DECEASED]);
    if (existent !== undefined) {
      this.deceased.setValue(true);
    }
    this.subscription = this.deceased.valueChanges.subscribe(includeDeceased => {
      triggerEvent('Cohort Builder Search', 'Click', 'Demo - Age/Deceased - Deceased Box');
      const wizard = this.wizard;
      wizard.item.searchParameters = [includeDeceased ? this.deceasedNode : this.selectedNode];
      const selections = [
        includeDeceased ? this.deceasedNode.parameterId : this.selectedNode.parameterId
      ];
      wizardStore.next(wizard);
      selectionsStore.next(selections);
    });
  }

  calculateAgeCount() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    let count = 0;
    for (let i = min.value; i <= max.value; i++) {
      const ageNode = this.ageNodes.find(node => node.name === i.toString());
      if (ageNode) {
        count += ageNode.count;
      }
    }
    this.ageCount = count;
    this.loading = false;
  }

  centerAgeCount() {
    if (this.ageNodes) {
      this.calculateAgeCount();
      this.ageClicked = false;
      const slider = <HTMLElement>document.getElementsByClassName('noUi-connect')[0];
      const wrapper = document.getElementById('count-wrapper');
      const count = document.getElementById('age-count');
      wrapper.setAttribute(
        'style', 'width: ' + slider.offsetWidth + 'px; left: ' + slider.offsetLeft + 'px;'
      );
      // set style properties also for cross-browser compatibility
      wrapper.style.width = slider.offsetWidth.toString();
      wrapper.style.left = slider.offsetLeft.toString();
      if (slider.offsetWidth < count.offsetWidth) {
        const margin = (slider.offsetWidth - count.offsetWidth) / 2;
        count.setAttribute('style', 'margin-left: ' + margin + 'px;');
        count.style.marginLeft = margin.toString();
      }
    }
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

  get noSexData() {
    return !this.loading && this.wizard.type === CriteriaType.SEX && this.nodes.length === 0;
  }

  get showPreview() {
    return !this.loading && (this.selections && this.selections.length) && this.wizard.type !== CriteriaType.AGE;
  }
}
