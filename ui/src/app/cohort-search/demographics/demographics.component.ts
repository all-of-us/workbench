import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

import {ageCountStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
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
// Mocked Feature Flag. TODO remove once server-side config is in place. Need to FF the Age title returned from typeToTitle also
  enableCBAgeOptions = true;
  ageTypes = [
    {label: 'Current Age', type: AttrName.AGE.toString()},
    {label: 'Age at Consent', type: 'AGE_AT_CONSENT'}, // TODO replace type with enum value once RW-4509 is complete
    {label: 'Age at CDR Date', type: 'AGE_AT_CDR'} // TODO replace type with enum value once RW-4509 is complete
  ];

    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    ageType: new FormControl(AttrName.AGE.toString()),
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
  deceasedNode: any;

  nodes = [];
  ageNodes: any;
  selections: Array<any>;
  count: number;
  ageCount: number;
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
      this.loadAgeNodesFromApi();
    } else {
      this.loadNodesFromApi();
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const {item: {searchParameters}, type} = this.wizard;
    this.loading = true;
    cohortBuilderApi().getCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), type).then(response => {
      this.nodes = response.items
        .filter(item => item.parentId !== 0)
        .sort(sortByCountThenName)
        .map(node => {
          node['parameterId'] = `param${node.conceptId || node.code}`;
          return node;
        });
      if (searchParameters.length) {
        this.calculate(true);
      }
      this.loading = false;
    });
  }

  async loadAgeNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.loading = true;
    const promises = [
      cohortBuilderApi().getCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), CriteriaType.DECEASED.toString()).then(response => {
        this.deceasedNode = {...response.items[0], parameterId: 'param-dec'};
      })
    ];
    if (this.enableCBAgeOptions) {
      const initialValue = {[AttrName.AGE.toString()]: [], 'AGE_AT_CONSENT': [], 'AGE_AT_CDR': []};
      promises.push(
        cohortBuilderApi().findAgeTypeCounts(+cdrVersionId).then(response => {
          this.ageNodes = response.items.reduce((acc, item) => {
            acc[item.ageType].push(item);
            return acc;
          }, initialValue);
        })
      );
    }
    await Promise.all(promises);
    this.calculateAge();
    this.loading = false;
  }

  loadOptions(nodes: any) {
    const params = this.wizard.item.searchParameters;
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
      const wizard = this.wizard;
      wizard.item.searchParameters.push(this.selectedNode);
      const selections = [this.ageNode.parameterId, ...this.selections];
      selectionsStore.next(selections);
      wizardStore.next(wizard);
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
      if (this.enableCBAgeOptions) {
        setTimeout(() => this.centerAgeCount(), 300);
      } else {
        this.count = null;
      }
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
        if (!this.enableCBAgeOptions) {
          this.count = null;
        }
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
        if (!this.enableCBAgeOptions) {
          this.count = null;
        }
      }
    }));
    this.subscription.add(this.demoForm.get('ageType').valueChanges.subscribe(() => this.calculateAge()));
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
    let attributes;
    if (params.length && params[0].type === CriteriaType.AGE) {
      attributes = params[0].attributes;
      const range = params[0].attributes[0].operands.map(op => parseInt(op, 10));
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
      this.count = this.wizard.count;
    } else {
      attributes = [{
        name: AttrName.AGE,
        operator: Operator.BETWEEN,
        operands: [minAge.toString(), maxAge.toString()]
      }];
    }
    this.selectedNode = {
      ...this.ageNode,
      name: `Age In Range ${attributes[0].operands[0]} - ${attributes[0].operands[1]}`,
      attributes,
    };
    if (!params.length) {
      const wizard = this.wizard;
      wizard.item.searchParameters.push(this.selectedNode);
      const selections = [this.ageNode.parameterId, ...this.selections];
      selectionsStore.next(selections);
      wizardStore.next(wizard);
    }
    if (!this.enableCBAgeOptions) {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      if (!ageCountStore.getValue()[cdrVersionId]) {
        // Get total age count for this cdr version if it doesn't exist in the store yet
        this.calculateAge(true);
      } else if (this.setTotalAge) {
        this.count = ageCountStore.getValue()[cdrVersionId];
      }
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
    const existent = this.wizard.item.searchParameters.find(s => s.type === CriteriaType.DECEASED.toString());
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

  centerAgeCount() {
    if (this.enableCBAgeOptions) {
      this.calculateAge();
      const slider = <HTMLElement>document.getElementsByClassName('noUi-connect')[0];
      const wrapper = document.getElementById('count-wrapper');
      const count = document.getElementById('age-count');
      wrapper.setAttribute(
        'style', 'width: ' + slider.offsetWidth + 'px; left: ' + slider.offsetLeft + 'px;'
      );
      // set style properties also for cross-browser compatibility
      wrapper.style.width = slider.offsetWidth.toString();
      wrapper.style.left = slider.offsetLeft.toString();
      if (!!count && slider.offsetWidth < count.offsetWidth) {
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

  calculateAge(init?: boolean) {
    if (this.enableCBAgeOptions) {
      const ageType = this.demoForm.get('ageType').value;
      const min = this.demoForm.get('ageMin').value;
      const max = this.demoForm.get('ageMax').value;
      this.ageCount = this.ageNodes[ageType]
        .filter(node => node.age >= min && node.age <= max)
        .reduce((acc, node) => acc + node.count, 0);
    } else {
      if (!init || this.setTotalAge) {
        this.calculating = true;
      }
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const parameter = init ? {
        ...this.ageNode,
        name: `Age In Range ${minAge} - ${maxAge}`,
        attributes: [{
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [minAge.toString(), maxAge.toString()]
        }],
      } : this.selectedNode;
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: DomainType.PERSON.toString(),
            searchParameters: [mapParameter(parameter)],
            modifiers: []
          }],
          temporal: false
        }]
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(response => {
        if (init) {
          const ageCounts = ageCountStore.getValue();
          ageCounts[cdrVersionId] = response;
          ageCountStore.next(ageCounts);
          if (this.setTotalAge) {
            this.count = response;
          }
        } else {
          this.count = response;
        }
        this.calculating = false;
      }, (err) => {
        console.error(err);
        this.calculating = false;
      });
    }
  }

  get noSexData() {
    return !this.loading && this.wizard.type === CriteriaType.SEX && this.nodes.length === 0;
  }

  get showPreview() {
    return !this.loading
      && (this.selections && this.selections.length)
      && !(this.wizard.type === CriteriaType.AGE && this.enableCBAgeOptions);
  }

  // Checks if form is in its initial state and if a count already exists before setting the total age count
  get setTotalAge() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    return min.value === minAge && max.value === maxAge && !this.count;
  }
}
