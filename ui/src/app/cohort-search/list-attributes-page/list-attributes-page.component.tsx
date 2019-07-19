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
  },
  errors: {
    background: '#f5dbd9',
    color: '#565656',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    margin: '0.25rem 0.5rem',
    padding: '3px 5px'
  },
  errorItem: {
    lineHeight: '16px',
  },
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
  count: number;
  countError: boolean;
  form: any;
  loading: boolean;
  options: any;
}
export const AttributesPage = withCurrentWorkspace() (
  class extends React.Component<Props, State> {
    units = PM_UNITS;
    selectedCode: any;

    constructor(props: Props) {
      super(props);
      this.state = {
        count: null,
        countError: false,
        form: {EXISTS: false, NUM: [], CAT: []},
        loading: false,
        options: [
          {label: 'Equals', value: Operator.EQUAL},
          {label: 'Greater than or Equal to', value: Operator.GREATERTHANOREQUALTO},
          {label: 'Less than or Equal to', value: Operator.LESSTHANOREQUALTO},
          {label: 'Between', value: Operator.BETWEEN},
        ],
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
      if (this.isBP) {
        const other = index === 0 ? 1 : 0;
        if (value === AttrName[AttrName.ANY]) {
          form.NUM[other].operator = AttrName[AttrName.ANY];
          form.NUM[other].operands = form.NUM[index].operands = [];
        } else if (form.NUM[other].operator === AttrName[AttrName.ANY]) {
          form.NUM[other].operator = value;
        }
      } else if (value === AttrName[AttrName.ANY]) {
        form.NUM[index].operands = [];
      } else if (value !== Operator[Operator.BETWEEN]) {
        form.NUM[index].operands.splice(1);
      }
      const count = value === AttrName[AttrName.ANY] ? criterion.count : null;
      this.setState({form, count});
    }

    inputChange(input: string, index: number, operand: number) {
      console.log(parseInt(input, 10));
      const {form} = this.state;
      let value = input;
      if (value && value.length > 10) {
        value = value.slice(0, 10);
      }
      form.NUM[index].operands[operand] = parseInt(value, 10);
      console.log(form.NUM[index].operands);
      console.log(form.NUM[index].operands.some(op => op < 0));
      this.setState({form, count: null});
    }

    validateForm() {
      const {form} = this.state;
      let formErrors = new Set(), formValid = true;
      if (form.EXISTS) {
        return {formValid, formErrors};
      }
      formErrors = form.NUM.reduce((acc, attr) => {
        switch (attr.operator) {
          case null:
            formValid = false;
            return acc;
          case 'ANY':
            return acc;
          case Operator.BETWEEN:
            if (attr.operands.length < 2) {
              formValid = false;
            }
            break;
          default:
            if (attr.operands.length === 0) {
              formValid = false;
            }
        }
        if (attr.operands.includes(NaN)) {
          formValid = false;
          acc.add('Form can only accept valid numbers');
        }
        if (this.isPM && attr.operands.some(op => op < 0)) {
          formValid = false;
          acc.add('Form cannot accept negative values');
        }
        if (this.isMeasurement && attr.operands.some(op => op < attr.MIN || op > attr.MAX)) {
          formValid = false;
          acc.add(`Values must be between ${attr.MIN} and ${attr.MAX}`);
        }
        return acc;
      }, formErrors);
      formValid = formValid || (this.isMeasurement && form.CAT.some(attr => attr.checked));
      return {formValid, formErrors};
    }

    get paramId() {
      const {criterion: {conceptId, id}} = this.props;
      // TODO replace this.selectedCode below
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
      this.setState({count: null, loading: true, countError: false});
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
        this.setState({loading: false, countError: true});
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
      return this.props.criterion.domainId === DomainType.MEASUREMENT;
    }

    get isPM() {
      return this.props.criterion.domainId === DomainType.PHYSICALMEASUREMENT;
    }

    get isBP() {
      return this.props.criterion.subtype === CriteriaSubType.BP;
    }

    render() {
      const {criterion} = this.props;
      const {count, countError, form, loading, options} = this.state;
      const {formValid, formErrors} = this.validateForm();
      const disabled = loading || form.EXISTS || !formValid
        || form.NUM.every(attr => attr.operator === 'ANY');
      return <div style={{margin: '0.5rem 0 1.5rem'}}>
        {!!formErrors.size && <div style={styles.errors}>
          {Array.from(formErrors).map((err, e) => <div key={e} style={styles.errorItem}>
            {err}
          </div>)}
        </div>}
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
            <div style={styles.buttonContainer}>
              <Button type='primary' disabled={disabled}
                style={{
                  ...styles.button,
                  ...(disabled ? {opacity: 0.4} : {}),
                  background: colorWithWhiteness(colors.primary, .2)
                }}
                onClick={() => this.requestPreview()}>Calculate</Button>
            </div>
            <div style={styles.resultsContainer}>
              <div style={{fontWeight: 'bold'}}>Results</div>
              <div>
                Number Participants:
                {count === null ? <span> -- </span> : <span> {count.toLocaleString()}</span>}
              </div>
            </div>
            {!loading && formValid && <div style={styles.buttonContainer}>
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
