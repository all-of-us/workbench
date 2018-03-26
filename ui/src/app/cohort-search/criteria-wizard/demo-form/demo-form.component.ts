import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../../redux';

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

  readonly minAge = 0;
  readonly maxAge = 120;
  subscription: Subscription;

  demoForm = new FormGroup({
    ageMin: new FormControl(0),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    genders: new FormControl(),
    races: new FormControl(),
    deceased: new FormControl(),
  });

  // Additional configuration
  ageRangeConfig = {
    behaviour: 'drag',
    connect: true,
    range: {min: this.minAge, max: this.maxAge},
    step: 1,
    // pips: {
    //   mode: 'steps',
    //   density: 5
    //   filter: n => n % 5 ? 0 : 1;
    // },
  };

  age: Criteria;
  deceased: Criteria;
  genders: Criteria[] = [];
  races: Criteria[] = [];

  loading = false;

  constructor(
    private route: ActivatedRoute,
    private api: CohortBuilderService,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.loading = true;
    this.demoForm.valueChanges.subscribe(console.log);
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
      this.genders = gen;
      this.races = race;
      this.deceased = dec[0];
      this.age = age[0];
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

    this.subscription = range.valueChanges.subscribe(([lo, hi]) => {
      min.setValue(lo, {emitEvent: false});
      max.setValue(hi, {emitEvent: false});
    });

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...range.value];
      range.setValue([value, hi], {emitEvent: false});
    }));

    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...range.value];
      range.setValue([lo, value], {emitEvent: false});
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

    const deceased = this.demoForm.get('deceased');
    if (deceased.value) {
      console.log('Processing deceased status');
      const param = fromJS(this.deceased)
        .set('parameterId', `param${this.deceased.id}`);
      this.actions.addParameter(param);
      hasSelection = true;
    }

    const gender = this.demoForm.get('genders');
    if (gender.value) {
      gender.value.map(node => {
        console.log(`Processing gender ${node}`);
        const id = `param${node.id || node.code}`;
        const param = fromJS(node).set('parameterId', id);
        this.actions.addParameter(param);
        hasSelection = true;
      });
    }

    const race = this.demoForm.get('races');
    if (race.value) {
      race.value.map(node => {
        console.log(`Processing race ${node}`);
        const id = `param${node.id || node.code}`;
        const param = fromJS(node).set('parameterId', id);
        this.actions.addParameter(param);
        hasSelection = true;
      });
    }

    const ageRange = this.demoForm.get('ageRange');
    if (ageRange.value) {
      const [ageLow, ageHigh] = this.demoForm.get('ageRange').value;
      if (ageHigh < 120 || ageLow > 0) {
        const attr = fromJS(<Attribute>{
          operator: 'between',
          operands: [
            ageLow  || 0,
            ageHigh || 120,
          ]
        });
        const id = `param${this.age.id || this.age.code}`;
        const param = fromJS(this.age)
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
}
