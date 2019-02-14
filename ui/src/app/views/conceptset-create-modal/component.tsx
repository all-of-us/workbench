import {Component, EventEmitter, Input, Output} from '@angular/core';
import {urlParamsStore} from 'app/utils/navigation';
import {ConceptSet, ConceptsService, CreateConceptSetRequest, DomainInfo} from 'generated';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {Domain} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';
import {ReactWrapperBase, summarizeErrors} from "../../utils/index";
import {Modal, ModalFooter, ModalTitle, ModalTitle, ModalBody} from "../../components/modals";
import {Button} from "app/components/buttons";
import {TextInput, ValidationError} from "app/components/inputs";
import * as fp from 'lodash/fp';

// @Component({
//   selector: 'app-create-concept-modal',
//   styleUrls: [
//     '../../styles/buttons.css',
//     '../../styles/inputs.css',
//     '../../styles/errors.css',
//     './component.css'
//   ],
//   templateUrl: './component.html',
// })
// export class CreateConceptSetModalComponent {
//   @Output() onUpdate: EventEmitter<void> = new EventEmitter();
//   public modalOpen  = false;
//   wsNamespace: string;
//   wsId: string;
//   name: string;
//   description: string;
//   domain: Domain;
//   conceptDomainList: Array<DomainInfo> = [];
//   required = false;
//   alreadyExist = false;
//
//   constructor(private conceptsService: ConceptsService,
//     private conceptSetService: ConceptSetsService,
//   ) {
//     const {ns, wsid} = urlParamsStore.getValue();
//     this.wsNamespace = ns;
//     this.wsId = wsid;
//   }
//
//   open(): void {
//     this.required = false;
//     this.alreadyExist = false;
//     this.reset();
//     this.conceptsService.getDomainInfo(this.wsNamespace, this.wsId).subscribe((response) => {
//       this.conceptDomainList = response.items;
//       this.domain = this.conceptDomainList[0].domain;
//     });
//     this.modalOpen = true;
//   }
//
//   close(): void {
//     this.modalOpen = false;
//   }
//
//   reset(): void {
//     this.name = '';
//     this.description = '';
//   }
//
//   saveConcept(): void {
//     this.required = false;
//     this.alreadyExist = false;
//
//     if (!this.name) {
//       this.required = true;
//       return;
//     }
//     const concept: ConceptSet = {
//       name: this.name,
//       description: this.description,
//       domain: this.domain
//     };
//     const request: CreateConceptSetRequest = {
//       conceptSet: concept
//     };
//     this.conceptSetService.createConceptSet(this.wsNamespace, this.wsId, request)
//       .subscribe(() => {
//         this.modalOpen = false;
//         this.onUpdate.emit();
//       });
//   }
// }

class CreateConceptSetModal extends React.Component<{
  onCreate: Function,
  onClose: Function,
  conceptDomainList: Array<DomainInfo>,
  existingConceptSets: ConceptSet[]
}, {
  name: string,
  description: string,
  domain: Domain,
  nameTouched: boolean
}> {

  constructor(props) {
    super(props)
    this.state = {
      name: '',
      description: '',
      domain: props.domainList[0],
      nameTouched: false,
    };
  }

  render() {
    const {onCreate, onClose, domainList, existingConceptSets} = this.props;
    const {name, description, domain, nameTouched} = this.state;
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: existingConceptSets.map((concept: ConceptSet) => {concept.name}),
          message: 'already exists'
        }
      }
    });
    return <Modal>
      <ModalTitle>Create a Concept Set</ModalTitle>
      <ModalBody>
        <TextInput placeholder="Name" value={name} onChange={(v) => {this.setState({name: v, nameTouched: true})}}/>
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.name)}
        </ValidationError>
        <TextInput placeholder="Description" value={description} onChange={(v) => {this.setState({description: v})}}/>
        <select value={domain} onChange={(e) => {this.setState({domain: e.target.value})}}>
          {domainList.map(concept => <option value={concept.domain}>
            {concept.domain}
          </option>)}
        </select>
        <ModalFooter>
          <Button type="secondary" onClick={onClose}>Cancel</Button>
          <Button onClick="saveConcept()">Save</Button>
        </ModalFooter>
      </ModalBody>
    </Modal>;
  }
}

@Component({
  selector: 'app-create-concept-modal',
  template: '<div #root></div>',
})
export class CreateConceptSetModalComponent extends ReactWrapperBase {
  @Input('onCreate') onCreate: Function;
  @Input('onClose') onClose: Function;
  @Input('conceptDomainList') conceptDomainList: Array<DomainInfo> = [];
  @Input('existingConceptSets') existingConceptSets: ConceptSet[];
  constructor() {
    super(CreateConceptSetModal,
      ['onCreate', 'onClose', 'conceptDomainList', 'existingConceptSets']);
  }
}