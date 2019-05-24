import {
  Button
} from 'app/components/buttons';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalInput,
  ModalTitle,
} from 'app/components/modals';

import {reactStyles} from 'app/utils';

import {
  RecentResource,
} from 'generated/fetch';

import * as React from 'react';

const styles = reactStyles({
  fieldHeader: {
    fontSize: 14,
    color: '#262262',
    fontWeight: 600,
    display: 'block'
  },
  field: {
    fontWeight: 400,
    width: '100%'
  }
});

export interface EditModalState {
  loading: boolean;
  resource: RecentResource;
  resourceType: string;
  resourceName: string;
  resourceDescription: string;
}

export interface EditModalProps {
  resource: RecentResource;
  onEdit: Function;
  onCancel: Function;
}

export class EditModal extends React.Component<EditModalProps, EditModalState> {

  constructor(props: EditModalProps) {
    super(props);
    this.state = {
      resource: props.resource,
      resourceType: props.resource.cohort ? 'Cohort' : 'Concept Set',
      loading: false,
      resourceName: props.resource.cohort ?
          props.resource.cohort.name : props.resource.conceptSet.name,
      resourceDescription: props.resource.cohort ?
          props.resource.cohort.description : props.resource.conceptSet.description
    };

  }

  save(): void {
    this.setState({loading: true});
    if (this.props.resource.cohort) {
      this.state.resource.cohort = {
        ...this.props.resource.cohort,
        name: this.state.resourceName,
        description: this.state.resourceDescription
      };
    } else if (this.props.resource.conceptSet) {
      this.state.resource.conceptSet = {
        ...this.props.resource.conceptSet,
        name: this.state.resourceName,
        description: this.state.resourceDescription
      };
    }

    this.props.onEdit(this.state.resource);
  }

  render() {
    return <React.Fragment>
      <Modal loading={this.state.loading} className='editModal'>
        <ModalTitle style={{fontSize: 16}}>
          Edit {this.state.resourceType} Information
        </ModalTitle>
        <ModalBody>
          <div style={{marginTop: '1rem'}}>
            <label className='required'
                   style={styles.fieldHeader}>{this.state.resourceType} Name: </label>
            <ModalInput value={this.state.resourceName}
                        data-test-id='edit-name'
                        onChange={(e) => this.setState({resourceName: e.target.value})}/>
          </div>
          <div style={{marginTop: '1rem'}}>
            <label style={styles.fieldHeader}>Description: </label>
            <textarea value={this.state.resourceDescription || ''}
                      onChange={(e) => this.setState({resourceDescription: e.target.value})}/>
          </div>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.props.onCancel()}>Cancel</Button>
          <Button disabled={!(this.state.resourceName.length > 0) || this.state.loading}
                  data-test-id='save-edit'
                  style={{marginLeft: '.5rem'}}
                  onClick={() => this.save()}>Save</Button>
        </ModalFooter>
      </Modal>
    </React.Fragment>;
  }
}
