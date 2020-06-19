import * as React from 'react';

import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Criteria, DomainType} from 'generated/fetch';

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
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 150px)',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '95%',
  },
  selectionItem: {
    display: 'flex',
    fontSize: '14px',
    padding: '0.2rem 0.5rem 0',
    width: '100%',
  },
  selectionPanel: {
    background: colorWithWhiteness(colors.black, 0.95),
    height: '100%',
    padding: '0.5rem 0 0 1rem',
  },
  selectionTitle: {
    color: colors.primary,
    margin: 0,
    padding: '0.5rem 0'
  }
});

interface Selection extends Criteria {
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
    return <div style={styles.selectionItem}>
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
}

export class SelectionList extends React.Component<Props> {
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
    return <div style={styles.selectionPanel}>
      <h5 style={styles.selectionTitle}>Selected Criteria</h5>
      <div style={styles.selectionContainer}>
        {selections.map((selection, s) =>
          <SelectionInfo key={s}
            index={s}
            selection={selection}
            removeSelection={() => removeSelection(selection)}/>
        )}
      </div>
      <div style={styles.buttonContainer}>
        <Button type='link'
          style={{...styles.button, color: colors.dark, fontSize: '14px'}}
          onClick={() => close()}>
          Cancel
        </Button>
        {this.showNext && <Button type='primary' onClick={() => setView('modifiers')}
          style={styles.button}
          disabled={selections.length === 0}>
          Next
        </Button>}
        {this.showBack && <Button type='primary'
          style={styles.button}
          onClick={() => back()}>
          Back
        </Button>}
        <Button type='primary' onClick={() => finish()}
          style={styles.button}
          disabled={selections.length === 0 || disableFinish}>
          Finish
        </Button>
      </div>
    </div>;
  }
}
