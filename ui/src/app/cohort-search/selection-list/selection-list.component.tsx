import * as React from 'react';

import {ModifierPage} from 'app/cohort-search/modifier-page/modifier-page.component';
import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow, FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {withCurrentCohortCriteria} from 'app/utils';
import {reactStyles} from 'app/utils';
import {currentCohortCriteriaStore, serverConfigStore} from 'app/utils/navigation';
import {Criteria, DomainType} from 'generated/fetch';
import * as fp from 'lodash/fp';

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
    height: 'calc(100% - 15rem)',
    minHeight: 'calc(100vh - 13rem)',
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
  }
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
  criteria?: Array<Selection>;
}

interface State {
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

export const SelectionList = withCurrentCohortCriteria()(class extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      modifierButtonText: 'APPLY MODIFIERS',
      showModifiersSlide: false
    };
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
    return <div style={{paddingLeft: '0.5rem'}}>
      {g['true'] && g['true'].length > 0 && this.renderCriteriaGroup(g['true'] , 'Standard Groups')}
      {g['false'] && g['false'].length > 0 && this.renderCriteriaGroup(g['false'], 'Source code Groups')}
    </div>;
  }

  removeCriteria(criteriaToDel) {
    const updateList =  fp.remove(
      (selection) => selection.parameterId === criteriaToDel.parameterId, this.props.criteria);
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

  render() {
    const {back, criteria} = this.props;
    const {modifierButtonText, showModifiersSlide} = this.state;
    return <div>
        {!showModifiersSlide && criteria &&  <React.Fragment>
          <h3 style={{...styles.sectionTitle, marginTop: 0}}>Add selected criteria to cohort</h3>
        <div style={{paddingTop: '0.5rem'}}>
          <div style={styles.selectionContainer}>
          {this.renderCriteria()}
          <div style = {{padding: '1rem 0 1.2rem 1.2rem'}}>
        <Button style={{backgroundColor: colors.white, border: '0.1rem solid' + colors.accent, color: colors.accent}}
                onClick = {() => this.setState({showModifiersSlide: true})}>
        {modifierButtonText}
        </Button>
        </div>
        </div></div>
        </React.Fragment>}
        {showModifiersSlide && <ModifierPage selections={criteria} applyModifiers={(modifier) => this.applyModifier(modifier)}/>}
       {!showModifiersSlide && <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '2rem'}}>
        <Button type='primary' style={styles.saveButton}>Save Criteria</Button>
        <Button type='link'
                style={{color: colors.primary, marginRight: '0.5rem'}}
                onClick={() => back()}>
          Back
        </Button>
      </FlexRowWrap>}
    </div>;
  }
});
