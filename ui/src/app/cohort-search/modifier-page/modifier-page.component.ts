import {NgRedux, select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormArray, FormControl, FormGroup} from '@angular/forms';
import { DomainType, ModifierType } from 'generated';
import {fromJS, List, Map} from 'immutable';
import * as moment from 'moment';
import {Subscription} from 'rxjs/Subscription';
import {CRITERIA_TYPES} from '../constant';
import {
  activeCriteriaType,
  activeModifierList,
  CohortSearchActions,
  CohortSearchState,
  criteriaChildren,
  previewStatus
} from '../redux';


@Component({
    selector: 'crit-modifier-page',
    templateUrl: './modifier-page.component.html',
    styleUrls: ['./modifier-page.component.css']
})
export class ModifierPageComponent implements OnInit, OnDestroy {
  @select(activeCriteriaType) ctype$;
  @select(activeModifierList) modifiers$;
  @select(previewStatus) preview$;
  formChanges = false;
  dateValueA: any;
  dateValueB: any;
  ctype: string;
  existing = List();
  preview = Map();
  subscription: Subscription;
  dropdownOption = {
    selected: ['', '', '', '']
  };

  readonly modifiers = [{
    name: 'ageAtEvent',
    label: 'Age At Event',
    inputType: 'number',
    modType: ModifierType.AGEATEVENT,
    options: [{
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
    modType: ModifierType.EVENTDATE,
    options: [{
      name: 'Is On or Before',
      value: 'LESS_THAN_OR_EQUAL_TO',
    }, {
      name: 'Is On or After',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }, {
      name: 'Is Between',
      value: 'BETWEEN',
    }]
  }, {
    name: 'hasOccurrences',
    label: 'Has Occurrences',
    inputType: 'number',
    modType: ModifierType.NUMOFOCCURRENCES,
    options: [{
      name: 'N or More',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }]
  }];

  form = new FormGroup({
    ageAtEvent: new FormGroup({
      option: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
    hasOccurrences: new FormGroup({
      option: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
    eventDate: new FormGroup({
      option: new FormControl(),
      valueA: new FormControl(),
      valueB: new FormControl(),
    }),
  });

  constructor(
    private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>
  ) {}

  ngOnInit() {
    this.subscription = this.modifiers$.subscribe(mods => this.existing = mods);
    this.subscription.add(this.ctype$.subscribe(ctype => {
      this.ctype = ctype;
      if ([CRITERIA_TYPES.PM, DomainType.VISIT].indexOf(ctype) === -1) {
        this.modifiers.push({
          name: 'encounters',
          label: 'During Visit Type',
          inputType: null,
          modType: ModifierType.ENCOUNTERS,
          options: []
        })
        this.form.addControl('encounters', new FormGroup({option: new FormControl()}));
      }
    }));
    this.subscription.add(this.ngRedux.select(criteriaChildren(DomainType[DomainType.VISIT], 0))
      .filter(visiTypes => visiTypes.size > 0)
      .subscribe(visitTypes => {
        if (this.modifiers[3]) {
          visitTypes.toJS().forEach(option => {
            if (option.parentId === 0) {
              this.modifiers[3].options.push({name: option.name, value: option.conceptId});
            }
          });
        }
      }));
    this.subscription.add(this.preview$.subscribe(prev => this.preview = prev));

    // This reseeds the form with existing data if we're editing an existing group
    this.subscription.add(this.modifiers$.first().subscribe(mods => {
      mods.forEach(mod => {
        const meta = this.modifiers.find(_mod => mod.get('name') === _mod.modType);
        if (meta) {
          this.form.get(meta.name).patchValue({
            option: mod.get('option'),
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
    selectChange(opt, index, e, mod) {
        this.dropdownOption.selected[index] = opt.name;
        if (e.target.value || this.form.controls.valueA) {
            this.formChanges = true;
        }
        console.log(mod);
        const modForm = <FormArray>this.form.controls[mod.name];
        const valueForm = <FormArray>modForm;
        valueForm.get('option').patchValue(opt.value);
        // if (mod.modType === 'AGE_AT_EVENT') {
        //     const ageAtEventForm = <FormArray>this.form.controls.ageAtEvent;
        //     const valueForm = <FormArray>ageAtEventForm;
        //     valueForm.get('option').patchValue(opt.value);
        // } else if (mod.modType === 'EVENT_DATE') {
        //     const eventDateForm = <FormArray>this.form.controls.eventDate;
        //     const valueForm = <FormArray>eventDateForm;
        //     valueForm.get('option').patchValue(opt.value);
        // } else if (mod.modType === 'NUM_OF_OCCURRENCES') {
        //     const hasOccurrencesForm = <FormArray>this.form.controls.hasOccurrences;
        //   console.log(hasOccurrencesForm);
        //     const valueForm = <FormArray>hasOccurrencesForm;
        //     valueForm.get('option').patchValue(opt.value);
        // } else if (mod.modType === 'ENCOUNTERS') {
        //     const encountersForm = <FormArray>this.form.controls.encounters;
        //     const valueForm = <FormArray>encountersForm;
        //     valueForm.get('option').patchValue(opt.value);
        // }
    }

  currentMods(vals) {
    return this.modifiers.map(({name, inputType, modType}) => {
      const {option, valueA, valueB} = vals[name];
      const between = option === 'BETWEEN';
      if (!option || !valueA || (between && !valueB)) {
        return ;
      }
      if (inputType === 'date') {
          this.dateValueA = moment(valueA, 'MM/DD/YYYY').format('YYYY-MM-DD');
          this.dateValueB = moment(valueB, 'MM/DD/YYYY').format('YYYY-MM-DD');
          const operands = [this.dateValueA];
          if (between) { operands.push(this.dateValueB); }
          return fromJS({name: modType, option, operands});
      } else {
          const operands = [valueA];
          if (between) { operands.push(valueB); }
          return fromJS({name: modType, option, operands});
      }
    });
  }

  requestPreview() {
    this.actions.requestPreview();
  }

  ngOnDestroy() {
      this.subscription.unsubscribe();
    }

}
