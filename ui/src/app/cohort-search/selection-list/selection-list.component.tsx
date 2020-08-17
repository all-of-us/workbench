import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {AttributesPageV2} from 'app/cohort-search/attributes-page-v2/attributes-page-v2.component';
import {saveCriteria} from 'app/cohort-search/cohort-search/cohort-search.component';
import {ModifierPage} from 'app/cohort-search/modifier-page/modifier-page.component';
import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {FlexColumn, FlexRow, FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentCohortCriteria, withCurrentCohortSearchContext} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  serverConfigStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {Criteria, DomainType} from 'generated/fetch';
import * as fp from 'lodash/fp';

const proIcons = {
  arrowLeft: '/assets/icons/arrow-left-regular.svg',
  times: '/assets/icons/times-light.svg'
};

const styles = reactStyles({
  buttonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '0.5rem',
    padding: '0.45rem 0rem'
  },
  button: {
    fontSize: '12px',
    height: '1.5rem',
    letterSpacing: '0.02rem',
    lineHeight: '0.75rem',
    margin: '0.25rem 0.5rem',
    padding: '0rem 0.75rem',
  },
  itemInfo: {
    width: '100%',
    minWidth: 0,
    flex: 1,
    display: 'flex',
    flexFlow: 'row nowrap',
    justifyContent: 'flex-start'
  },
  itemName: {
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.25rem',
    padding: 0
  },
  saveButton: {
    height: '2rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.5rem'
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 17rem)',
    minHeight: 'calc(100vh - 15rem)',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '95%',
  },
  selectionItem: {
    display: 'flex',
    fontSize: '14px',
    padding: '0',
    width: '100%',
    color: 'rgb(38, 34, 98)'
  },
  selectionPanel: {
    height: '35rem',
    paddingTop: '0.5rem',
  },
  selectionTitle: {
    color: colors.primary,
    margin: 0,
    padding: '0.5rem 0'
  },
  modifierButton: {
    position: 'absolute',
    bottom: '1rem' ,
    width: '80%',
    backgroundColor: colors.white,
    border: '0.1rem solid' + colors.accent,
    color: colors.accent
  },
  // Remove the following styles once enableCohortBuilderV2 is set to true
  selectionContainerModal: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 150px)',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '95%',
  },
  selectionPanelModal: {
    background: colorWithWhiteness(colors.black, 0.95),
    height: '100%',
    padding: '0.5rem 0 0 1rem',
  },
  selectionItemModal: {
    display: 'flex',
    fontSize: '14px',
    padding: '0.2rem 0.5rem 0',
    width: '100%',
  },
  backButton: {
    border: '0px',
    backgroundColor: 'none',
    color: colors.accent,
    marginRight: '1rem'
  },
  sectionTitle: {
    marginTop: '0.5rem',
    fontWeight: 600,
    color: colors.primary
  },
  navIcons: {
    position: 'absolute',
    right: '0',
    top: '0.75rem',
  },
});

export interface Selection extends Criteria {
  parameterId: string;
}

interface SelectionInfoProps {
  index: number;
  selection: Selection;
  removeSelection: Function;
}

interface SelectionInfoState {
  truncated: boolean;
}

// Delete once enableCohortBuilderV2 flag is removed
export class SelectionInfoModal extends React.Component<SelectionInfoProps, SelectionInfoState> {
  name: HTMLDivElement;
  constructor(props: SelectionInfoProps) {
    super(props);
    this.state = {truncated: false};
  }

  componentDidMount(): void {
    const {offsetWidth, scrollWidth} = this.name;
    this.setState({truncated: scrollWidth > offsetWidth});
  }

  get showType() {
    return ![
      DomainType.PHYSICALMEASUREMENT.toString(),
      DomainType.DRUG.toString(),
      DomainType.SURVEY.toString()
    ].includes(this.props.selection.domainId);
  }

  get showOr() {
    const {index, selection} = this.props;
    return index > 0 && selection.domainId !== DomainType.PERSON.toString();
  }

  render() {
    const {selection, removeSelection} = this.props;
    const itemName = <React.Fragment>
      {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
      {nameDisplay(selection)} {attributeDisplay(selection)}
    </React.Fragment>;
    return <div style={styles.selectionItemModal}>
      <button style={styles.removeSelection} onClick={() => removeSelection()}>
        <ClrIcon shape='times-circle'/>
      </button>
      <div style={styles.itemInfo}>
        {this.showOr && <strong>OR&nbsp;</strong>}
        {!!selection.group && <span>Group&nbsp;</span>}
        <TooltipTrigger disabled={!this.state.truncated} content={itemName}>
          <div style={styles.itemName} ref={(e) => this.name = e}>
            {itemName}
          </div>
        </TooltipTrigger>
      </div>
    </div>;
  }
}

export class SelectionInfo extends React.Component<SelectionInfoProps, SelectionInfoState> {
  name: HTMLDivElement;
  constructor(props: SelectionInfoProps) {
    super(props);
    this.state = {truncated: false};
  }

  componentDidMount(): void {
    const {offsetWidth, scrollWidth} = this.name;
    this.setState({truncated: scrollWidth > offsetWidth});
  }

  get showType() {
    return ![
      DomainType.PHYSICALMEASUREMENT.toString(),
      DomainType.DRUG.toString(),
      DomainType.SURVEY.toString()
    ].includes(this.props.selection.domainId);
  }

  get showOr() {
    const {index, selection} = this.props;
    return index > 0 && selection.domainId !== DomainType.PERSON.toString();
  }

  render() {
    const {selection, removeSelection} = this.props;
    const itemName = <React.Fragment>
      {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
      {nameDisplay(selection)} {attributeDisplay(selection)}
    </React.Fragment>;
    return <FlexColumn style={styles.selectionItem}>
      {this.showOr && <div style={{padding: '0.3rem 0rem 0.3rem 1rem'}}>OR&nbsp;</div>}
      <FlexRow style={{alignItems: 'baseline'}}>
      <button style={styles.removeSelection} onClick={() => removeSelection()}>
        <ClrIcon shape='times-circle'/>
      </button>
      <FlexColumn>
        <div>Group</div>
        <TooltipTrigger disabled={!this.state.truncated} content={itemName}>
          <div style={styles.itemName} ref={(e) => this.name = e}>
            {itemName}
          </div>
        </TooltipTrigger>
      </FlexColumn>
      </FlexRow>
    </FlexColumn>;
  }
}
interface Props {
  back: Function;
  close: Function;
  disableFinish: boolean;
  domain: DomainType;
  finish: Function;
  removeSelection: Function;
  selections: Array<Selection>;
  setView: Function;
  view: string;
  cohortContext?: any;
  criteria?: Array<Selection>;
}

interface State {
  attributesSelection: Criteria;
  modifierButtonText: string;
  showModifiersSlide: boolean;
}


export class SelectionListModalVersion extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  get showModifiers() {
    return ![DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(this.props.domain);
  }

  get showNext() {
    return this.showModifiers && this.props.view !== 'modifiers';
  }

  get showBack() {
    return this.showModifiers && this.props.view === 'modifiers';
  }

  render() {
    const {back, close, disableFinish, finish, removeSelection, selections, setView} = this.props;
    return <div style={styles.selectionPanelModal}>
      <h5 style={styles.selectionTitle}>Selected Criteria</h5>
      <div style={styles.selectionContainerModal}>
        {selections.map((selection, s) =>
            <SelectionInfoModal key={s}
                           index={s}
                           selection={selection}
                           removeSelection={() => removeSelection(selection)}/>
        )}
      </div>
      {!serverConfigStore.getValue().enableCohortBuilderV2 && <div style={styles.buttonContainer}>
        <Button type='link'
          style={{...styles.button, color: colors.dark, fontSize: '14px'}}
          onClick={() => close()}>
          Cancel
        </Button>
        {this.showNext && <Button type='primary' onClick={() => setView('modifiers')}
          style={styles.button}
          disabled={!selections || selections.length === 0}>
          Next
        </Button>}
        {this.showBack && <Button type='primary'
          style={styles.button}
          onClick={() => back()}>
          Back
        </Button>}
        <Button type='primary' onClick={() => finish()}
          style={styles.button}
          disabled={!selections || selections.length === 0 || disableFinish}>
          Finish
        </Button>
      </div>}
    </div>;
  }
}

export const SelectionList = fp.flow(withCurrentCohortCriteria(), withCurrentCohortSearchContext())(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        attributesSelection: undefined,
        modifierButtonText: 'APPLY MODIFIERS',
        showModifiersSlide: false
      };
    }

    componentDidMount(): void {
      this.subscription = attributesSelectionStore.subscribe(attributesSelection => {
        this.setState({attributesSelection});
        if (!!attributesSelection) {
          setSidebarActiveIconStore.next('criteria');
        }
      });
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (!this.props.criteria && !!prevProps.criteria) {
        this.setState({
          modifierButtonText: 'APPLY MODIFIERS',
          showModifiersSlide: false
        });
      }
    }

    get showModifiers() {
      return ![DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(this.props.domain);
    }

    get showNext() {
      return this.showModifiers && this.props.view !== 'modifiers';
    }

    get showBack() {
      return this.showModifiers && this.props.view === 'modifiers';
    }

    showOr(index, selection) {
      return index > 0 && selection.domainId !== DomainType.PERSON.toString();
    }

    renderCriteria() {
      const {criteria} = this.props;
      const g = fp.groupBy('isStandard', criteria);
      return <div style={{paddingLeft: '0.5rem', paddingBottom: '4rem'}}>
        {g['true'] && g['true'].length > 0 && this.renderCriteriaGroup(g['true'] , 'Standard Groups')}
        {g['false'] && g['false'].length > 0 && this.renderCriteriaGroup(g['false'], 'Source code Groups')}
      </div>;
    }

    removeCriteria(criteriaToDel) {
      const updateList = fp.remove((selection) => selection.parameterId === criteriaToDel.parameterId, this.props.criteria);
      currentCohortCriteriaStore.next(updateList);
    }

    renderCriteriaGroup(criteriaGroup, header) {
      return  <React.Fragment>
        <h3> {header}</h3>
        <hr style={{marginRight: '0.5rem'}}/>
        {criteriaGroup && criteriaGroup.map((criteria, index) =>
          <SelectionInfo key={index}
                         index={index}
                         selection={criteria}
                         removeSelection={() => this.removeCriteria(criteria)}/>

        )}
      </React.Fragment> ;
    }

    applyModifier(modifiers) {
      if (modifiers) {
        const modifierButtonText = '(' + modifiers.length + ')  MODIFIERS APPLIED';
        this.setState({showModifiersSlide: false, modifierButtonText: modifierButtonText});
      } else {
        this.setState({showModifiersSlide: false, modifierButtonText: 'APPLY MODIFIERS'});
      }
    }

    get showModifierButton() {
      const {criteria} = this.props;
      return criteria && criteria.length > 0 &&
        criteria[0].domainId !== DomainType.PHYSICALMEASUREMENT.toString()
        && criteria[0].domainId !== DomainType.PERSON.toString();
    }

    get showAttributesOrModifiers() {
      const {attributesSelection, showModifiersSlide} = this.state;
      return attributesSelection || showModifiersSlide;
    }

    render() {
      const {back, criteria} = this.props;
      const {attributesSelection, modifierButtonText, showModifiersSlide} = this.state;
      return <div>
        <FlexRow style={styles.navIcons}>
          {this.showAttributesOrModifiers &&
            <Clickable style={{marginRight: '1rem'}}
                       onClick={() => this.setState({attributesSelection: undefined})}>
              <img src={proIcons.arrowLeft}
                   style={{height: '21px', width: '18px'}}
                   alt='Go back'/>
            </Clickable>
          }
          <Clickable style={{marginRight: '1rem'}}
                     onClick={() => setSidebarActiveIconStore.next(undefined)}>
            <img src={proIcons.times}
                 style={{height: '27px', width: '17px'}}
                 alt='Close'/>
          </Clickable>
        </FlexRow>
          {!this.showAttributesOrModifiers && <React.Fragment>
            <h3 style={{...styles.sectionTitle, marginTop: 0}}>Add selected criteria to cohort</h3>
            <div style={{paddingTop: '0.5rem', position: 'relative'}}>
              <div style={styles.selectionContainer}>
                {this.renderCriteria()}
                {this.showModifierButton && <div style={{paddingLeft: '0.6rem'}}>
                  <Button type='secondaryOnDarkBackground' style={styles.modifierButton}
                          onClick={() => this.setState({showModifiersSlide: true})}>
                    {modifierButtonText}
                  </Button>
                </div>}
              </div>
            </div>
            <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '2rem'}}>
              <Button type='primary'
                      style={styles.saveButton}
                      onClick={() => saveCriteria()}>Save Criteria</Button>
              <Button type='link'
                      style={{color: colors.primary, marginRight: '0.5rem'}}
                      onClick={() => back()}>
                Back
              </Button>
            </FlexRowWrap>
          </React.Fragment>}
          {showModifiersSlide && <ModifierPage selections={criteria} applyModifiers={(modifier) => this.applyModifier(modifier)}/>}
          {!!attributesSelection && <AttributesPageV2 close={() => attributesSelectionStore.next(undefined)} node={attributesSelection}/>}
      </div>;
    }
  }
);
