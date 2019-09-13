import {Component, Input} from '@angular/core';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, mapParameter} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {DatePicker} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CriteriaType, DomainType, ModifierType, Operator} from 'generated/fetch';
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
    height: '1.5rem',
    margin: '0.25rem 0.5rem 0.25rem 0',
    borderRadius: '3px',
  },
  previewCount: {
    color: '#4a4a4a',
    fontWeight: 'bold'
  },
  error: {
    background: '#F7981C',
    color: '#ffffff',
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
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
};

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
};

interface Props {
  disabled: Function;
  wizard: any;
  workspace: WorkspaceData;
}

interface State {
  formState: any;
  count: number;
  visitCounts: any;
  error: boolean;
  loading: boolean;
}

export const ListModifierPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    updateInput: Function;
    constructor(props: Props) {
      super(props);
      this.state = {
        formState: [{
          name: ModifierType.AGEATEVENT,
          label: 'Age At Event',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Greater Than or Equal To',
            value: Operator.GREATERTHANOREQUALTO,
          }, {
            label: 'Less Than or Equal To',
            value: Operator.LESSTHANOREQUALTO,
          }, {
            label: 'Between',
            value: Operator.BETWEEN,
          }]
        }, {
          name: ModifierType.NUMOFOCCURRENCES,
          label: 'Has Occurrences',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'N or More',
            value: Operator.GREATERTHANOREQUALTO,
          }]
        }, {
          name: ModifierType.EVENTDATE,
          label: 'Shifted Event Date',
          type: 'date',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Is On or Before',
            value: Operator.LESSTHANOREQUALTO,
          }, {
            label: 'Is On or After',
            value: Operator.GREATERTHANOREQUALTO,
          }, {
            label: 'Is Between',
            value: Operator.BETWEEN,
          }]
        }],
        loading: false,
        count: null,
        visitCounts: undefined,
        error: false,
      };
      this.updateInput = fp.debounce(300, () => this.updateMods());
    }

    componentDidMount() {
      const {workspace: {cdrVersionId}} = this.props;
      const {formState} = this.state;
      if (this.addEncounters) {
        // get options for visit modifier from api
        cohortBuilderApi()
        .getCriteriaBy(
            +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
        )
        .then(response => {
          const visitCounts = {};
          const encounters = {
            name: ModifierType.ENCOUNTERS,
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
      // This reseeds the form state with existing data if we're editing an existing item
      wizard.item.modifiers.forEach(existing => {
        const index = formState.findIndex(mod => existing.name === mod.name);
        if (index > -1) {
          const mod = formState[index];
          const values = existing.operands.filter(val => !!val);
          switch (mod.name) {
            case ModifierType.ENCOUNTERS:
              formState[index] = {...mod, operator: existing.operands[0], values};
              break;
            case ModifierType.EVENTDATE:
              formState[index] = {
                ...mod,
                operator: existing.operator,
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

    selectChange = (sel: any, index: number) => {
      const {formState} = this.state;
      this.trackEvent(formState[index].label);
      const {name} = formState[index];
      if (name === ModifierType.ENCOUNTERS) {
        formState[index].values = [sel];
      } else if (!sel) {
        formState[index].values = [undefined, undefined];
      } else if (sel !== Operator.BETWEEN) {
        formState[index].values[1] = undefined;
      }
      formState[index].operator = sel;
      this.setState({count: null, formState});
      this.updateMods();

    }

    inputChange = (index: string, field: string, value: any) => {
      const {formState} = this.state;
      formState[index].values[field] = value;
      this.setState({count: null, formState});
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
              acc.push({name, operator: Operator.IN, operands: [operator.toString()]});
              break;
            case ModifierType.EVENTDATE:
              const formatted = values.map(val => moment(val, 'YYYY-MM-DD', true).isValid()
                    ? moment(val).format('YYYY-MM-DD') : undefined);
              acc.push({name, operator, operands: formatted.filter(val => !!val)});
              break;
            default:
              acc.push({name, operator, operands: values.filter(val => !!val)});
          }
        }
        return acc;
      }, []);
      wizardStore.next(wizard);
    }

    get addEncounters() {
      const {wizard: {domain}} = this.props;
      return ![DomainType.PHYSICALMEASUREMENT, DomainType.VISIT].includes(domain);
    }

    calculate = () => {
      const {
        wizard: {domain, item: {modifiers, searchParameters}, role},
        workspace: {cdrVersionId}
      } = this.props;
      this.trackEvent('Calculate');
      try {
        this.setState({loading: true, count: null, error: false});
        const request = {
          includes: [],
          excludes: [],
          [role]: [{
            items: [{
              type: domain,
              searchParameters: searchParameters.map(mapParameter),
              modifiers: modifiers
            }]
          }]
        };
        cohortBuilderApi().countParticipants(+cdrVersionId, request).then(response => {
          this.setState({count: response, loading: false});
        }, () => this.setState({loading: false, error: true}));
      } catch (error) {
        console.error(error);
        // TODO this is not catching errors. Need to try again with the new api call
        this.setState({loading: false, error: true});
      }
    }

    trackEvent = (label: string) => {
      const {wizard: {domain}} = this.props;
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `Modifiers - ${label} - ${domainToTitle(domain)} - Cohort Builder Search`
      );
    }

    validateValues() {
      const {formState} = this.state;
      let initialState = true;
      let untouched = false;
      const errors = formState.reduce((acc, item) => {
        if (item.name !== ModifierType.ENCOUNTERS) {
          item.values.forEach((val, v) => {
            if (val !== undefined) {
              initialState = false;
              const error = validatorFuncs[item.name](val);
              if (error) {
                acc.add(error);
              }
            } else if (item.operator !== undefined) {
              initialState = false;
              if (v === 0 || (v === 1 && item.operator === Operator.BETWEEN)) {
                untouched = true;
              }
            }
          });
        } else if (item.values[0] !== undefined) {
          initialState = false;
        }
        return acc;
      }, new Set());
      return {errors, initialState, untouched};
    }

    optionTemplate = (opt: any, name: any) => {
      if (name !== ModifierType.ENCOUNTERS || !opt.value) {
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
      const {count, error, formState, loading} = this.state;
      const tooltip = `Dates are consistently shifted within a participant’s record by a time period
        of up to 364 days backwards. The date shift differs across participants.`;
      const {errors, initialState, untouched} = this.validateValues();
      const disableFinish = !!errors.size || untouched || loading;
      this.props.disabled(disableFinish);
      const disabled = disableFinish || initialState;
      return <div style={{marginTop: '1rem'}}>
        <div style={styles.header}>
          The following modifiers are optional and apply to all selected criteria
        </div>
        {error && <div style={styles.error}>
          <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
            shape='exclamation-triangle' size='22'/>
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        {!!errors.size && <div style={styles.errors}>
          {Array.from(errors).map((err, e) => <div key={e} style={styles.errorItem}>
            {err}
          </div>)}
        </div>}
        {formState.map((mod, i) => {
          const {label, name, options, operator} = mod;
          return <div key={i} style={{marginTop: '0.75rem'}}>
            <label style={styles.label}>{label}</label>
            {name === ModifierType.EVENTDATE &&
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
              {operator && name !== ModifierType.ENCOUNTERS && <React.Fragment>
                {this.renderInput(i, '0', mod.type)}
                {operator === Operator.BETWEEN && <React.Fragment>
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
                disabled={disabled} onClick={() => this.calculate()}>
                {loading &&
                  <Spinner size={16} style={{marginRight: '0.25rem', marginLeft: '-0.25rem'}}/>
                }
                Calculate
              </Button>
            </div>
            {!loading && count !== null && <div style={columns.col8}>
              <div style={{color: '#262262'}}>Results</div>
              {!loading && <div>
                Number Participants: <span style={styles.previewCount}>{count}</span>
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
export class ModifierPageComponent extends ReactWrapperBase {
  @Input('disabled') disabled: Props['disabled'];
  @Input('wizard') wizard: Props['wizard'];

  constructor() {
    super(ListModifierPage, ['disabled', 'wizard']);
  }
}
