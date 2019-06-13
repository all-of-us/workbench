import {Component, Input} from '@angular/core';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {DatePicker} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {TreeType} from 'generated';
import {CriteriaType, DomainType, ModifierType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

const styles = reactStyles({
  header: {
    color: '#262262',
    fontWeight: 500,
    fontSize: '16px',
    borderBottom: '1px solid #262262',
    paddingBottom: '0.5rem'
  },
  errors: {
    background: '#f5dbd9',
    color: '#565656',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginTop: '0.25rem',
    padding: '3px 5px'
  },
  errorItem: {
    lineHeight: '16px',
  },
  label: {
    color: '#262262',
    fontWeight: 500,
  },
  modifier: {
    marginTop: '0.4rem'
  },
  select: {
    width: '12rem',
    height: '1.6rem',
    paddingLeft: '0.5rem',
    marginRight: '1rem',
  },
  number: {
    borderRadius: '3px',
    border: '1px solid #a6a6a6',
    width: '3rem',
    height: '1.6rem',
    verticalAlign: 'middle',
  },
  date: {
    width: '6.5rem',
    display: 'inline-block',
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
  },
  footer: {
    background: '#e4f3fc',
    padding: '0.5rem',
    position: 'absolute',
    width: '93%',
    bottom: '1rem'
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem'
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  button: {
    color: '#ffffff',
    margin: '0.25rem 0.5rem 0.25rem 0',
    borderRadius: '3px',
  },
  previewCount: {
    color: '#4a4a4a',
    fontWeight: 'bold'
  }
});

const button = {
  active: {
    ...styles.button,
    background: '#2691d0',
    border: '1px solid #0077b7',
  },
  disabled: {
    ...styles.button,
    background: '#c3c3c3',
    border: 'transparent',
    opacity: 0.3,
  }
}

const columns = {
  col3: {
    ...styles.col,
    flex: '0 0 25%',
    maxWidth: '25%'
  },
  col8: {
    ...styles.col,
    flex: '0 0 67.66667%',
    maxWidth: '67.66667%'
  }
};

const validatorFuncs = {
  AGE_AT_EVENT: (value) => {
    if (value === '') {
      return 'Age At Event is required';
    }
    if (value < 0 || value > 120) {
      return 'Age At Event must be between 0 - 120';
    }
    if (!Number.isInteger(parseFloat(value))) {
      return 'Age At Event must be a whole number';
    }
    return null;
  },
  EVENT_DATE: (value) => {
    if (value === '') {
      return 'Event Date is required';
    }
    if (!moment(value, 'YYYY-MM-DD', true).isValid()) {
      return 'Dates must be in format \'YYYY-MM-DD\'';
    }
    return null;
  },
  NUM_OF_OCCURRENCES: (value) => {
    if (value === '') {
      return 'Has Occurrences is required';
    }
    if (value < 1 || value > 99) {
      return 'Has Occurrences must be between 1 - 99';
    }
    if (!Number.isInteger(parseFloat(value))) {
      return 'Has Occurrences must be a whole number';
    }
    return null;
  }
}

interface Props {
  disabled: Function;
  wizard: any;
  workspace: WorkspaceData;
}

interface State {
  formState: any;
  preview: any;
  visitCounts: any;
}

export const ListModifierPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    updateInput: Function;
    constructor(props: Props) {
      super(props);
      this.state = {
        formState: [{
          name: 'AGE_AT_EVENT',
          label: 'Age At Event',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
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
          name: 'NUM_OF_OCCURRENCES',
          label: 'Has Occurrences',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'N or More',
            value: 'GREATER_THAN_OR_EQUAL_TO',
          }]
        }, {
          name: 'EVENT_DATE',
          label: 'Shifted Event Date',
          type: 'date',
          operator: undefined,
          values: [undefined, undefined],
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
        preview: {
          loading: false,
          count: 10,
        },
        visitCounts: undefined
      };
      this.updateInput = fp.debounce(300, () => this.updateMods());
    }

    componentDidMount() {
      const {workspace: {cdrVersionId}, wizard} = this.props;
      const {formState} = this.state;
      if (this.addEncounters) {
        cohortBuilderApi()
        .getCriteriaBy(
            +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
        )
        .then(response => {
          const visitCounts = {};
          const encounters = {
            name: 'ENCOUNTERS',
            label: 'During Visit Type',
            type: null,
            operator: undefined,
            values: [undefined],
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
    }

    getExisting() {
      const {wizard} = this.props;
      const {formState} = this.state;
      // This reseeds the form with existing data if we're editing an existing group
      wizard.item.modifiers.forEach(existing => {
        const index = formState.findIndex(mod => existing.name === mod.name);
        if (index) {
          const mod = formState[index];
          const values = existing.operands.filter(val => !!val);
          switch (mod.name) {
            case ModifierType.ENCOUNTERS:
              formState[index] = {...mod, operator: existing.operands[0], values};
              break;
            case ModifierType.EVENTDATE:
              formState[index] = {
                ...mod,
                operator: existing.operands[0],
                values: values.map(val => new Date(val + 'T08:00:00'))
              };
              break;
            default:
              formState[index] = {...mod, operator: existing.operator, values};
          }
        }
      });
      this.setState({formState});
    }

    selectChange = (sel, index) => {
      const {formState} = this.state;
      const {name} = formState[index];
      if (name === 'ENCOUNTERS') {
        formState[index].values = [sel];
      } else if (!sel) {
        formState[index].values = [undefined, undefined];
      } else if (sel !== 'BETWEEN') {
        formState[index].values[1] = undefined;
      }
      formState[index].operator = sel;
      this.setState({formState});
      this.updateMods();

    }

    inputChange = (index: string, field: string, value: any) => {
      const {formState} = this.state;
      formState[index].values[field] = value;
      this.setState({formState});
      this.updateInput();
    }

    updateMods() {
      const {wizard} = this.props;
      const {formState} = this.state;
      wizard.item.modifiers = formState.reduce((acc, mod) => {
        const {name, operator, values} = mod;
        if (operator) {
          switch (name) {
            case ModifierType.ENCOUNTERS:
              acc.push({name, operator: 'IN', operands: [operator.toString()]});
              break;
            case ModifierType.EVENTDATE:
              acc.push({name, operator, operands: values.map(val => {
                return moment(val, 'YYYY-MM-DD', true).isValid()
                  ? moment(val).format('YYYY-MM-DD') : undefined;
              })});
              break;
            default:
              acc.push({name, operator, operands: values});
          }
        }
        return acc;
      }, []);
      wizardStore.next(wizard);
    }

    get addEncounters() {
      const {wizard: {domain}} = this.props;
      return [TreeType[TreeType.PM], TreeType[TreeType.VISIT]].indexOf(TreeType[domain]) === -1;
    }

    requestPreview() {
      // TODO calculate count when new api call is ready
    }
    // get disableCalculate() {
    //   const disable = !!this.preview.requesting || !!this.errors.size || this.form.invalid;
    //   this.props.disabled(disable);
    //   return disable;
    // }

    optionTemplate = (opt: any, name: string) => {
      if (name !== 'ENCOUNTERS' || !opt.value) {
        return opt.label;
      }
      const {visitCounts} = this.state;
      return <div className='p-clearfix'>
        {opt.label}
        &nbsp;<span style={styles.count}>{visitCounts[opt.value]}</span>
      </div>;
    }

    renderInput(index: string, field: string, type) {
      const {values} = this.state.formState[index];
      switch (type) {
        case 'number':
          return <input type='number' style={styles.number} value={values[field]}
            onChange={e => this.inputChange(index, field, e.target.value)}/>;
        case 'date':
          return <div style={styles.date}>
            <DatePicker
              value={values[field]}
              placeholder='YYYY-MM-DD'
              onChange={e => this.inputChange(index, field, e)}
              maxDate={new Date()}
            />
          </div>;
      }
    }

    render() {
      const {formState, preview} = this.state;
      const tooltip = `Dates are consistently shifted within a participant’s record by a time period
        of up to 364 days backwards. The date shift differs across participants.`;
      let initialState = true;
      let untouched = false;
      const errors = formState.reduce((acc, item) => {
        item.values.forEach((val, v) => {
          if (val !== undefined) {
            initialState = false;
            const error = validatorFuncs[item.name](val);
            if (error) {
              acc.add(error);
            }
          } else if (item.operator !== undefined) {
            initialState = false;
            if (v === 0 || (v === 1 && item.operator === 'BETWEEN')) {
              untouched = true;
            }
          }
        });
        return acc;
      }, new Set());
      const disabled = !!errors.size || initialState || untouched;
      this.props.disabled(disabled);
      return <div style={{marginTop: '1rem'}}>
        <div style={styles.header}>
          The following modifiers are optional and apply to all selected criteria
        </div>
        {!!errors.size && <div style={styles.errors}>
          {Array.from(errors).map((error, e) => <div key={e} style={styles.errorItem}>
            {error}
          </div>)}
        </div>}
        {formState.map((mod, i) => {
          const {label, name, options, operator} = mod;
          return <div key={i} style={{marginTop: '0.75rem'}}>
            <label style={styles.label}>{label}</label>
            {name === 'EVENT_DATE' &&
              <TooltipTrigger content={<div>{tooltip}</div>}>
                <ClrIcon style={styles.info} className='is-solid' shape='info-standard'/>
              </TooltipTrigger>
            }
            <div style={styles.modifier}>
              <Dropdown value={operator}
                style={styles.select}
                onChange={(e) => this.selectChange(e.value, i)}
                options={options}
                itemTemplate={(e) => this.optionTemplate(e, name)}/>
              {operator && name !== 'ENCOUNTERS' && <React.Fragment>
                {this.renderInput(i, '0', mod.type)}
                {operator === 'BETWEEN' && <React.Fragment>
                  <span style={{margin: '0 0.25rem'}}>and</span>
                  {this.renderInput(i, '1', mod.type)}
                </React.Fragment>}
              </React.Fragment>}
            </div>
          </div>;
        })}
        <div style={styles.footer}>
          <div style={styles.row}>
            <div style={columns.col3}>
              <Button type='primary' style={disabled ? button.disabled : button.active}
                disabled={disabled}>Calculate</Button>
            </div>
            {!preview.loading && preview.count && <div style={columns.col8}>
              <div style={{color: '#262262'}}>Results</div>
              {!preview.loading && <div>
                Number Participants: <span style={styles.previewCount}>{preview.count}</span>
              </div>}
            </div>}
          </div>
        </div>
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
