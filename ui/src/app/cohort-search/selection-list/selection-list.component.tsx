import {Component, Input} from '@angular/core';
import * as React from 'react';

import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {ReactWrapperBase} from 'app/utils';
import {DomainType} from 'generated/fetch';

interface SelectionInfoProps {
  index: number;
  selection: any;
  removeSelection: Function;
}

export class SelectionInfo extends React.Component<SelectionInfoProps> {
  constructor(props: SelectionInfoProps) {
    super(props);
  }

  get showType() {
    return ![DomainType.PHYSICALMEASUREMENT, DomainType.DRUG, DomainType.SURVEY].includes(this.props.selection.domainId);
  }
  get showOr() {
    const {index, selection} = this.props;
    return index > 0 && selection.domainId !== DomainType.PERSON;
  }

  render() {
    const {selection, removeSelection} = this.props;
    return <div className='container'>
      <button type='button' style={{margin: 0, padding: '0 0.25rem'}}
        className='btn btn-icon btn-link btn-sm text-danger'
        onClick={() => removeSelection()}>
        <ClrIcon shape='times-circle'/>
      </button>
      <span className='item-info'>
        {this.showOr && <small className='selection-or font-weight-bold'>OR</small>}
        {!!selection.group && <small className='text-muted'>Group</small>}
        <small className='name'>
          {this.showType && <strong>{typeDisplay(selection)}</strong>}
          <span className='text-size text-muted'>{nameDisplay(selection)}</span>
          <span className='text-muted'>{attributeDisplay(selection)}</span>
        </small>
      </span>
    </div>;
  }
}

interface Props {
  back: Function;
  cancel: Function;
  domain: DomainType;
  errors: Array<string>;
  finish: Function;
  removeSelection: Function;
  selections: Array<any>;
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
    const {back, cancel, errors, finish, removeSelection, selections, setView} = this.props;
    return <div className='panel-right-container'>
      <h5 className='selection-title'>Selected Criteria</h5>
      <div className='panel-right selected'>
        {selections.map((selection, s) =>
          <SelectionInfo index={s} selection={selection} removeSelection={() => removeSelection(selection)}/>
        )}
      </div>
      <div className='footer'>
        <button type='button' onClick={() => cancel()} className='btn btn-link'>
          Cancel
        </button>
        {this.showNext && <button type='button'
          disabled={selections.length === 0}
          onClick={() => setView('modifiers')}
          className='btn btn-primary'>
          Next
        </button>}
        {this.showBack && <button type='button'
          onClick={back()}
          className='btn btn-primary'>
          Back
        </button>}
        <button type='button'
          disabled={errors.length > 0}
          onClick={finish()}
          className='btn btn-primary'>
          Finish
        </button>
      </div>
    </div>;
  }
}

@Component({
  selector: 'crit-selection-list',
  template: '<div #root></div>'
})
export class SelectionListComponent extends ReactWrapperBase {
  @Input('back') back: Props['back'];
  @Input('cancel') cancel: Props['cancel'];
  @Input('domain') domain: Props['domain'];
  @Input('errors') errors: Props['errors'];
  @Input('finish') finish: Props['finish'];
  @Input('removeSelection') removeSelection: Props['removeSelection'];
  @Input('selections') selections: Props['selections'];
  @Input('setView') setView: Props['setView'];
  @Input('view') view: Props['view'];
  constructor() {
    super(SelectionList, ['back', 'cancel', 'domain', 'errors', 'finish', 'removeSelection', 'selections', 'setView', 'view']);
  }
}
