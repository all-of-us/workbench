import {Component, EventEmitter, Input, Output} from '@angular/core';
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
      <Modal style={{borderRadius: '8px'}}>
        <ModalTitle>Are you sure you want to
          delete {this.props.resourceType}: {this.props.resource.name}?
        </ModalTitle>

        <ModalBody>This will permanently delete the {this.props.resourceType}.</ModalBody>
        <ModalFooter style={{paddingTop: '1rem'}}>
          <Button type='secondary' onClick={() => this.props.closeFunction()}>Cancel</Button>
          <Button disabled={this.state.loading}
                  style={{paddingLeft: '0.77rem'}}
                  onClick={() => this.emitDelete(this.props.resource)}>Delete {this.props.resourceType}</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;

    {/*<div class="modal-title">*/}
        {/*<!--The ngIf is to avoid null pointers before the modal is opened.-->*/}
    {/*<div *ngIf="deleting" class="modal-title-text">Are you sure you want to delete {{resourceType | titlecase}}: {{resource.name}}?</div>*/}
    {/*<div class="modal-title-subtext">This will permanently delete the {{resourceType}}.</div>*/}
    {/*</div>*/}
    {/*<div class="modal-footer">*/}
    {/*<button type="button" class="btn btn-outline" (click)="deleting=false">Cancel</button>*/}
    {/*<button type="button" class="btn btn-primary confirm-delete-btn" (click)="emitDelete(resource)" [clrLoading]="loading" [disabled]="loading">Delete {{resourceType}}</button>*/}
    {/*</div>*/}
  }
}

@Component({
  selector: 'app-confirm-delete-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class ConfirmDeleteModalComponent {
  @Input() resourceType: string;
  @Output() receiveDelete = new EventEmitter<any>();
  @Input() resource: {name: string};

  @Input() deleting: boolean;
  @Input() closeFunction: Function;

  componentId = 'confirm-delete-modal';

  // open(): void {
  //   this.deleting = true;
  //   this.loading = false;
  // }
  //
  // close(): void {
  //   this.deleting = false;
  // }
  //
  // emitDelete(resource: any): void {
  //   if (!this.loading) {
  //     this.loading = true;
  //     this.receiveDelete.emit(resource);
  //   }
  // }

  ngOnInit(): void {
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
