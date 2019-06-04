import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {dateValidator, integerAndRangeValidator} from 'app/cohort-search/validators';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ModifierType, Operator, TreeType} from 'generated';
import {CriteriaType, DomainType} from 'generated/fetch';
import {List} from 'immutable';
import * as moment from 'moment';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

interface Props {
  disabled: Function;
  wizard: any;
  workspace: WorkspaceData;
}

interface State {
  formValues: any;
}

export const ListModifierPage  = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        formValues: {
          ageAtEvent: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
          hasOccurrences: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
          eventDate: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
        }
      };
    }
    formChanges = false;
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

    dateA = new FormControl();
    dateB = new FormControl();
    errors = new Set();

    componentDidMount() {
      const {workspace: {cdrVersionId}, wizard} = this.props;
      let {formValues} = this.state;
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
        formValues = {
          ...formValues,
          encounters: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          }
        };
        this.setState({formValues});
        cohortBuilderApi()
          .getCriteriaBy(
            +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
          )
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
          wizard.item.modifiers = newMods.filter(mod => !!mod);
          wizardStore.next(wizard);
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
      const {wizard} = this.props;
      const {formValues} = this.state;
      // This reseeds the form with existing data if we're editing an existing group
      wizard.item.modifiers.forEach(mod => {
        const meta = this.modifiers.find(_mod => mod.name === _mod.modType);
        if (meta) {
          if (meta.modType === ModifierType.ENCOUNTERS) {
            const selected = meta.operators.find(
              operator => operator.value
                && operator.value.toString() === mod.operands[0]
            );
            if (selected) {
              this.dropdownOption.selected[3] = selected.name;
              formValues[meta.name] = {
                operator: mod.operands[0],
                encounterType: mod.encounterType
              };
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
            formValues[meta.name] = {
              operator: mod.operator,
              valueA: mod.operands[0],
              valueB: mod.operands[1],
            };
          }
        }
      });
      this.setState({formValues});
    }

    showCount(modName: string, optName: string) {
      return modName === 'encounters' && optName !== 'Any';
    }

    selectChange(opt, index, e, mod) {
      const {formValues} = this.state;
      this.dropdownOption.selected[index] = opt.name;
      if (mod.name === 'encounters') {
        formValues.encounters.encounterType = opt.name;
      } else if (opt.name === 'Any') {
        formValues[mod.name] = {
          operator: undefined,
          valueA: undefined,
          valueB: undefined,
        };
      }
      formValues[mod.name].operator = opt.value;
      this.setState({formValues});
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
      const {wizard: {domain}} = this.props;
      return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(TreeType[domain]) === -1
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
      this.props.disabled(disable);
      return disable;
    }
  }
);

@Component({
  selector: 'crit-list-modifier-page',
  template: '<div #root></div>'
})
export class ListModifierPageComponent extends ReactWrapperBase {
  @Input('disabled') disabled: Props['disabled'];
  @Input('wizard') wizard: Props['wizard'];
  constructor() {
    super(ListModifierPage, ['disabled', 'wizard']);
  }
}
