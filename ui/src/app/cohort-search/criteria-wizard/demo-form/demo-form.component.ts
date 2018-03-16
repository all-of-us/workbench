import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS} from 'immutable';
import {forkJoin} from 'rxjs/observable/forkJoin';

import {CohortSearchActions} from '../../redux';

import {CohortBuilderService, Criteria} from 'generated';


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
  readonly MAX_AGE = 120;
  readonly MIN_AGE = 0;

  demoForm = new FormGroup({
    genders: new FormControl(),
    races: new FormControl(),
    deceased: new FormControl(),
  });

  deceased: Criteria[] = [];
  genders: Criteria[] = [];
  races: Criteria[] = [];

  constructor(
    private route: ActivatedRoute,
    private api: CohortBuilderService,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.demoForm.valueChanges.subscribe(console.log);
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;

    const calls = ['DEC', 'GEN', 'RACE'].map(code => this.api
      .getCriteriaByTypeAndSubtype(cdrid, 'DEMO', code)
      .map(response => {
        const items = response.items;
        items.sort(sortByCountThenName);
        return items;
      })
    );

    forkJoin(...calls).subscribe(([dec, gen, race]) => {
      this.deceased = dec;
      this.genders = gen;
      this.races = race;
    });
  }

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
    let hasSelection = false;
    // transform the form into a set of criteria selections
    if (this.demoForm.get('deceased').value === 'isDeceased') {
      console.log('Filtering by deadness');
      const node = this.deceased.find(node => node.code === 'Deceased');
      if (node) {
        const id = `param${node.id || node.code}`;
        const param = fromJS(node).set('parameterId', id);
        this.actions.addParameter(param);
        hasSelection = true;
      }
    }

    const gender = this.demoForm.get('genders');
    const race = this.demoForm.get('races');

    gender.value && gender.value.map(node => {
      console.log(`Processing gender ${node}`);
      const id = `param${node.id || node.code}`;
      const param = fromJS(node).set('parameterId', id);
      this.actions.addParameter(param);
      hasSelection = true;
    });

    race.value && race.value.map(node => {
      console.log(`Processing race ${node}`);
      const id = `param${node.id || node.code}`;
      const param = fromJS(node).set('parameterId', id);
      this.actions.addParameter(param);
      hasSelection = true;
    });

    if (hasSelection) {
      this.actions.finishWizard();
    }
  }
}
