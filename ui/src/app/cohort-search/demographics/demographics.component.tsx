import {Component, Input} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {ageCountStore} from 'app/cohort-search/search-state.service';
import {mapParameter, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {AttrName, CriteriaType, DomainType, Operator} from 'generated/fetch';

const minAge = 18;
const maxAge = 120;

/*
 * Sorts a plain JS array of plain JS objects first by a 'count' key and then
 * by a 'name' key
 */
function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
        ? (critA.name > critB.name ? 1 : -1)
        : diff;
}
interface Props {
  select: Function;
  selectedIds: Array<string>;
  selections: Array<any>;
  wizard: any;
}

interface State {
  ageTypeNodes: any;
  calculating: boolean;
  count: number;
  loading: boolean;
  nodes: Array<any>;
}

export class Demographics extends React.Component<Props, State> {
  readonly criteriaType = CriteriaType;
  readonly minAge = minAge;
  readonly maxAge = maxAge;
  subscription = new Subscription();
  selectedNode: any;
  enableCBAgeTypeOptions = serverConfigStore.getValue().enableCBAgeTypeOptions;
  ageTypes = [
    {label: 'Current Age', type: AttrName.AGE.toString()},
    {label: 'Age at Consent', type: AttrName.AGEATCONSENT.toString()},
    {label: 'Age at CDR Date', type: AttrName.AGEATCDR.toString()}
  ];

    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    ageType: new FormControl(AttrName.AGE.toString()),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }

  ageNode = {
    hasAncestorData: false,
    attributes: [],
    code: '',
    domainId: DomainType.PERSON,
    group: false,
    name: 'Age',
    parameterId: 'age-param',
    isStandard: true,
    type: CriteriaType.AGE,
    value: ''
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      ageTypeNodes: undefined,
      calculating: false,
      count: undefined,
      loading: true,
      nodes: undefined
    };
  }

  componentDidMount(): void {
    if (this.props.wizard.type === CriteriaType.AGE) {
      this.initAgeControls();
      this.initAgeRange();
      if (this.enableCBAgeTypeOptions) {
        this.loadAgeNodesFromApi();
      }
    } else {
      this.loadNodesFromApi();
    }
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {selections, wizard} = this.props;
    if (selections !== prevProps.selections && wizard.type !== CriteriaType.AGE) {
      this.calculate();
    }
  }

  componentWillUnmount(): void {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const {selections, wizard} = this.props;
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), wizard.type).then(response => {
      const nodes = response.items
        .filter(item => item.parentId !== 0)
        .sort(sortByCountThenName)
        .map(node => ({...node, parameterId: `param${node.conceptId || node.code}`}));
      if (selections.length) {
        this.calculate(true);
      }
      this.setState({loading: false, nodes});
    });
  }

  loadAgeNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    if (this.enableCBAgeTypeOptions) {
      const initialValue = {[AttrName.AGE.toString()]: [], 'AGE_AT_CONSENT': [], 'AGE_AT_CDR': []};
      cohortBuilderApi().findAgeTypeCounts(+cdrVersionId).then(response => {
        const ageTypeNodes = response.items.reduce((acc, item) => {
          acc[item.ageType].push(item);
          return acc;
        }, initialValue);
        setTimeout(() => this.centerAgeCount());
        this.setState({loading: false, ageTypeNodes});
      });
    }
  }

    /*
      * We want the two inputs to mirror the slider, so here we're wiring all
      * three inputs together using the valueChanges Observable and the
      * emitEvent option.  Setting emitEvent to false will prevent the other
      * Observables from firing when a control is updated this way, hence
      * preventing any infinite update cycles.
      */
  initAgeControls() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    this.subscription.add(this.ageRange.valueChanges.subscribe(([lo, hi]) => {
      min.setValue(lo, {emitEvent: false});
      max.setValue(hi, {emitEvent: false});
      if (this.enableCBAgeTypeOptions) {
        if (!!this.state.ageTypeNodes) {
          this.centerAgeCount();
        }
      } else {
        this.setState({count: null});
      }
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
        if (!this.enableCBAgeTypeOptions) {
          this.setState({count: null});
        }
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
        if (!this.enableCBAgeTypeOptions) {
          this.setState({count: null});
        }
      }
    }));
    if (this.enableCBAgeTypeOptions) {
      this.subscription.add(this.demoForm.get('ageType').valueChanges.subscribe(name => {
        this.calculateAge();
        this.props.select(this.selectedNode);
      }));
    }
  }

  checkMax() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (max.value < min.value) {
      max.setValue(min.value);
    }
  }

  checkMin() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (min.value > max.value) {
      min.setValue(max.value);
    } else if (min.value < this.minAge) {
      min.setValue(this.minAge);
    }
  }

  initAgeRange() {
    const {select, selections, wizard} = this.props;
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    const ageType = this.demoForm.get('ageType');
    let attributes;
    if (selections.length && selections[0].type === CriteriaType.AGE) {
      attributes = selections[0].attributes;
      const range = selections[0].attributes[0].operands.map(op => parseInt(op, 10));
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
      if (this.enableCBAgeTypeOptions) {
        ageType.setValue(attributes[0].name, {emitEvent: false});
      }
      this.setState({count: wizard.count});
    } else {
      attributes = [{
        name: AttrName.AGE,
        operator: Operator.BETWEEN,
        operands: [minAge.toString(), maxAge.toString()]
      }];
    }
    this.selectedNode = {
      ...this.ageNode,
      name: `Age In Range ${attributes[0].operands[0]} - ${attributes[0].operands[1]}`,
      attributes,
    };
    if (!selections.length) {
      setTimeout(() => select(this.selectedNode));
    }
    if (!this.enableCBAgeTypeOptions) {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      if (!ageCountStore.getValue()[cdrVersionId]) {
        // Get total age count for this cdr version if it doesn't exist in the store yet
        this.calculateAge(true);
      } else if (this.setTotalAge) {
        this.setState({count:  ageCountStore.getValue()[cdrVersionId]});
      }
    }

    const ageDiff = this.ageRange.valueChanges
      .debounceTime(250)
      .distinctUntilChanged()
      .map(([lo, hi]) => {
        const attr = {
          name: ageType.value,
          operator: Operator.BETWEEN,
          operands: [lo.toString(), hi.toString()]
        };
        return {
          ...this.ageNode,
          name: `Age In Range ${lo} - ${hi}`,
          attributes: [attr],
        };
      }).subscribe(newNode => {
        this.selectedNode = newNode;
        select(this.selectedNode);
      });
    this.subscription.add(ageDiff);
  }

  centerAgeCount() {
    if (this.enableCBAgeTypeOptions) {
      this.calculateAge();
      const slider = document.getElementsByClassName('noUi-connect')[0] as HTMLElement;
      const wrapper = document.getElementById('count-wrapper');
      const count = document.getElementById('age-count');
      wrapper.setAttribute(
        'style', 'width: ' + slider.offsetWidth + 'px; left: ' + slider.offsetLeft + 'px;'
      );
      // set style properties also for cross-browser compatibility
      wrapper.style.width = slider.offsetWidth.toString();
      wrapper.style.left = slider.offsetLeft.toString();
      if (!!count && slider.offsetWidth < count.offsetWidth) {
        const margin = (slider.offsetWidth - count.offsetWidth) / 2;
        count.setAttribute('style', 'margin-left: ' + margin + 'px;');
        count.style.marginLeft = margin.toString();
      }
    }
  }

  selectOption = (opt: any) => {
    triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(opt.type)} - ${opt.name}`);
    this.props.select({...opt, name: `${typeToTitle(opt.type)} - ${opt.name}`});
  }

  calculate(init?: boolean) {
    let count = 0;
    this.props.selections.forEach(sp => {
      if (init) {
        const node = this.state.nodes.find(n => n.conceptId === sp.conceptId);
        if (node) {
          sp.count = node.count;
        }
      }
      count += sp.count;
    });
    this.setState({count});
  }

  calculateAge(init?: boolean) {
    if (this.enableCBAgeTypeOptions) {
      const ageType = this.demoForm.get('ageType').value;
      const min = this.demoForm.get('ageMin').value;
      const max = this.demoForm.get('ageMax').value;
      const count = this.state.ageTypeNodes[ageType]
        .filter(node => node.age >= min && node.age <= max)
        .reduce((acc, node) => acc + node.count, 0);
      this.setState({count});
    } else {
      if (!init || this.setTotalAge) {
        this.setState({calculating: true});
      }
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const parameter = init ? {
        ...this.ageNode,
        name: `Age In Range ${minAge} - ${maxAge}`,
        attributes: [{
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [minAge.toString(), maxAge.toString()]
        }],
      } : this.selectedNode;
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: DomainType.PERSON.toString(),
            searchParameters: [mapParameter(parameter)],
            modifiers: []
          }],
          temporal: false
        }],
        dataFilters: []
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(response => {
        if (init) {
          const ageCounts = ageCountStore.getValue();
          ageCounts[cdrVersionId] = response;
          ageCountStore.next(ageCounts);
          if (this.setTotalAge) {
            this.setState({count: response});
          }
        } else {
          this.setState({count: response});
        }
        this.setState({calculating: false});
      }, (err) => {
        console.error(err);
        this.setState({calculating: false});
      });
    }
  }

  get noSexData() {
    const {wizard: {type}} = this.props;
    const {loading, nodes} = this.state;
    return !loading && type === CriteriaType.SEX && nodes.length === 0;
  }

  get showPreview() {
    const {selections, wizard} = this.props;
    return !this.state.loading
      && (selections && selections.length)
      && !(wizard.type === CriteriaType.AGE && this.enableCBAgeTypeOptions);
  }

  // Checks if form is in its initial state and if a count already exists before setting the total age count
  get setTotalAge() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    return min.value === minAge && max.value === maxAge && !this.state.count;
  }

  render() {
    const {selectedIds, wizard} = this.props;
    const {calculating, count, loading, nodes} = this.state;
    return loading
      ? <SpinnerOverlay/>
      : <React.Fragment>
      <form className='form-container'>
        {wizard.type === CriteriaType.AGE && <div>
          <div className='age-label'>
            Age Range
          </div>
          <div className='control-wrapper slider-wrapper'>
            <input type='number'
              id='min-age'
              min={minAge} max={maxAge}
              className='number-display'
              onBlur={() => this.checkMin()}/>
            <div className='slider'>
              {this.enableCBAgeTypeOptions && <div id='count-wrapper'>
                {calculating
                  ? <span className='spinner spinner-inline'></span>
                  : <span id='age-count' className='badge badge-info'>
                      {count.toLocaleString()}
                    </span>
                }
              </div>}
            {/* nouislider here */}
            </div>
            <input type='number'
              id='max-age'
              min={minAge} max={maxAge}
              className='number-display'
              onBlur={() => this.checkMax()}/>
          </div>
          {serverConfigStore.getValue().enableCBAgeTypeOptions && <div style={{marginLeft: '1rem'}}>
            {this.ageTypes.map((ageType, a) => <div key={a} className='radio-inline'>
              <input type='radio' id={`age_${a}`} value={ageType.type}/>
              <label>{ageType.label}</label>
            </div>)}
          </div>}
          {loading && <div className='spinner' style={{left: '45%'}}></div>}
        </div>}

        <div>
          {this.noSexData && <div className='alert alert-warning'>
            <div className='alert-items'>
              <div className='alert-item static'>
                <div className='alert-icon-wrapper'>
                  <ClrIcon className='alert-icon is-solid' shape='exclamation-triangle'/>
                </div>
                <span className='alert-text'>
                  This data does not exist in the dataset you’re currently using.
                   To use this data, please create a new workspace with the most recent dataset.
                </span>
              </div>
            </div>
          </div>}
          <div className='control-wrapper'>
            <div className='ds-wrapper'>
              <div className='ds-options'>
                <div className='select-box'>
                  <div className='option-list' >
                    {loading && <span className='spinner spinner-md'></span>}
                    {nodes.map((opt, o) => <div key={o} className='option' onClick={() => this.selectOption(opt)}>
                      {selectedIds.includes(opt.parameterId)
                        ? <ClrIcon shape='check-circle' size='20'  className='selection-icon items-disabled-icon'/>
                        : <ClrIcon shape='plus-circle'  size='20' className='selection-icon items-selection-icon'/>
                      }
                      {opt.name}
                      {opt.count !== null && <span className='badge badge-info'>
                        {opt.count.toLocaleString()}
                      </span>}
                    </div>)}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
      {this.showPreview && <div className='count-preview'>
        {wizard.type === CriteriaType.AGE && <div style={{marginRight: '0.25rem'}}>
          <button className='btn btn-primary' disabled={calculating || count !== null} onClick={() => this.calculateAge()}>
            Calculate
          </button>
        </div>}
        {count !== null || wizard.type === CriteriaType.AGE && <div>
          <div className='result-text'>
            Results
          </div>
          <div>
            Number Participants:
            <span className='text-bold'>
              {count !== null ? count.toLocaleString() : ' -- '}
            </span>
          </div>
        </div>}
      </div>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'crit-demographics',
  template: '<div #root></div>'
})
export class DemographicsComponent extends ReactWrapperBase {
  @Input('select') select: Props['select'];
  @Input('selectedIds') selectedIds: Props['selectedIds'];
  @Input('selections') selections: Props['selections'];
  @Input('wizard') wizard: Props['wizard'];

  constructor() {
    super(Demographics, ['select', 'selectedIds', 'selections', 'wizard']);
  }
}
