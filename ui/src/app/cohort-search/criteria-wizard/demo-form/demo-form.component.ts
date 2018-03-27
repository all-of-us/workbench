import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';
import {Subscription} from 'rxjs/Subscription';

import {activeParameterList, CohortSearchActions} from '../../redux';

import {Attribute, CohortBuilderService, Criteria} from 'generated';

/* Demographic Criteria Subtypes and Constants */
const AGE = 'AGE';
const DEC = 'DEC';
const GEN = 'GEN';
const RACE = 'RACE';
const minAge = 0;
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
  selector: 'crit-demo-form',
  templateUrl: './demo-form.component.html',
  // Buttons styles picked up from parent (wizard.ts)
  styleUrls: ['./demo-form.component.css'],
})
export class DemoFormComponent implements OnInit {
  @Input() open: boolean;
  @select(activeParameterList) selection$;
  readonly minAge = minAge;
  readonly maxAge = maxAge;
  loading = false;
  subscription = new Subscription();
  hasSelection = false;

  /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(0),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    genders: new FormControl(),
    races: new FormControl(),
    deceased: new FormControl(),
  });
  get genders()  { return this.demoForm.get('genders');  }
  get races()    { return this.demoForm.get('races');    }
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

  /* Storage for the demographics options (fetched via the API) */
  ageNode;
  deceasedNode;
  genderNodes = [];
  raceNodes = [];

  constructor(
    private route: ActivatedRoute,
    private api: CohortBuilderService,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.loading = true;
    this.demoForm.valueChanges.withLatestFrom(this.selection$).subscribe(console.log);
    this.subscription = this.selection$.subscribe(sel => this.hasSelection = sel.size > 0);
    this.initAgeControls();

    this.selection$.first().subscribe(selections => {
      this.initGenders(selections);
      this.initRaces(selections);
      this.initDeceased(selections);
      this.initAgeRange(selections);
      this.loadNodesFromApi();
    });
  }

  loadNodesFromApi() {
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    /*
     * When we fetch the crit nodes we immediately sort by count (secondary
     * sort on name), then map each collection to immutable objects and attach
     * a parameterId.  Parameter ID's should be unique for criteria, so we can
     * use those to determine if we need to add / remove params (e.g. in the
     * edit scenario)
     *
     * We do NOT calculate a param ID for AGE criteria since those are
     * determined by a hash that relies on user input.
     */
    const calls = [DEC, GEN, RACE, AGE].map(code => this.api
      .getCriteriaByTypeAndSubtype(cdrid, 'DEMO', code)
      .map(response => {
        let items = response.items;
        items.sort(sortByCountThenName);
        const nodes = fromJS(items).map(node => {
          if (node.get('subtype') === AGE) {
          } else {
            const paramId = `param${node.get('id', node.get('code'))}`;
            node = node.set('parameterId', paramId);
          }
          return node;
        });
        return nodes.size > 1 ? nodes : nodes.get(0);
      })
    );
    forkJoin(...calls).subscribe(([dec, gen, race, age]) => {
      this.deceasedNode = dec;
      this.genderNodes = gen;
      this.raceNodes = race;
      this.ageNode = age;
      this.loading = false;
    });
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
    }));
    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      this.ageRange.setValue([value, hi], {emitEvent: false});
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      this.ageRange.setValue([lo, value], {emitEvent: false});
    }));
  }

  initDeceased(selections) {
    const existent = selections.find(s => s.get('subtype') === DEC);
    if (existent !== undefined) {
      this.deceased.setValue(true);
    }
    this.subscription.add(this.deceased.valueChanges.subscribe(includeDeceased => {
      console.log(`Deceased status change: ${includeDeceased}`);
      if (!this.deceasedNode) {
        console.log('No node from which to make parameter for deceased status');
        return ;
      }
      includeDeceased
        ? this.actions.addParameter(this.deceasedNode)
        : this.actions.removeParameter(this.deceasedNode.get('parameterId'));
    }));
  }

  initGenders(selections) {
    const genDiff = this.genders.valueChanges.pairwise().map(([prior, latter]) => {
      const add = latter.filter(item => !prior.includes(item));
      const del = prior.filter(item => !latter.includes(item));
      return [add, del];
    });
    this.subscription.add(genDiff.subscribe(([add, del]) => {
      add.forEach(item => this.actions.addParameter(item));
      del.forEach(item => this.actions.removeParameter(item.get('parameterId')));
    }));
    const initialGenders = selections.filter(s => s.get('subtype') === GEN).toArray();
    this.genders.setValue(initialGenders);
  }

  initRaces(selections) {
    const raceDiff = this.races.valueChanges.pairwise().map(([prior, latter]) => {
      const add = latter.filter(item => !prior.includes(item));
      const del = prior.filter(item => !latter.includes(item));
      return [add, del];
    });
    this.subscription.add(raceDiff.subscribe(([add, del]) => {
      add.forEach(item => this.actions.addParameter(item));
      del.forEach(item => this.actions.removeParameter(item.get('parameterId')));
    }));
    const initialRaces = selections.filter(s => s.get('subtype') === RACE).toArray();
    this.races.setValue(initialRaces);
  }

  initAgeRange(selections) {
    const existent = selections.find(s => s.get('subtype') === AGE);
    if (existent) {
      const range = existent.getIn(['attribute', 'operands']).toArray();
      this.ageRange.setValue(range);
      this.demoForm.get('ageMin').setValue(range[0]);
      this.demoForm.get('ageMax').setValue(range[1]);
    }
    const ageDiff = this.ageRange.valueChanges;
    /*
    if (this.ageRange.value) {
      const [ageLow, ageHigh] = this.ageRange.value;
      if (ageHigh < 120 || ageLow > 0) {
        const attr = fromJS(<Attribute>{
          operator: 'between',
          operands: [
            ageLow  || 0,
            ageHigh || 120,
          ]
        });
        const param = this.ageNode
          .set('parameterId', attr.hashCode())
          .set('attribute', attr);
        this.actions.addParameter(param);
      }
    }
     */
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
    if (this.ageRange.value) {
      const [ageLow, ageHigh] = this.ageRange.value;
      if (ageHigh < 120 || ageLow > 0) {
        const attr = fromJS(<Attribute>{
          operator: 'between',
          operands: [
            ageLow  || 0,
            ageHigh || 120,
          ]
        });
        const param = this.ageNode
          .set('parameterId', attr.hashCode())
          .set('attribute', attr);
        this.actions.addParameter(param);
      }
    }
    this.actions.finishWizard();
  }

  /*
   * ClrModal doesn't emit an event specific to cancellation like the wizard
   * does, so here we'll intercept closures to make sure we're running
   * this.onCancel when the user hits <ESC> or the X in the corner as well as
   * the `Cancel` button
   */
  openChange(value: boolean) {
    if (!value) {
      this.onCancel();
    }
  }

  /*
   * When editing an existing set of demographics options, this function is
   * critical for telling the select which option boxes to highlight.  See the
   * [compareWith] function on each `option` element.
   */
  optionComparator(A, B): boolean {
    // Sometimes (for some reason) A or B is undefined... not sure how or why :/
    if (A && B) {
      const idMatch = A.get('id') === B.get('id');
      const subtypeAndCodeMatch =
        A.get('subtype') === B.get('subtype')
        && A.get('code') == B.get('code');
      return idMatch || subtypeAndCodeMatch;
    }
  }
}
