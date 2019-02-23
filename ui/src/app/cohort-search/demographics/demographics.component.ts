import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {fromJS, List, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeCriteriaSubtype,
  activeParameterList,
  CohortSearchActions,
  CohortSearchState,
  participantsCount,
  previewStatus,
} from 'app/cohort-search/redux';
import {currentWorkspaceStore} from 'app/utils/navigation';

import {Attribute, CohortBuilderService, Operator, TreeSubType, TreeType} from 'generated';

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

@Component({
  selector: 'crit-demographics',
  templateUrl: './demographics.component.html',
    // Buttons styles picked up from parent (wizard.ts)
  styleUrls: [
    './demographics.component.css',
    '../../styles/buttons.css',
  ]
})
export class DemographicsComponent implements OnInit, OnChanges, OnDestroy {
  @select(activeCriteriaSubtype) subtype$;
  @select(activeParameterList) selection$;
  @Input() selectedParamId: any;
  @Input() selectedTypes: any;
  @Output() itemsAddedFlag = new EventEmitter<boolean>();
  @select(previewStatus) preview$;
  @select(participantsCount) count$;
  readonly treeSubType = TreeSubType;
  readonly minAge = minAge;
  readonly maxAge = maxAge;
  loading = {
    'AGE': false,
    'GEN': false,
    'RACE': false,
    'ETH': false,
    'DEC': false
  };
  subscription = new Subscription();
  hasSelection = false;
  selectedNode: any;
  preview = Map();
  ageClicked = false;


    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    deceased: new FormControl(),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

    /* Storage for the demographics options (fetched via the API) */
  ageNode;
  ageNodes: Array<any>;
  ageCount: number;
  deceasedNode;

  genderNodes = List();
  initialGenders = List();

  raceNodes = List();
  initialRaces = List();

  ethnicityNodes = List();
  initialEthnicities = List();
  deceasedClicked = false;
  noSelection = true;
  isCancelTimerInitiated: any = false;
  showCalculateContainer = false;
  count: any;
  subtype: string;

  constructor(
    private api: CohortBuilderService,
    private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>
  ) {}

  ngOnChanges() {
    this.showCalculateContainer = false;
    if (this.selectedTypes === 'Age') {
      this.ageClicked = false;
    } else if (this.selectedTypes === 'Deceased') {
      this.deceasedClicked = false;
    }
    if (this.noSelection && this.selectedParamId) {
      if (this.isCancelTimerInitiated) {
        clearTimeout(this.isCancelTimerInitiated);
        return;
      }
    } else if (this.selectedParamId) {
      this.showCalculateContainer = true;
      this.getSearchResponse();
    }

  }

  ngOnInit() {
        // Set back to false at the end of loadNodesFromApi (i.e. the end of the
        // initialization routine)
    this.subscription = this.selection$.subscribe(sel => this.hasSelection = sel.size > 0);
    this.initAgeControls();
    this.subscription.add(this.preview$.subscribe(prev => this.preview = prev));
    this.selection$.first().subscribe(selections => {
            /*
             * Each subtype of DEMO requires subtly different initialization, which
             * is handled by special-case methods which each receive any selected
             * criteria already in the state (i.e. if we're editing a search group
             * item).  Finally we load the relevant criteria from the API.
             */
      this.initialGenders = selections
                .filter(s => s.get('subtype') === TreeSubType[TreeSubType.GEN]);
      this.initialRaces = selections
                .filter(s => s.get('subtype') === TreeSubType[TreeSubType.RACE]);
      this.initialEthnicities = selections
                .filter(s => s.get('subtype') === TreeSubType[TreeSubType.ETH]);
      this.initDeceased(selections);
      this.initAgeRange(selections);
      this.loadNodesFromApi();
    });

    this.subscription.add(this.subtype$.subscribe(sub => this.subtype = sub));

    this.subscription.add(this.selection$
      .map(sel => sel.size === 0)
      .subscribe(sel => this.noSelection = sel)
        );


    this.subscription.add(this.count$
      .subscribe(totalCount => {
        if (totalCount) {
          this.count = totalCount;
        }
      }));

    this.subscription.add (this.ngRedux
      .select(activeParameterList)
      .subscribe(val => {
        val.forEach( paramList => {

          if (paramList.get('subtype') === TreeSubType.DEC) {
            this.deceasedClicked = paramList.get('name');
          } else if (paramList.get('subtype') === TreeSubType.AGE) {
            this.ageClicked = paramList.get('name');
          }
        });

      }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    /*
     * Each subtype's possible criteria is loaded via the API.  Race and Gender
     * criteria nodes become options in their respective dropdowns; deceased
     * and age are used as templates for constructing relevant seach
     * parameters.  Upon load we immediately map the criteria to immutable
     * objects complete with deterministically generated `parameterId`s and
     * sort them by count, then by name.
     */
    [
      TreeSubType[TreeSubType.AGE],
      TreeSubType[TreeSubType.DEC],
      TreeSubType[TreeSubType.GEN],
      TreeSubType[TreeSubType.RACE],
      TreeSubType[TreeSubType.ETH]
    ].forEach(code => {
      this.loading[code] = true;
      this.api.getCriteriaBy(cdrid, TreeType[TreeType.DEMO], code, null, null)
        .subscribe(response => {
          const items = response.items
                    .filter(item => item.parentId !== 0
                        || code === TreeSubType[TreeSubType.DEC]);
          items.sort(sortByCountThenName);
          const nodes = fromJS(items).map(node => {
            if (node.get('subtype') !== TreeSubType[TreeSubType.AGE]) {
              const paramId =
                            `param${node.get('conceptId', node.get('code'))}`;
              node = node.set('parameterId', paramId);
            }
            return node;
          });
          this.loadOptions(nodes, code);
        });
    });
  }

  loadOptions(nodes: any, subtype: string) {
    this.loading[subtype] = false;
    switch (subtype) {
            /* Age and Deceased are single nodes we use as templates */
      case TreeSubType[TreeSubType.AGE]:
        this.ageNode = nodes.get(0);
        this.ageNodes = nodes.toJS();
        this.calculateAgeCount();
        const attr = fromJS(<Attribute>{
          name: 'Age',
          operator: Operator.BETWEEN,
          operands: [minAge.toString(), maxAge.toString()],
          conceptId: this.ageNode.get('conceptId', null)
        });
        const paramId = `age-param${this.ageNode.get('id')}`;
        this.selectedNode = this.ageNode
          .set('parameterId', paramId)
          .set('attributes', [attr]);
        this.actions.addParameter(this.selectedNode);
        break;
      case TreeSubType[TreeSubType.DEC]:
        this.deceasedNode = nodes.get(0);
        break;
            /* Gender, Race, and Ethnicity are all used to generate option lists */
      case TreeSubType[TreeSubType.GEN]:
        this.genderNodes = nodes;
        break;
      case TreeSubType[TreeSubType.RACE]:
        this.raceNodes = nodes;
        break;
      case TreeSubType[TreeSubType.ETH]:
        this.ethnicityNodes = nodes;
        break;
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
      setTimeout(() => this.centerAgeCount(), 300);
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
      }
    }));
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

    /*
     * The next four initialization methods do the following: if a value exists
     * for that subtype already (i.e. we're editing), set that value on the
     * relevant form control.  Also set up a subscriber to the observable stream
     * coming from that control's `valueChanges` that will fire ADD_PARAMETER or
     * REMOVE_PARAMETER events as appropriate.
     *
     * The exact ordering of these operations is slightly different per type.
     * For race and gender, we watch the valueChanges stream in pairs so that we
     * can generate added and removed lists, so they get their initialization
     * _after_ the change listener is attached (otherwise we would never detect
     * the _first_ selection, which would be dropped by `pairwise`).
     *
     * For Age, since the slider emits an event with every value, and there can
     * be many values very quickly, we debounce the event emissions by 1/4 of a
     * second.  Furthermore, we generate the correct parameter ID as a hash from
     * the given user input so that we can determine if the age range has changed
     * or not.
     *
     * (TODO: can we reduce all age criterion to 'between'?  Or should we be
     * determining different attributes (operators, really) be examining the
     * bounds and the diff between low and high?  And should we be generating the
     * parameterId by stringifying the attribute (which may be more stable than
     * using a hash?)
     */
  initAgeRange(selections) {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');

    const existent = selections.find(s => s.get('subtype') === TreeSubType[TreeSubType.AGE]);
    if (existent) {
      const range = existent.getIn(['attributes', '0', 'operands']).toArray();
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
    }
    const selectedAge = this.selection$
            .map(selectedNodes => selectedNodes
              .find(node => node.get('subtype') === TreeSubType[TreeSubType.AGE])
            );

    const ageDiff = this.ageRange.valueChanges
            .debounceTime(250)
            .distinctUntilChanged()
            .map(([lo, hi]) => {
                const attr = fromJS(<Attribute>{
                    name: 'Age',
                    operator: Operator.BETWEEN,
                    operands: [lo, hi],
                    conceptId: this.ageNode.get('conceptId', null)
                });
                const paramId = `age-param${this.ageNode.get('id')}`;
                return this.ageNode
                    .set('parameterId', paramId)
                    .set('attributes', [attr]);
            })
            .withLatestFrom(selectedAge)
            .filter(([newNode, oldNode]) => {
              this.selectedNode = newNode;
              this.actions.addParameter(newNode);
              if (oldNode) {
                return oldNode.get('parameterId') !== newNode.get('parameterId');
              }
              return true;
            });
    this.subscription.add(ageDiff.subscribe(([newNode, oldNode]) => {
      if (oldNode) {
        this.actions.removeParameter(oldNode.get('parameterId'));
      }
      this.selectedNode = newNode;
    }));
  }

  initDeceased(selections) {
    const existent = selections.find(s => s.get('subtype') === TreeSubType[TreeSubType.DEC]);
    if (existent !== undefined) {
      this.deceased.setValue(true);

    }
    this.subscription.add(this.deceased.valueChanges.subscribe(includeDeceased => {
      if (!this.deceasedNode) {
        console.warn('No node from which to make parameter for deceased status');
        return ;
      }
      if (includeDeceased) {
        this.actions.addParameter(this.deceasedNode);
        this.actions.removeParameter(this.selectedNode.get('parameterId'));
      } else {
        this.actions.addParameter(this.selectedNode);
        this.actions.removeParameter(this.deceasedNode.get('parameterId'));
      }
    }));
  }

  selectedDeasease() {
    this.showCalculateContainer = true;
    this.actions.addParameter(this.deceasedNode);
    this.getSearchResponse();
  }

  calculateAgeCount() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    let count = 0;
    for (let i = min.value; i <= max.value; i++) {
      const ageNode = this.ageNodes.find(node => node.name === i.toString());
      count += ageNode.count;
    }
    this.ageCount = count;
  }

  centerAgeCount() {
    if (this.ageNodes) {
      this.calculateAgeCount();
      this.ageClicked = false;
      const slider = <HTMLElement>document.getElementsByClassName('noUi-connect')[0];
      const wrapper = document.getElementById('count-wrapper');
      const count = document.getElementById('age-count');
      wrapper.setAttribute(
        'style', 'width: ' + slider.offsetWidth + 'px; left: ' + slider.offsetLeft + 'px;'
      );
      // set style properties also for cross-browser compatibility
      wrapper.style.width = slider.offsetWidth.toString();
      wrapper.style.left = slider.offsetLeft.toString();
      if (slider.offsetWidth < count.offsetWidth) {
        const margin = (slider.offsetWidth - count.offsetWidth) / 2;
        count.setAttribute('style', 'margin-left: ' + margin + 'px;');
        count.style.marginLeft = margin.toString();
      }
    }
  }

  getAgeValue() {
    this.showCalculateContainer = true;
    if (!this.selectedNode) {
      this.ageRange.updateValueAndValidity ({onlySelf: false, emitEvent: true});
      if (this.isCancelTimerInitiated) {
        clearTimeout(this.isCancelTimerInitiated);
      }
      this.isCancelTimerInitiated =  setTimeout (() => {
        this.actions.addParameter(this.selectedNode);
        this.actions.requestPreview();
        this.showCalculateContainer = false;
      }, 500);
    } else {
      this.actions.addParameter(this.selectedNode);
      this.actions.requestPreview();
      this.showCalculateContainer = false;
    }
  }

  getSearchResponse() {
    if (this.isCancelTimerInitiated) {
      clearTimeout(this.isCancelTimerInitiated);
    }
    this.isCancelTimerInitiated = setTimeout(() => {
      this.actions.requestPreview();
      this.showCalculateContainer = false;
    }, 200);
  }

  getItems(flag) {
    if (flag) {
      this.showCalculateContainer = true;
      this.getSearchResponse();
    } else {
      this.showCalculateContainer = false;
    }

  }
}
