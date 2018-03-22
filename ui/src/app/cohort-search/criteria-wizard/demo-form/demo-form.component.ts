import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';

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

  demoForm = new FormGroup({
    ageHigh: new FormControl(),
    ageLow: new FormControl(),
    genders: new FormControl(),
    races: new FormControl(),
    deceased: new FormControl(),
  });

  age: Criteria;
  deceased: Criteria[] = [];
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
      this.deceased = dec;
      this.genders = gen;
      this.races = race;
      this.age = age[0];
      this.loading = false;
    });
  }

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
    let hasSelection = false;

    if (this.demoForm.get('deceased').value === 'isDeceased') {
      console.log('Filtering by deadness');
      const node = this.deceased.find(_node => _node.code === 'Deceased');
      if (node) {
        const id = `param${node.id || node.code}`;
        const param = fromJS(node).set('parameterId', id);
        this.actions.addParameter(param);
        hasSelection = true;
      }
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

    const ageHigh = this.demoForm.get('ageHigh').value;
    const ageLow = this.demoForm.get('ageLow').value;

    if (ageHigh || ageLow) {
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
