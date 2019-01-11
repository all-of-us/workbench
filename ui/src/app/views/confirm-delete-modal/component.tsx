import {
  Component, DoCheck, ElementRef, EventEmitter, Input, OnInit, Output,
  ViewChild
} from '@angular/core';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

import {
  Button
} from 'app/components/buttons';

class CurrId {
  static currId = 0;
  static getUniqueId = () => {
    return CurrId.currId += 1;
  }
}

interface ConfirmDeleteModalProps {
  deleting: boolean,
  closeFunction: Function,
  resourceType: string,
  receiveDelete: EventEmitter<any>,
  resource: {name: string}
}

interface ConfirmDeleteModalState {
  loading: boolean
}

export class ConfirmDeleteModal extends React.Component<ConfirmDeleteModalProps, ConfirmDeleteModalState> {
  state: ConfirmDeleteModalState;
  props: ConfirmDeleteModalProps;

  constructor(props: ConfirmDeleteModalProps) {
    super(props);
    this.state = {loading: false};
  }

  emitDelete(resource: any): void {
    if (!this.state.loading) {
      this.setState({loading: true});
      this.props.receiveDelete.emit(resource);
    }
  }

  render() {
    return <React.Fragment>
      {this.props.deleting &&
      <Modal>
        <ModalTitle style={{lineHeight: '28px'}}>Are you sure you want to
          delete {this.props.resourceType}: {this.props.resource.name}?
        </ModalTitle>
        <ModalBody style={{marginTop: '0.2rem', lineHeight: '28.px'}}>
          This will permanently delete the {this.props.resourceType}.
        </ModalBody>
        <ModalFooter style={{paddingTop: '1rem'}}>
          <Button type='secondary'
                  onClick={() => this.props.closeFunction()}>Cancel</Button>
          <Button disabled={this.state.loading}
                  style={{marginLeft: '0.5rem'}}
                  onClick={() => this.emitDelete(this.props.resource)}>
            Delete {this.props.resourceType}
          </Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-confirm-delete-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class ConfirmDeleteModalComponent implements DoCheck, OnInit {
  @Input() resourceType: string;
  @Output() receiveDelete = new EventEmitter<any>();
  @Input() resource: {name: string};
  @Input() deleting: boolean;
  @Input() closeFunction: Function;

  @ViewChild('defaultId') templateValue: ElementRef;
  componentId = 'confirm-delete-modal-' + CurrId.getUniqueId();

  constructor() {}

  ngOnInit(): void {
    console.log(this.templateValue);
    this.templateValue.nativeElement.id = this.componentId;
    ReactDOM.render(React.createElement(ConfirmDeleteModal,
        {deleting: this.deleting, closeFunction: this.closeFunction, resourceType: this.resourceType,
          receiveDelete: this.receiveDelete, resource: this.resource}),
        document.getElementById(this.componentId));
  }

  ngDoCheck(): void {
    ReactDOM.render(React.createElement(ConfirmDeleteModal,
        {deleting: this.deleting, closeFunction: this.closeFunction, resourceType: this.resourceType,
          receiveDelete: this.receiveDelete, resource: this.resource}),
        document.getElementById(this.componentId));
  }
}
