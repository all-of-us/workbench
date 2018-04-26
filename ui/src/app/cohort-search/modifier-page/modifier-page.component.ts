import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeModifierList,
  CohortSearchActions,
  CohortSearchState,
} from '../redux';

@Component({
    selector: 'crit-modifier-page',
    templateUrl: './modifier-page.component.html',
    styleUrls: ['./modifier-page.component.css']
})
export class ModifierPageComponent implements OnInit, OnDestroy {
  @select(activeModifierList) modifiers$;

  existing = List();
  subscription: Subscription;

  readonly modifiers = [{
    name: 'ageAtEvent',
    label: 'Age At Event',
    inputType: 'number',
    modType: 'AGE_AT_EVENT',
    operators: [{
      name: 'Greater Than or Equal To',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }, {
      name: 'Less Than or Equal To',
      value: 'LESS_THAN_OR_EQUAL_TO',
    }, {
      name: 'Between',
      value: 'BETWEEN',
    }]
  }, {
    name: 'eventDate',
    label: 'Event Date',
    inputType: 'date',
    modType: 'EVENT_DATE',
    operators: [{
      name: 'Is On or Before',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }, {
      name: 'Is On or After',
      value: 'LESS_THAN_OR_EQUAL_TO',
    }, {
      name: 'Is Between',
      value: 'BETWEEN',
    }]
  }, {
    name: 'hasOccurrences',
    label: 'Has Occurrences',
    inputType: 'number',
    modType: 'NUM_OF_OCCURRENCES',
    operators: [{
      name: 'N or More',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }]
  }];

  form = new FormGroup({
    ageAtEvent: new FormGroup({
      operator: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
    hasOccurrences: new FormGroup({
      operator: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
    eventDate: new FormGroup({
      operator: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
  });

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.modifiers$.subscribe(mods => this.existing = mods);

    // This reseeds the form with existing data if we're editing an existing group
    this.subscription.add(this.modifiers$.first().subscribe(mods => {
      mods.forEach(mod => {
        const meta = this.modifiers.find(_mod => mod.get('name') === _mod.modType);
        if (meta) {
          this.form.get(meta.name).setValue({
            operator: mod.get('operator'),
            valueA: mod.getIn(['operands', 0]),
            valueB: mod.getIn(['operands', 1]),
          }, {emitEvent: false});
        }
      });
    }));

    this.subscription.add(this.form.valueChanges
      .do(console.log)
      .map(vals => this.currentMods(vals))
      .subscribe(newMods => {
        console.log(this.existing);
        console.log(newMods);

        /*
         * NOTE: the way this process works is basically as follows: 1) compute
         * a modifier per modifier category 2) merge those with the existing
         * modifiers 3) check for any existing modifiers that no longer exist
         * and remove them 4) iterate through the new mods and add them
         *
         * This is a "good enough for now" kind of thing.  If it gets too slow,
         * then the faster way would be to have an "update mods" action" and
         * move all the work into the reducer, effectively ending the story at
         * step two.  Leaving it this way, though, gives us some flexibility
         * with individual modifiers if we need it.
         */
        this.existing
          .filter(mod => !newMods.includes(mod))
          .forEach(mod => this.actions.removeModifier(mod));

        // dispatch the new
        newMods
          .filter(mod => !!mod)
          .filter(mod => !this.existing.includes(mod))
          .forEach(mod => this.actions.addModifier(mod));
      })
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  currentMods(vals) {
    return this.modifiers.map(({name, inputType, modType}) => {
      const {operator, valueA, valueB} = vals[name];
      const between = operator === 'BETWEEN';
      if (!operator || !valueA || (between && !valueB)) {
        return ;
      }
      const operands = [valueA];
      if (between) { operands.push(valueB); }
      return fromJS({name: modType, operator, operands});
    });
  }

  showValueB(modName) {
    return this.form.get([modName, 'operator']).value === 'BETWEEN';
  }
}
