import {select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';
import {Subscription} from 'rxjs/Subscription';

import {activeParameterList, CohortSearchActions} from '../../redux';

import {Attribute, CohortBuilderService} from 'generated';

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
export class DemoFormComponent implements OnInit, OnDestroy {
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
    // Set back to false at the end of loadNodesFromApi (i.e. the end of the
    // initialization routine)
    this.loading = true;

    this.subscription = this.selection$.subscribe(sel => this.hasSelection = sel.size > 0);
    this.initAgeControls();

    this.selection$.first().subscribe(selections => {
      /*
       * Each subtype of DEMO requires subtly different initialization, which
       * is handled by special-case methods which each receive any selected
       * criteria already in the state (i.e. if we're editing a search group
       * item).  Finally we load the relevant criteria from the API.
       */
      this.initGenders(selections);
      this.initRaces(selections);
      this.initDeceased(selections);
      this.initAgeRange(selections);
      this.loadNodesFromApi();
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    /*
     * Each subtype's possible criteria is loaded via the API.  Race and Gender
     * criteria nodes become options in their respective dropdowns; deceased
     * and age are used as templates for constructing relevant seach
     * parameters.  Upon load we immediately map the criteria to immutable
     * objects complete with deterministically generated `parameterId`s and
     * sort them by count, then by name.
     */
    const calls = [AGE, DEC, GEN, RACE].map(code => this.api
      .getCriteriaByTypeAndSubtype(cdrid, 'DEMO', code)
      .map(response => {
        const items = response.items;
        items.sort(sortByCountThenName);
        const nodes = fromJS(items).map(node => {
          if (node.get('subtype') !== AGE) {
            const paramId = `param${node.get('id', node.get('code'))}`;
            node = node.set('parameterId', paramId);
          }
          return node;
        });
        return nodes.size > 1 ? nodes : nodes.get(0);
      })
    );
    forkJoin(...calls).subscribe(([age, dec, gen, race]) => {
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
  initAgeRange(selections) {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');

    const existent = selections.find(s => s.get('subtype') === AGE);
    if (existent) {
      const range = existent.getIn(['attribute', 'operands']).toArray();
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
    }

    const selectedAge = this.selection$
      .map(selectedNodes => selectedNodes
        .find(node => node.get('subtype') === AGE)
      );

    const ageDiff = this.ageRange.valueChanges
      .debounceTime(250)
      .distinctUntilChanged()
      .map(([lo, hi]) => {
        const attr = fromJS(<Attribute>{
          operator: 'between',
          operands: [lo, hi],
        });
        const paramId = attr.hashCode();
        return this.ageNode
          .set('parameterId', paramId)
          .set('attribute', attr);
      })
      .withLatestFrom(selectedAge)
      .filter(([newNode, oldNode]) => {
        if (oldNode) {
          return oldNode.get('parameterId') !== newNode.get('parameterId');
        }
        return true;
      });
    this.subscription.add(ageDiff.subscribe(([newNode, oldNode]) => {
      if (oldNode) {
        this.actions.removeParameter(oldNode.get('parameterId'));
      }
      this.actions.addParameter(newNode);
    }));
  }

  initDeceased(selections) {
    const existent = selections.find(s => s.get('subtype') === DEC);
    if (existent !== undefined) {
      this.deceased.setValue(true);
    }
    this.subscription.add(this.deceased.valueChanges.subscribe(includeDeceased => {
      if (!this.deceasedNode) {
        console.warn('No node from which to make parameter for deceased status');
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

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
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
        && A.get('code') === B.get('code');
      return idMatch || subtypeAndCodeMatch;
    }
  }
}
