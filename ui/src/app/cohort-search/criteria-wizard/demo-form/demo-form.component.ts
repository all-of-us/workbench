import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS, is} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';
import {Subscription} from 'rxjs/Subscription';

import {activeParameterList, CohortSearchActions} from '../../redux';

import {Attribute, CohortBuilderService, Criteria} from 'generated';

function sortByCountThenName(critA, critB) {
  // Sorts by Count, then secondarily by Name
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
  @select(activeParameterList) selected$;

  readonly minAge = 0;
  readonly maxAge = 120;
  loading = false;
  subscription = new Subscription();

  demoForm = new FormGroup({
    ageMin: new FormControl(0),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    genders: new FormControl(),
    races: new FormControl(),
    deceased: new FormControl(),
  });

  get genders() { return this.demoForm.get('genders'); }
  get races() { return this.demoForm.get('races'); }
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

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

    this.selected$.first().subscribe(selections => {
      console.log('DemoForm.ngOnInit selections');
      console.dir(selections);
      const genders = selections.filter(s => s.get('subtype') === 'GEN');
      this.genders.setValue(genders.toArray());
      console.log(this.genders.value);
    });

    this.demoForm.valueChanges.withLatestFrom(this.selected$).subscribe(console.log);

    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;

    const calls = ['DEC', 'GEN', 'RACE', 'AGE'].map(code => this.api
      .getCriteriaByTypeAndSubtype(cdrid, 'DEMO', code)
      .map(response => {
        const items = response.items;
        items.sort(sortByCountThenName);
        return items;
      })
    );

    forkJoin(...calls).subscribe(([dec, gen, race, age]) => {
      this.genderNodes = fromJS(gen);
      this.raceNodes = fromJS(race);
      this.deceasedNode = fromJS(dec[0]);
      this.ageNode = fromJS(age[0]);
      this.loading = false;
    });

    /*
     * We want the two inputs to mirror the slider, so here we're wiring all
     * three inputs together using the valueChanges Observable and the
     * emitEvent option.  Setting emitEvent to false will prevent the other
     * Observables from firing when a control is updated this way, hence
     * preventing any infinite update cycles.
     */
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    const range = this.demoForm.get('ageRange');

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

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
    let hasSelection = false;


    if (this.deceased.value) {
      console.log('Processing deceased status');
      const paramId = `param${this.deceasedNode.get('id')}`;
      const param = this.deceasedNode.set('parameterId', paramId);
      this.actions.addParameter(param);
      hasSelection = true;
    }

    if (this.genders.value) {
      this.genders.value.map(node => {
        console.log(`Processing gender ${node}`);
        const paramId = `param${node.get('id', node.get('code'))}`;
        const param = node.set('parameterId', paramId);
        this.actions.addParameter(param);
        hasSelection = true;
      });
    }

    if (this.races.value) {
      this.races.value.map(node => {
        console.log(`Processing race ${node}`);
        const paramId = `param${node.get('id', node.get('code'))}`;
        const param = node.set('parameterId', paramId);
        this.actions.addParameter(param);
        hasSelection = true;
      });
    }

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
        hasSelection = true;
      }
    }

    if (hasSelection) {
      this.actions.finishWizard();
    }
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
      return A.get('id') === B.get('id');
    }
  }
}
