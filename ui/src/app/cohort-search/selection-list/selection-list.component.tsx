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
import {Attribute, Criteria, DomainType, Modifier} from 'generated/fetch';
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
  modifierButtonContainer: {
    bottom: '0.5rem',
    paddingLeft: '0.5rem',
    position: 'absolute',
    width: 'calc(100% - 1.5rem)'
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
    height: '80%',
    lineHeight: '0.75rem',
    marginTop: '0.5rem',
    padding: '0.5rem',
    position: 'relative',
  },
  selectionItem: {
    display: 'flex',
    fontSize: '14px',
    padding: '0',
    width: '100%',
    color: 'rgb(38, 34, 98)'
  },
  selectionList: {
    height: '100%',
    overflow: 'hidden auto',
    paddingLeft: '0.5rem',
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
    height: '1.75rem',
    maxWidth: '100%',
    width: '100%',
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
  selectionHeader: {
    color: colors.primary,
    fontWeight: 600,
    marginTop: '0.5rem'
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

function mapCriteria(crit: Selection) {
  return {
    attributes: crit.attributes,
    code: crit.code,
    domainId: crit.domainId,
    group: crit.group,
    hasAncestorData: crit.hasAncestorData,
    isStandard: crit.isStandard,
    name: crit.name,
    parameterId: crit.parameterId,
    type: crit.type
  };
}

export interface Selection extends Criteria {
  attributes?: Array<Attribute>;
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

  render() {
    const {index, selection, removeSelection} = this.props;
    const itemName = <React.Fragment>
      {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
      {nameDisplay(selection)}
    </React.Fragment>;
    return <FlexColumn style={styles.selectionItem}>
      {index > 0 && <div style={{padding: '0.3rem 0rem 0.3rem 1rem'}}>OR&nbsp;</div>}
      <FlexRow style={{alignItems: 'baseline'}}>
      <button style={styles.removeSelection} onClick={() => removeSelection()}>
        <ClrIcon shape='times-circle'/>
      </button>
      <FlexColumn style={{width: 'calc(100% - 1rem)'}}>
        {selection.group && <div>Group</div>}
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
  disableSave: boolean;
  modifiers: Array<Modifier>;
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
        disableSave: false,
        modifiers: [],
        showModifiersSlide: false
      };
    }

    componentDidMount(): void {
      const {cohortContext} = this.props;
      if (!!cohortContext) {
        // Check for disabling the Save Criteria button
        this.setState({modifiers: cohortContext.item.modifiers}, () => this.checkCriteriaChanges());
      }
      this.subscription = attributesSelectionStore.subscribe(attributesSelection => {
        this.setState({attributesSelection});
        if (!!attributesSelection) {
          setSidebarActiveIconStore.next('criteria');
        }
      });
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      const {cohortContext, criteria} = this.props;
      if (!criteria && !!prevProps.criteria) {
        this.setState({showModifiersSlide: false});
      }
      if (!!cohortContext && !!criteria && criteria !== prevProps.criteria) {
        // Each time the criteria changes, we check for disabling the Save Criteria button again
        this.checkCriteriaChanges();
      }
    }

    checkCriteriaChanges() {
      const {cohortContext, criteria} = this.props;
      if (criteria.length === 0) {
        this.setState({disableSave: true});
      } else {
        // We need to check for changes to criteria selections AND the modifiers array
        const mappedCriteriaString = JSON.stringify({
          criteria: criteria.map(mapCriteria),
          modifiers: this.state.modifiers
        });
        const mappedParametersString = JSON.stringify({
          criteria: cohortContext.item.searchParameters.map(mapCriteria),
          modifiers: cohortContext.item.modifiers
        });
        this.setState({disableSave: mappedCriteriaString === mappedParametersString});
      }
    }

    removeCriteria(criteriaToDel) {
      const updateList = fp.remove((selection) => selection.parameterId === criteriaToDel.parameterId, this.props.criteria);
      currentCohortCriteriaStore.next(updateList);
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
      const {back, cohortContext, criteria} = this.props;
      const {attributesSelection, disableSave, showModifiersSlide} = this.state;
      return <div id='selection-list' style={{height: '100%'}}>
        <FlexRow style={styles.navIcons}>
          {this.showAttributesOrModifiers &&
            <Clickable style={{marginRight: '1rem'}}
                       onClick={() => this.setState({attributesSelection: undefined, showModifiersSlide: false})}>
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
            <div style={styles.selectionContainer}>
              {!!criteria && <div style={this.showModifierButton
                ? {...styles.selectionList, height: 'calc(100% - 2rem)'}
                : styles.selectionList}>
                {criteria.map((selection, s) => <SelectionInfo key={s}
                  index={s}
                  selection={selection}
                  removeSelection={() => this.removeCriteria(selection)}/>
                )}
              </div>}
              {this.showModifierButton && <div style={styles.modifierButtonContainer}>
                <Button type='secondaryLight' style={styles.modifierButton}
                        onClick={() => this.setState({showModifiersSlide: true})}>
                  {cohortContext.item.modifiers.length > 0
                    ? '(' + cohortContext.item.modifiers.length + ')  MODIFIERS APPLIED'
                    : 'APPLY MODIFIERS'
                  }
                </Button>
              </div>}
            </div>
            <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '1rem'}}>
              <Button type='primary'
                      style={styles.saveButton}
                      disabled={disableSave}
                      onClick={() => saveCriteria()}>Save Criteria</Button>
              <Button type='link'
                      style={{color: colors.primary, marginRight: '0.5rem'}}
                      onClick={() => back()}>
                Back
              </Button>
            </FlexRowWrap>
          </React.Fragment>}
          {showModifiersSlide && !attributesSelection && <ModifierPage selections={criteria}
                                               closeModifiers={() => {
                                                 this.setState({showModifiersSlide: false});
                                                 this.checkCriteriaChanges();
                                               }}/>}
          {!!attributesSelection && <AttributesPageV2 close={() => attributesSelectionStore.next(undefined)}
                                                      node={attributesSelection}/>}
      </div>;
    }
  }
);
