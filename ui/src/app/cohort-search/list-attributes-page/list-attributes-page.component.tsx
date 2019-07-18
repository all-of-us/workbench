import {Component, Input} from '@angular/core';
import {AttrName, CriteriaSubType, DomainType, Operator} from 'generated/fetch';
import * as React from 'react';

import {PM_UNITS, PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {mapParameter, stripHtml} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {CheckBox} from 'app/components/inputs';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  countPreview: {
    backgroundColor: '#E4F3FC',
    padding: '0.5rem',
    marginTop: '1rem',
    position: 'absolute',
    width: '93%',
    bottom: '1rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap'
  },
  label: {
    color: '#262262',
    padding: '0.5rem',
    fontWeight: 500,
    display: 'flex',
  },
  orCircle: {
    backgroundColor: '#e2e2e9',
    borderRadius: '50%',
    width: '2.25rem',
    height: '2.25rem',
    margin: '0.75rem 3rem 0.25rem',
    lineHeight: '2.25rem',
    textAlign: 'center',
    fontSize: '0.45rem',
  },
  container: {
    display: 'flex',
    marginLeft: 'auto',
    marginRight: 'auto',
    padding: '0.2rem 0.5rem 0',
  },
  dropdown: {
    width: '12rem',
    marginRight: '1rem',
  },
  number: {
    borderRadius: '3px',
    border: '1px solid #a6a6a6',
    width: '3rem',
    height: '1.6rem',
    verticalAlign: 'middle',
  },
  badge: {
    background: colors.primary,
    color: colors.white,
    fontSize: '10px',
    height: '0.625rem',
    padding: '0 4px',
    borderRadius: '10px',
    display: 'inline-flex'
  },
  buttonContainer: {
    flex: '0 0 25%',
    maxWidth: '25%',
    padding: '0 0.5rem',
  },
  button: {
    height: '1.75rem',
    margin: '.25rem .5rem .25rem 0',
    borderRadius: '4px',
    fontWeight: 600
  },
  resultsContainer: {
    flex: '0 0 41.66667%',
    maxWidth: '41.66667%',
    padding: '0.3rem 0.5rem 0',
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '20px'
  }
});

const optionUtil = {
  ANY: {display: 'Any', code: 'Any'},
  EQUAL: {display: '= ', code: '01'},
  GREATER_THAN_OR_EQUAL_TO: {display: '>= ', code: '02'},
  LESS_THAN_OR_EQUAL_TO: {display: '<= ', code: '03'},
  BETWEEN: {display: '', code: '04'},
};

interface Props {
  criterion: any;
  close: Function;
  workspace: WorkspaceData;
}

interface State {
  form: any;
  options: any;
  count: number;
  loading: boolean;
  error: boolean;
}
export const AttributesPage = withCurrentWorkspace() (
  class extends React.Component<Props, State> {
    units = PM_UNITS;
    selectedCode: any;

    constructor(props: Props) {
      super(props);
      this.state = {
        form: {EXISTS: false, NUM: [], CAT: []},
        options: [
          {label: 'Equals', value: Operator.EQUAL},
          {label: 'Greater than or Equal to', value: Operator.GREATERTHANOREQUALTO},
          {label: 'Less than or Equal to', value: Operator.LESSTHANOREQUALTO},
          {label: 'Between', value: Operator.BETWEEN},
        ],
        count: null,
        loading: false,
        error: false,
      };
    }

    componentDidMount() {
      const {criterion} = this.props;
      const{form, options} = this.state;
      if (this.isMeasurement) {
        const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
        cohortBuilderApi().getCriteriaAttributeByConceptId(cdrid, criterion.conceptId)
          .then(resp => {
            resp.items.forEach(attr => {
              if (attr.type === AttrName[AttrName.NUM]) {
                if (!form.NUM.length) {
                  form.NUM.push({
                    name: AttrName.NUM,
                    operator: null,
                    operands: [],
                    conceptId: criterion.conceptId,
                    [attr.conceptName]: attr.estCount
                  });
                } else {
                  form.NUM[0][attr.conceptName] = attr.estCount;
                }
              } else {
                if (parseInt(attr.estCount, 10) > 0) {
                  attr['checked'] = false;
                  form.CAT.push(attr);
                }
              }
            });
            this.setState({form});
          });
      } else {
        options.unshift({label: 'Any', value: AttrName[AttrName.ANY]});
        form.NUM = criterion.subtype === CriteriaSubType[CriteriaSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{
            name: criterion.subtype,
            operator: AttrName.ANY,
            operands: [],
            MIN: 0,
            MAX: 10000
          }];
        this.setState({form, options, count: criterion.count});
      }
    }

    radioChange(checked: boolean) {
      const {form} = this.state;
      let {criterion: {count}} = this.props;
      if (checked) {
        form.EXISTS = true;
        form.NUM = form.NUM.map(attr =>
          ({...attr, operator: this.isPM ? AttrName.ANY : null, operands: []}));
        form.CAT = form.CAT.map(attr => ({...attr, checked: false}));
      } else {
        count = null;
        form.EXISTS = false;
      }
      this.setState({form, count});
    }

    selectChange(index: number, value: string) {
      const {criterion} = this.props;
      const {form} = this.state;
      form.NUM[index].operator = value;
      if (criterion.subtype === 'BP') {
        const other = index === 0 ? 1 : 0;
        if (value === AttrName[AttrName.ANY]) {
          form.NUM[other].operator = AttrName[AttrName.ANY];
        } else if (form.NUM[other].operator === AttrName[AttrName.ANY]) {
          form.NUM[other].operator = value;
        }
      } else {
        if (value !== Operator[Operator.BETWEEN]) {
          form.NUM[index].operands.splice(1);
        }
      }
      const count = value === AttrName[AttrName.ANY] ? criterion.count : null;
      this.setState({form, count});
    }

    // TODO remove or refactor with custom validation
    // setValidation(option: string) {
    //   if (option === 'Any') {
    //     this.form.controls.NUM.get(['num0', 'valueA']).clearValidators();
    //     this.form.controls.NUM.get(['num0', 'valueB']).clearValidators();
    //     if (this.attrs.NUM.length === 2) {
    //       this.form.controls.NUM.get(['num1', 'valueA']).clearValidators();
    //       this.form.controls.NUM.get(['num1', 'valueB']).clearValidators();
    //     }
    //     this.form.controls.NUM.reset();
    //   } else {
    //     const validators = [Validators.required];
    //     if (this.isMeasurement) {
    //       const min = parseFloat(this.attrs.NUM[0].MIN);
    //       const max = parseFloat(this.attrs.NUM[0].MAX);
    //       validators.push(rangeValidator('Values', min, max));
    //     } else {
    //       validators.push(numberAndNegativeValidator('Form'));
    //     }
    //     this.attrs.NUM.forEach((attr, i) => {
    //       this.form.controls.NUM.get(['num' + i, 'valueA']).setValidators(validators);
    //       if (option === 'Between') {
    //         this.form.controls.NUM.get(['num' + i, 'valueB']).setValidators(validators);
    //       } else {
    //         this.form.controls.NUM.get(['num' + i, 'valueB']).clearValidators();
    //         this.form.controls.NUM.get(['num' + i, 'valueB']).reset();
    //       }
    //     });
    //     this.cdref.detectChanges();
    //   }
    // }

    inputChange(input: string, index: number, operand: number) {
      const {form} = this.state;
      let value = input;
      if (value && value.length > 10) {
        value = value.slice(0, 10);
      }
      form.NUM[index].operands[operand] = value;
      this.setState({form, count: null});
    }

    // TODO refactor with custom validation
    // get isValid() {
    //   if (this.isPM || !this.form.valid) {
    //     return this.form.valid;
    //   }
    //   if (this.attrs.EXISTS) {
    //     return true;
    //   }
    //   let valid = false;
    //   this.attrs.NUM.forEach(num => {
    //     if (num.operator) {
    //       valid = true;
    //     }
    //   });
    //   this.attrs.CAT.forEach(cat => {
    //     if (cat.checked) {
    //       valid = true;
    //     }
    //   });
    //   return valid;
    // }

    get paramId() {
      const {criterion: {conceptId, id}} = this.props;
      return `param${(conceptId || id) + this.selectedCode}`;
    }

    get displayName() {
      const {criterion: {name}} = this.props;
      return stripHtml(name);
    }

    get paramWithAttributes() {
      const {criterion} = this.props;
      const {form} = this.state;
      let name;
      const attrs = [];
      if (form.EXISTS) {
        name = criterion.name + ' (Any)';
      } else {
        name = this.paramName;
        form.NUM.forEach((attr) => {
          if (criterion.subtype !== CriteriaSubType.BP) {
            delete(attr.conceptId);
          }
          if (attr.operator === AttrName.ANY && criterion.subtype === CriteriaSubType.BP) {
            attr.name = AttrName.ANY;
            attr.operands = [];
            delete(attr.operator);
            attrs.push(attr);
          } else if (attr.operator !== AttrName.ANY) {
            attrs.push(attr);
          }
        });

        const catOperands = form.CAT.reduce((checked, current) => {
          if (current.checked) {
            checked.push(current.valueAsConceptId);
          }
          return checked;
        }, []);
        if (catOperands.length) {
          attrs.push({name: AttrName.CAT, operator: Operator.IN, operands: catOperands});
        }
        name += (this.isPM && attrs[0] && attrs[0].name !== AttrName.ANY
          ? this.units[criterion.subtype]
          : '') + ')';
      }
      return {
        ...criterion,
        parameterId: this.paramId,
        name: name,
        attributes: attrs
      };
    }

    get paramName() {
      const {criterion} = this.props;
      const {form, options} = this.state;
      let name = criterion.name + ' (';
      form.NUM.forEach((attr, i) => {
        if (attr.operator === AttrName.ANY) {
          if (i === 0) {
            name += 'Any';
          }
        } else {
          if (i > 0) {
            name += ' / ';
          }
          if (criterion.subtype === CriteriaSubType.BP) {
            name += attr.name + ' ';
          }
          name += options.find(option => option.value === attr.operator).display
            + attr.operands.join('-');
        }
      });
      return name;
    }

    requestPreview() {
      this.setState({count: null, loading: true, error: false});
      const param = this.paramWithAttributes;
      const cdrVersionId = +(currentWorkspaceStore.getValue().cdrVersionId);
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: param.domainId,
            searchParameters: [mapParameter(param)],
            modifiers: []
          }],
          temporal: false
        }]
      };
      cohortBuilderApi().countParticipants(cdrVersionId, request).then(response => {
        this.setState({count: response, loading: false});
      }, () => {
        this.setState({loading: false, error: true});
      });
    }

    addAttrs() {
      const param = this.paramWithAttributes;
      const wizard = wizardStore.getValue();
      let selections = selectionsStore.getValue();
      if (!selections.includes(param.parameterId)) {
        selections = [param.parameterId, ...selections];
        selectionsStore.next(selections);
      } else {
        wizard.item.searchParameters = wizard.item.searchParameters
          .filter(p => p.parameterId !== param.parameterId);
      }
      wizard.item.searchParameters.push(param);
      wizardStore.next(wizard);
      this.props.close();
    }

    get hasUnits() {
      return typeof PM_UNITS[this.props.criterion.subtype] !== 'undefined';
    }

    get isMeasurement() {
      return this.props.criterion.domainId === DomainType[DomainType.MEASUREMENT];
    }

    get isPM() {
      return this.props.criterion.domainId === DomainType[DomainType.PHYSICALMEASUREMENT];
    }

    get isBP() {
      return this.props.criterion.subtype === CriteriaSubType[CriteriaSubType.BP];
    }

    get showCalc() {
      const {form} = this.state;
      let notAny = true;
      if (this.isPM) {
        notAny = form.NUM && form.NUM[0].operator !== AttrName.ANY;
      }
      return !form.EXISTS && notAny;
    }

    render() {
      const {criterion} = this.props;
      const {count, error, form, loading, options} = this.state;
      // TODO add validation check here
      const disabled = loading || form.EXISTS || form.NUM.every(attr => attr.operator === 'ANY');
      return <div style={{margin: '0.5rem 0 1.5rem', paddingTop: '0.5rem'}}>
        {this.isMeasurement && <div>
          <div style={styles.label}>{this.displayName}</div>
          <CheckBox style={{marginLeft: '0.5rem'}}
            onChange={(v) => this.radioChange(v)}/> Any value (lab exists)
          {!form.EXISTS && form.NUM.length > 0 && <div style={styles.orCircle}>OR</div>}
        </div>}
        {!form.EXISTS && <React.Fragment>
          {form.NUM.map((attr, a) => <div key={a}>
            {this.isMeasurement && <div style={styles.label}>Numeric Values</div>}
            {this.isBP && <div style={styles.label}>{attr.name}</div>}
            <div style={styles.container}>
              <div style={styles.dropdown}>
                <Dropdown style={{width: '100%'}}value={attr.operator} options={options}
                  placeholder='Select Operator' onChange={(e) => this.selectChange(a, e.value)}/>
              </div>
              {![null, 'ANY'].includes(attr.operator) && <div>
                <input style={styles.number} type='number' min={attr.MIN} max={attr.MAX}
                  onChange={(e) => this.inputChange(e.target.value, a, 0)}/>
                {this.hasUnits && <span> {PM_UNITS[criterion.subtype]}</span>}
              </div>}
              {attr.operator === Operator.BETWEEN && <React.Fragment>
                <div style={{padding: '0.2rem 1.5rem 0 1rem'}}>and</div>
                <div>
                  <input style={styles.number} type='number' min={attr.MIN} max={attr.MAX}
                    onChange={(e) => this.inputChange(e.target.value, a, 1)}/>
                  {this.hasUnits && <span> {PM_UNITS[criterion.subtype]}</span>}
                </div>
              </React.Fragment>}
              {this.isMeasurement && attr.operator !== null &&
                <span style={{paddingTop: '0.2rem'}}>&nbsp;Ranges: {attr.MIN} - {attr.MAX}</span>
              }
            </div>
          </div>)}
          {form.CAT.length > 0 && <React.Fragment>
            <div style={styles.orCircle}>OR</div>
            <div style={styles.label}>Categorical Values</div>
            <div>
              {form.CAT.map((attr, a) => <React.Fragment>
                <CheckBox key={a} checked={attr.checked} onChange={(e) => console.log(e)} />
                {attr.conceptName} <span style={styles.badge}> {attr.estCount}</span>
              </React.Fragment>)}
            </div>
          </React.Fragment>}
        </React.Fragment>}
        <div style={styles.countPreview}>
          <div style={styles.row}>
            {this.showCalc && <div style={styles.buttonContainer}>
              <Button type='primary' disabled={disabled}
                style={{
                  ...styles.button,
                  ...(disabled ? {opacity: 0.4} : {}),
                  background: colorWithWhiteness(colors.primary, .2)
                }}
                onClick={() => this.requestPreview()}>Calculate</Button>
            </div>}
            <div style={styles.resultsContainer}>
              <div style={{fontWeight: 'bold'}}>Results</div>
              <div>
                Number Participants:
                {count === null ? <span> -- </span> : <span> {count.toLocaleString()}</span>}
              </div>
            </div>
            {!loading && <div style={styles.buttonContainer}>
              <Button type='link'
                style={{...styles.button, color: colorWithWhiteness(colors.primary, .2)}}
                onClick={() => this.addAttrs()}> ADD THIS</Button>
            </div>}
          </div>
        </div>
      </div>;
    }
  }
);

@Component({
  selector: 'crit-list-attributes-page',
  template: '<div #root></div>'
})
export class ListAttributesPageComponent extends ReactWrapperBase {
  @Input('criterion') criterion: Props['criterion'];
  @Input('close') close: Props['close'];

  constructor() {
    super(AttributesPage, ['criterion', 'close']);
  }
}
