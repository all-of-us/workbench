import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ModifierType, TreeType} from 'generated';
import {CriteriaType, DomainType} from 'generated/fetch';
import {List} from 'immutable';
import * as moment from 'moment';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

const styles = reactStyles({
  header: {
    color: '#262262',
    fontWeight: 500,
    fontSize: '16px',
    borderBottom: '1px solid #262262',
    paddingBottom: '0.5rem'
  },
  label: {
    color: '#262262',
    fontWeight: 500,
  },
  modifier: {
    marginTop: '0.5rem'
  },
  select: {
    width: '12rem',
    height: '1.5rem',
    paddingLeft: '0.5rem',
    marginRight: '1rem',
  },
  input: {
    borderRadius: '3px',
    border: '1px solid #a6a6a6',
    width: '3rem',
    height: '1.5rem',
  },
  count: {
    display: 'inline-flex',
    height: '0.625rem',
    background: '#0079b8',
    color: '#ffffff',
    fontSize: '10px',
    padding: '0 0.166667rem',
    borderRadius: '10px',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  info: {
    color: '#0077b7',
    marginLeft: '0.25rem',
  }
});

interface Props {
  disabled: Function;
  wizard: any;
  workspace: WorkspaceData;
}

interface State {
  formState: any;
  visitCounts: any;
}

export const ListModifierPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        formState: [{
          name: 'ageAtEvent',
          label: 'Age At Event',
          values: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Greater Than or Equal To',
            value: 'GREATER_THAN_OR_EQUAL_TO',
          }, {
            label: 'Less Than or Equal To',
            value: 'LESS_THAN_OR_EQUAL_TO',
          }, {
            label: 'Between',
            value: 'BETWEEN',
          }]
        }, {
          name: 'hasOccurrences',
          label: 'Has Occurrences',
          values: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'N or More',
            value: 'GREATER_THAN_OR_EQUAL_TO',
          }]
        }, {
          name: 'eventDate',
          label: 'Shifted Event Date',
          values: {
            operator: undefined,
            valueA: undefined,
            valueB: undefined,
          },
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Is On or Before',
            value: 'LESS_THAN_OR_EQUAL_TO',
          }, {
            label: 'Is On or After',
            value: 'GREATER_THAN_OR_EQUAL_TO',
          }, {
            label: 'Is Between',
            value: 'BETWEEN',
          }]
        }],
        visitCounts: undefined
      };
    }

    formChanges = false;
    existing = List();
    preview: any = {count: -1};
    dateObjs = [null, null];
    subscription: Subscription;
    dropdownOption = {
      selected: ['Any', 'Any', 'Any', 'Any']
    };

    readonly modifiers = [{
      name: 'ageAtEvent',
      label: 'Age At Event',
      inputType: 'number',
      min: 1,
      max: 120,
      maxLength: 3,
      modType: ModifierType.AGEATEVENT,
      operators: [{
        label: 'Any',
        value: undefined,
      }, {
        label: 'Greater Than or Equal To',
        value: 'GREATER_THAN_OR_EQUAL_TO',
      }, {
        label: 'Less Than or Equal To',
        value: 'LESS_THAN_OR_EQUAL_TO',
      }, {
        label: 'Between',
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
        label: 'Any',
        value: undefined,
      }, {
        label: 'Is On or Before',
        value: 'LESS_THAN_OR_EQUAL_TO',
      }, {
        label: 'Is On or After',
        value: 'GREATER_THAN_OR_EQUAL_TO',
      }, {
        label: 'Is Between',
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
        label: 'Any',
        value: undefined,
      }, {
        label: 'N or More',
        value: 'GREATER_THAN_OR_EQUAL_TO',
      }]
    }];

    dateA = new FormControl();
    dateB = new FormControl();
    errors = new Set();

    componentDidMount() {
      const {workspace: {cdrVersionId}, wizard} = this.props;
      const {formState} = this.state;
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
            label: 'Any',
            value: undefined,
          }]
        });
        cohortBuilderApi()
        .getCriteriaBy(
            +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
        )
        .then(response => {
          const visitCounts = {};
          const encounters = {
            name: 'encounters',
            label: 'During Visit Type',
            values: {
              operator: undefined,
              encounterType: undefined,
            },
            options: [{
              label: 'Any',
              value: undefined,
            }]
          };
          response.items.forEach(option => {
            if (option.parentId === 0 && option.count > 0) {
              encounters.options.push({
                label: option.name,
                value: option.conceptId.toString()
              });
              visitCounts[option.conceptId] = option.count;
            }
          });
          formState.push(encounters);
          this.setState({formState, visitCounts});
          this.getExisting();
        });
      } else {
        this.getExisting();
      }

      // this.subscription = this.form.valueChanges
      // .map(this.currentMods)
      // .subscribe(newMods => {
      //   wizard.item.modifiers = newMods.filter(mod => !!mod);
      //   wizardStore.next(wizard);
      // });
      //
      // this.subscription.add(this.dateA.valueChanges.subscribe(value => {
      //   const formatted = moment(value).format('YYYY-MM-DD');
      //   this.form.get(['eventDate', 'valueA']).setValue(formatted);
      // }));
      //
      // this.subscription.add(this.dateB.valueChanges.subscribe(value => {
      //   const formatted = moment(value).format('YYYY-MM-DD');
      //   this.form.get(['eventDate', 'valueB']).setValue(formatted);
      // }));
    }

    getExisting() {
      const {wizard} = this.props;
      const {formState} = this.state;
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
              this.dropdownOption.selected[3] = selected.label;
              formState[meta.name] = {
                operator: mod.operands[0],
                encounterType: mod.encounterType
              };
            }
          } else {
            const selected = meta.operators.find(
              operator => operator.value === mod.operator
            );
            const index = this.modifiers.indexOf(meta);
            this.dropdownOption.selected[index] = selected.label;
            if (meta.modType === ModifierType.EVENTDATE) {
              this.dateObjs = [
                new Date(mod.operands[0] + 'T08:00:00'),
                new Date(mod.operands[1] + 'T08:00:00')
              ];
            }
            formState[meta.name] = {
              operator: mod.operator,
              valueA: mod.operands[0],
              valueB: mod.operands[1],
            };
          }
        }
      });
      this.setState({formState});
    }

    showCount(modName: string, optName: string) {
      return modName === 'encounters' && optName !== 'Any';
    }

    selectChange = (sel, index) => {
      const {formState} = this.state;
      const {name} = formState[index];
      if (name === 'encounters') {
        formState[index].values.encounterType = sel;
      } else if (!sel) {
        formState[index].values = {
          operator: undefined,
          valueA: undefined,
          valueB: undefined,
        };
      }
      formState[index].values.operator = sel;
      this.setState({formState});
    }

    inputChange = (index: string, field: string, value: any) => {
      const {formState} = this.state;
      formState[index].values[field] = value;
      this.setState({formState});
    }

    get addEncounters() {
      const {wizard: {domain}} = this.props;
      return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(TreeType[domain]) === -1
          && !this.modifiers.find(modifier => modifier.modType === ModifierType.ENCOUNTERS);
    }

    requestPreview() {
      // TODO calculate count when new api call is ready
    }

    // dateBlur(index: number) {
    //   const control = index === 0 ? 'valueA' : 'valueB';
    //   this.dateObjs[index] = new Date(this.form.get(['eventDate', control]).value + 'T08:00:00');
    // }
    //
    // get disableCalculate() {
    //   const disable = !!this.preview.requesting || !!this.errors.size || this.form.invalid;
    //   this.props.disabled(disable);
    //   return disable;
    // }

    optionTemplate = (opt: any, name: string) => {
      if (name !== 'encounters' || !opt.value) {
        return opt.label;
      }
      const {visitCounts} = this.state;
      return <div className='p-clearfix'>
        {opt.label}
        &nbsp;<span style={styles.count}>{visitCounts[opt.value]}</span>
      </div>;
    }

    render() {
      const {formState} = this.state;
      return <div style={{marginTop: '1rem'}}>
        <div style={styles.header}>
          The following modifiers are optional and apply to all selected criteria
        </div>
        {formState.map((mod, i) => {
          const {label, name, options, values: {operator, valueA, valueB}} = mod;
          return <div key={i} style={{marginTop: '1rem'}}>
            <label style={styles.label}>{label}</label>
            {name === 'eventDate' &&
              <ClrIcon style={styles.info} className='is-solid' shape='info-standard'/>
            }
            <div style={styles.modifier}>
              {/*TODO finish styling prime dropdown*/}
              <Dropdown value={operator}
                style={styles.select}
                panelStyle={{color: 'red'}}
                onChange={(e) => this.selectChange(e.value, i)}
                options={options}
                itemTemplate={(e) => this.optionTemplate(e, name)}/>
              {operator && name !== 'encounters' && <React.Fragment>
                <input type='number' style={styles.input} value={valueA}
                       onChange={(e) => this.inputChange(i, 'valueA', e.target.value)}/>
                {operator === 'Between' &&
                <input type='number' style={styles.input} value={valueB}
                       onChange={(e) => this.inputChange(i, 'valueB', e.target.value)}/>
                }
              </React.Fragment>}
            </div>
          </div>;
        })}
      </div>;
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
