import {select} from '@angular-redux/store';
import {
  AfterContentChecked,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import {FormArray, FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService, ModifierType, TreeType} from 'generated';
import {fromJS, List, Map} from 'immutable';
import * as moment from 'moment';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {
  activeCriteriaType,
  activeModifierList,
  CohortSearchActions,
  previewStatus,
} from '../redux';

@Component({
    selector: 'crit-modifier-page',
    templateUrl: './modifier-page.component.html',
    styleUrls: ['./modifier-page.component.css']
})
export class ModifierPageComponent implements OnInit, OnDestroy, AfterContentChecked {
  @select(activeCriteriaType) ctype$;
  @select(activeModifierList) modifiers$;
  @select(previewStatus) preview$;
  formChanges = false;
  ctype: string;
  existing = List();
  preview = Map();
  dateObjs = [new Date(), new Date()];
  subscription: Subscription;
  dropdownOption = {
    selected: ['Any', 'Any', 'Any', 'Any']
  };
  visitCounts: any;

  readonly modifiers = [{
    name: 'ageAtEvent',
    label: 'Age At Event',
    inputType: 'number',
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
    }]
  }, {
    name: 'eventDate',
    label: 'Shifted Event Date',
    inputType: 'date',
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
  showError = false;
  constructor(
    private actions: CohortSearchActions,
    private api: CohortBuilderService,
    private cdref: ChangeDetectorRef,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    this.subscription = this.modifiers$.subscribe(mods => this.existing = mods);
    this.subscription.add(this.ctype$.subscribe(ctype => {
      this.ctype = ctype;
      if (this.addEncounters) {
        this.modifiers.push({
          name: 'encounters',
          label: 'During Visit Type',
          inputType: null,
          modType: ModifierType.ENCOUNTERS,
          operators: [{
            name: 'Any',
            value: undefined,
          }]
        });
        this.form.addControl('encounters', new FormGroup({operator: new FormControl()}));
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
    }));

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
              .takeWhile((val, index) => !selected && index < 10)
              .subscribe(i => {
                selected = meta.operators.find(
                  operator => operator.value
                    && operator.value.toString() === mod.getIn(['operands', 0])
                );
                if (selected) {
                  this.dropdownOption.selected[3] = selected.name;
                  this.form.get(meta.name).patchValue({
                    operator: mod.getIn(['operands', 0]),
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
    valueForm.get('operator').patchValue(opt.value);
  }

  currentMods(vals) {
    this.ngAfterContentChecked();
    return this.modifiers.map(({name, inputType, modType}) => {
      if (modType === ModifierType.ENCOUNTERS) {
        if (!vals[name].operator) {
          return;
        }
        return fromJS({name: modType, operator: 'IN', operands: [vals[name].operator.toString()]});
      } else {
        const {operator, valueA, valueB} = vals[name];
        const between = operator === 'BETWEEN';
        if (!operator || !valueA || (between && !valueB)) {
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
          return fromJS({name: modType, operator, operands});
        }
      }
    });
  }

  get addEncounters() {
    return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(this.ctype) === -1
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

  numberValidation(event) {
    if (!((event.keyCode > 95 && event.keyCode < 106)
      || (event.keyCode > 47 && event.keyCode < 58)
      || event.keyCode === 8)) {
      return false;
    }
  }

  negativeNumber() {
   this.modifiers$.forEach(item => {
    const modArr = item.map(modValue => {
       return modValue.toJS().operands.map( o => {
          return o < 0;
        });
      });
     this.showError = modArr.toJS().flat().includes(true);
    });
  }
}
