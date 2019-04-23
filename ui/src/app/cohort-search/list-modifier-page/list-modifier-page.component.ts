import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {dateValidator, integerAndRangeValidator} from 'app/cohort-search/validators';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {ModifierType, Operator, TreeType} from 'generated';
import {List} from 'immutable';
import * as moment from 'moment';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'crit-list-modifier-page',
  templateUrl: './list-modifier-page.component.html',
  styleUrls: ['./list-modifier-page.component.css']
})
export class ListModifierPageComponent implements OnInit, OnDestroy {
  @Input() disabled: Function;
  @Input() wizard: any;
  formChanges = false;
  domain: string;
  existing = List();
  preview: any = {count: -1};
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

  ngOnInit() {
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    this.domain = this.wizard.domain;
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
      cohortBuilderApi().getCriteriaBy(cdrid, TreeType[TreeType.VISIT], null, 0)
        .then(response => {
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
          this.getExisting();
        });
    } else {
      this.getExisting();
    }

    this.subscription = this.form.valueChanges
      .map(this.currentMods)
      .subscribe(newMods => {
        this.wizard.item.modifiers = newMods.filter(mod => !!mod);
        wizardStore.next(this.wizard);
      });

    this.subscription.add(this.dateA.valueChanges.subscribe(value => {
      const formatted = moment(value).format('YYYY-MM-DD');
      this.form.get(['eventDate', 'valueA']).setValue(formatted);
    }));

    this.subscription.add(this.dateB.valueChanges.subscribe(value => {
      const formatted = moment(value).format('YYYY-MM-DD');
      this.form.get(['eventDate', 'valueB']).setValue(formatted);
    }));
  }

  getExisting() {
    // This reseeds the form with existing data if we're editing an existing group
    this.wizard.item.modifiers.forEach(mod => {
      const meta = this.modifiers.find(_mod => mod.name === _mod.modType);
      if (meta) {
        if (meta.modType === ModifierType.ENCOUNTERS) {
          const selected = meta.operators.find(
            operator => operator.value
              && operator.value.toString() === mod.operands[0]
          );
          if (selected) {
            this.dropdownOption.selected[3] = selected.name;
            this.form.get(meta.name).patchValue({
              operator: mod.operands[0],
              encounterType: mod.encounterType
            }, {emitEvent: false});
          }
        } else {
          const selected = meta.operators.find(
            operator => operator.value === mod.operator
          );
          const index = this.modifiers.indexOf(meta);
          this.dropdownOption.selected[index] = selected.name;
          if (meta.modType === ModifierType.EVENTDATE) {
            this.dateObjs = [
              new Date(mod.operands[0] + 'T08:00:00'),
              new Date(mod.operands[1] + 'T08:00:00')
            ];
          }
          this.form.get(meta.name).patchValue({
            operator: mod.operator,
            valueA: mod.operands[0],
            valueB: mod.operands[1],
          }, {emitEvent: false});
        }
      }
    });
  }

  showCount(modName: string, optName: string) {
    return modName === 'encounters' && optName !== 'Any';
  }

  selectChange(opt, index, e, mod) {
    this.dropdownOption.selected[index] = opt.name;
    const modForm = <FormArray>this.form.controls[mod.name];
    const valueForm = <FormArray>modForm;
    if (mod.name === 'encounters') {
      valueForm.get('encounterType').patchValue(opt.name, {emitEvent: false});
    } else {
      if (opt.name === 'Any') {
        this.form.get([mod.name, 'valueA']).clearValidators();
        this.form.get([mod.name, 'valueB']).clearValidators();
        this.form.get(mod.name).reset({}, {emitEvent: false});
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
          this.form.get([mod.name, 'valueB']).reset(null, {emitEvent: false});
        }
      }
    }
    valueForm.get('operator').patchValue(opt.value);
  }

  currentMods = (vals) => {
    this.errors = new Set();
    return this.modifiers.map(mod => {
      const {name, inputType, maxLength, modType} = mod;
      if (modType === ModifierType.ENCOUNTERS) {
        if (!vals[name].operator) {
          return;
        }
        return {name: modType, operator: 'IN',
          encounterType: vals[name].encounterType.toString(),
          operands: [vals[name].operator.toString()]};
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
          return {name: modType, operator, operands};
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
          return {name: modType, operator, operands};
        }
      }
    });
  }

  get addEncounters() {
    return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(TreeType[this.domain]) === -1
      && !this.modifiers.find(modifier => modifier.modType === ModifierType.ENCOUNTERS);
  }

  requestPreview() {
    // TODO calculate count when new api call is ready
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  dateBlur(index: number) {
    const control = index === 0 ? 'valueA' : 'valueB';
    this.dateObjs[index] = new Date(this.form.get(['eventDate', control]).value + 'T08:00:00');
  }

  get disableCalculate() {
    const disable = !!this.preview.requesting || !!this.errors.size || this.form.invalid;
    this.disabled(disable);
    return disable;
  }
}
