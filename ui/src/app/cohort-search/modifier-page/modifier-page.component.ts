import {select} from '@angular-redux/store';
import {
  AfterContentChecked,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {
  activeCriteriaType,
  activeModifierList,
  CohortSearchActions,
  previewStatus,
} from 'app/cohort-search/redux';
import {dateValidator, integerAndRangeValidator} from 'app/cohort-search/validators';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderService, ModifierType, Operator, TreeType} from 'generated';
import {fromJS, List, Map} from 'immutable';
import * as moment from 'moment';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'crit-modifier-page',
  templateUrl: './modifier-page.component.html',
  styleUrls: ['./modifier-page.component.css']
})
export class ModifierPageComponent implements OnInit, OnDestroy, AfterContentChecked {
  @select(activeCriteriaType) ctype$;
  @select(activeModifierList) modifiers$;
  @select(previewStatus) preview$;
  @Output() disabled = new EventEmitter<boolean>();
  formChanges = false;
  ctype: string;
  existing = List();
  preview = Map();
  dateObjs = [null , null];
  subscription: Subscription;
  dropdownOption = {
    selected: ['Any', 'Any', 'Any', 'Any']
  };
  visitCounts: any;

  readonly modifiers = [{
    name: 'ageAtEvent',
    label: 'Age At Event',
    inputType: 'number',
    min: 1,
    max: 120,
    maxLength: 3,
    modType: ModifierType.AGEATEVENT,
    operators: [{
      name: 'Any',
      value: undefined,
    }, {
      name: 'Greater Than or Equal To',
      value: 'GREATER_THAN_OR_EQUAL_TO',
    }, {
      name: 'Less Than or Equal To',
      value: 'LESS_THAN_OR_EQUAL_TO',
    }, {
      name: 'Between',
      value: 'BETWEEN',
    }],
  }, {
    name: 'eventDate',
    label: 'Shifted Event Date',
    inputType: 'date',
    min: null,
    max: null,
    maxLength: null,
    modType: ModifierType.EVENTDATE,
    operators: [{
      name: 'Any',
      value: undefined,
    }, {
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
    min: 1,
    max: 99,
    maxLength: 2,
    modType: ModifierType.NUMOFOCCURRENCES,
    operators: [{
      name: 'Any',
      value: undefined,
    }, {
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

  dateA = new FormControl();
  dateB = new FormControl();
  errors = new Set();
  constructor(
    private actions: CohortSearchActions,
    private api: CohortBuilderService,
    private cdref: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    this.subscription = this.modifiers$.subscribe(mods => this.existing = mods);
    this.subscription.add(this.ctype$
      .filter(ctype => !! ctype)
      .subscribe(ctype => {
        this.ctype = ctype;
        if (this.addEncounters) {
          this.modifiers.push({
            name: 'encounters',
            label: 'During Visit Type',
            inputType: null,
            min: null,
            max: null,
            maxLength: null,
            modType: ModifierType.ENCOUNTERS,
            operators: [{
              name: 'Any',
              value: undefined,
            }]
          });
          this.form.addControl('encounters',
            new FormGroup({operator: new FormControl(),
              encounterType: new FormControl()}));
          this.api.getCriteriaBy(cdrid, TreeType[TreeType.VISIT], null, 0)
            .filter(response => !!response)
            .subscribe(response => {
              this.visitCounts = {};
              response.items.forEach(option => {
                if (option.parentId === 0 && option.count > 0) {
                  this.modifiers[3].operators.push({
                    name: option.name,
                    value: option.conceptId.toString()
                  });
                  this.visitCounts[option.conceptId] = option.count;
                }
              });
            });
        }
      })
    );

    this.subscription.add(this.preview$.subscribe(prev => this.preview = prev));

    // This reseeds the form with existing data if we're editing an existing group
    this.subscription.add(this.modifiers$.first().subscribe(mods => {
      mods.forEach(mod => {
        const meta = this.modifiers.find(_mod => mod.get('name') === _mod.modType);
        if (meta) {
          if (meta.modType === ModifierType.ENCOUNTERS) {
            let selected;
            // make sure the encounters options are loaded before setting the value
            Observable.interval(100)
              .takeWhile((val, index) => !selected && index < 30)
              .subscribe(i => {
                selected = meta.operators.find(
                  operator => operator.value
                    && operator.value.toString() === mod.getIn(['operands', 0])
                );
                if (selected) {
                  this.dropdownOption.selected[3] = selected.name;
                  this.form.get(meta.name).patchValue({
                    operator: mod.getIn(['operands', 0]),
                    encounterType: mod.get('encounterType')
                  }, {emitEvent: false});
                }
              });
          } else {
            const selected = meta.operators.find(
              operator => operator.value === mod.get('operator')
            );
            const index = this.modifiers.indexOf(meta);
            this.dropdownOption.selected[index] = selected.name;
            this.ngAfterContentChecked();
            if (meta.modType === ModifierType.EVENTDATE) {
              this.dateObjs = [
                new Date(mod.getIn(['operands', 0]) + 'T08:00:00'),
                new Date(mod.getIn(['operands', 1]) + 'T08:00:00')
              ];
            }
            this.form.get(meta.name).patchValue({
              operator: mod.get('operator'),
              valueA: mod.getIn(['operands', 0]),
              valueB: mod.getIn(['operands', 1]),
            }, {emitEvent: false});
          }
        }
      });
    }));

    this.subscription.add(this.form.valueChanges
      .do(console.log)
      .map(vals => this.currentMods(vals))
      .subscribe(newMods => {
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

        // update the calculate button
        this.formChanges = !newMods.every(element => element === undefined);
        // clear preview/counts
        this.preview = Map();
      })
    );

    this.subscription.add(this.dateA.valueChanges.subscribe(value => {
      const formatted = moment(value).format('YYYY-MM-DD');
      this.form.get(['eventDate', 'valueA']).setValue(formatted);
    }));

    this.subscription.add(this.dateB.valueChanges.subscribe(value => {
      const formatted = moment(value).format('YYYY-MM-DD');
      this.form.get(['eventDate', 'valueB']).setValue(formatted);
    }));
  }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }

  showCount(modName: string, optName: string) {
    return modName === 'encounters' && optName !== 'Any';
  }

  selectChange(opt, index, e, mod) {
    this.dropdownOption.selected[index] = opt.name;
    const modForm = <FormArray>this.form.controls[mod.name];
    const valueForm = <FormArray>modForm;
    if (mod.name === 'encounters') {
      valueForm.get('encounterType').patchValue(opt.name);
    } else {
      if (opt.name === 'Any') {
        this.form.get([mod.name, 'valueA']).clearValidators();
        this.form.get([mod.name, 'valueB']).clearValidators();
        this.form.get(mod.name).reset();
      } else {
        const validators = [Validators.required];
        if (mod.modType === ModifierType.EVENTDATE) {
          validators.push(dateValidator());
        } else {
          validators.push(integerAndRangeValidator(mod.label, mod.min, mod.max));
          validators.push(Validators.maxLength(mod.maxLength));
        }
        this.form.get([mod.name, 'valueA']).setValidators(validators);
        if (opt.value === Operator.BETWEEN) {
          this.form.get([mod.name, 'valueB']).setValidators(validators);
        } else {
          this.form.get([mod.name, 'valueB']).clearValidators();
          this.form.get([mod.name, 'valueB']).reset();
        }
      }
    }
    valueForm.get('operator').patchValue(opt.value);
  }

  currentMods(vals) {
    this.ngAfterContentChecked();
    this.errors = new Set();
    return this.modifiers.map(mod => {
      const {name, inputType, maxLength, modType} = mod;
      if (modType === ModifierType.ENCOUNTERS) {
        if (!vals[name].operator) {
          return;
        }
        return fromJS({name: modType, operator: 'IN',
          encounterType: vals[name].encounterType.toString(),
          operands: [vals[name].operator.toString()]});
      } else {
        const {operator, valueA, valueB} = vals[name];
        const between = operator === 'BETWEEN';
        if (!operator || (!valueA && !valueB)) {
          if (inputType !== 'date'
            && (this.form.get([name, 'valueA']).dirty || this.form.get([name, 'valueB']).dirty)) {
            this.errors.add({name, type: 'integer'});
          }
          return;
        }
        if (inputType === 'date') {
          const dateValueA = moment(valueA).format('YYYY-MM-DD');
          const dateValueB = moment(valueB).format('YYYY-MM-DD');
          const operands = [dateValueA];
          if (between && dateValueB) {
            operands.push(dateValueB);
          }
          return fromJS({name: modType, operator, operands});
        } else {
          const operands = [valueA];
          if (between) {
            operands.push(valueB);
          }
          operands.forEach((value, i) => {
            const input = i === 0 ? 'valueA' : 'valueB';
            if (value && value.length > maxLength) {
              value = value.slice(0, maxLength);
              this.form.get([name, input]).setValue(value, {emitEvent: false});
            }
          });
          return fromJS({name: modType, operator, operands});
        }
      }
    });
  }

  get addEncounters() {
    return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(TreeType[this.ctype]) === -1
      && !this.modifiers.find(modifier => modifier.modType === ModifierType.ENCOUNTERS);
  }

  requestPreview() {
    this.actions.requestPreview();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  dateBlur(index: number) {
    const control = index === 0 ? 'valueA' : 'valueB';
    this.dateObjs[index] = new Date(this.form.get(['eventDate', control]).value + 'T08:00:00');
  }

  get disableCalculate() {
    const disable = !!this.preview.get('requesting') || !!this.errors.size || this.form.invalid;
    this.disabled.emit(disable);
    return disable;
  }
}
