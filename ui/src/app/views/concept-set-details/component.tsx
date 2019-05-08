import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {FadeBox} from 'app/components/containers';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {ConceptTable} from 'app/views/concept-table/component';
import {Concept, ConceptSet, WorkspaceAccessLevel} from 'generated/fetch';

const styles = reactStyles({
  conceptSetHeader: {
    color: '#2F2E7E', fontSize: 20, fontWeight: 600, marginBottom: '1.5rem'
  },
  conceptSetData: {
    display: 'flex', flexDirection: 'row', color: '#000', fontWeight: 600
  }
});

export const ConceptSetDetails = fp.flow(withUrlParams(), withCurrentWorkspace())(
  class extends React.Component<{
    urlParams: any, workspace: WorkspaceData}, {
      conceptSet: ConceptSet, deleting: boolean, editing: boolean, loading: boolean,
      selectedConcepts: Concept[]}> {
    constructor(props) {
      super(props);
      this.state = {
        conceptSet: undefined,
        deleting: false,
        editing: false,
        loading: true,
        selectedConcepts: []
      };
    }

    componentDidMount() {
      this.getConceptSet();
    }

    async getConceptSet() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      try {
        const resp = await conceptSetsApi().getConceptSet(ns, wsid, csid);
        this.setState({conceptSet: resp, loading: false});
      } catch (error) {
        console.log(error);
        // todo: cannot find concept set
      }
    }

    onSelectConcepts() {
      // TODO
    }

    onDeleteConcept() {
      // TODO
    }

    get canEdit(): boolean {
      return this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel === WorkspaceAccessLevel.OWNER
          || this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel === WorkspaceAccessLevel.WRITER;
    }

    render() {
      const {conceptSet, loading, selectedConcepts} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {loading ? <SpinnerOverlay/> :
        <div style={{display: 'flex', flexDirection: 'column'}}>
          <div style={{display: 'flex', flexDirection: 'row', paddingBottom: '1.5rem'}}>
            <div style={{flexDirection: 'column', alignItems: 'space-between'}}>
              <div style={styles.conceptSetHeader}>{conceptSet.name}</div>
              <div style={styles.conceptSetData}>
                <div>Participant Count: {conceptSet.participantCount}</div>
                <div style={{marginLeft: '2rem'}}>
                  Domain: {fp.capitalize(conceptSet.domain.toString())}</div>
              </div>
            </div>
          </div>
          <ConceptTable concepts={conceptSet.concepts} loading={loading}
                        reactKey={conceptSet.domain.toString()}
                        onSelectConcepts={() => this.onSelectConcepts()}
                        placeholderValue={'No Concepts Found'}
                        selectedConcepts={selectedConcepts}/>
        </div>}
      </FadeBox>;
    }
  });


@Component({
  template: '<div #root></div>'
})
export class ConceptSetDetailsComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptSetDetails, []);
  }
}


// @Component({
//   styleUrls: ['../../styles/buttons.css',
//     '../../styles/cards.css',
//     '../../styles/headers.css',
//     '../../styles/inputs.css',
//     '../../styles/errors.css',
//     './component.css'],
//   templateUrl: './component.html',
// })
// export class ConceptSetDetailsComponent implements OnInit, OnDestroy {
//   @ViewChild(ConceptTableComponent) conceptTable;
//
//   wsNamespace: string;
//   wsId: string;
//   accessLevel: WorkspaceAccessLevel;
//   conceptSet: ConceptSet;
//
//   editing = false;
//   editSubmitting = false;
//   editName: string;
//   editDescription: string;
//
//   removing = false;
//   removeSubmitting = false;
//   confirmDeleting = false;
//   selectedConcepts: Concept[] = [];
//
//   constructor(
//     private conceptSetsService: ConceptSetsService,
//   ) {
//     this.receiveDelete = this.receiveDelete.bind(this);
//     this.closeConfirmDelete = this.closeConfirmDelete.bind(this);
//     this.removeFab = this.removeFab.bind(this);
//     this.onSelectConcepts = this.onSelectConcepts.bind(this);
//   }
//
//   ngOnInit() {
//     const {ns, wsid, csid} = urlParamsStore.getValue();
//     this.wsNamespace = ns;
//     this.wsId = wsid;
//     const {accessLevel} = currentWorkspaceStore.getValue();
//     this.accessLevel = accessLevel;
//     this.conceptSetsService.getConceptSet(ns, wsid, csid).subscribe(conceptSet => {
//       currentConceptSetStore.next(conceptSet as unknown as FetchConceptSet);
//       this.conceptSet = conceptSet;
//       this.editName = conceptSet.name;
//       this.editDescription = conceptSet.description;
//     });
//   }
//
//   ngOnDestroy() {
//     currentConceptSetStore.next(undefined);
//   }
//
//   validateEdits(): boolean {
//     return !!this.editName;
//   }
//
//   removeFab(): void {
//     this.removing = true;
//   }
//
//   submitEdits() {
//     if (!this.validateEdits() || this.editSubmitting) {
//       return;
//     }
//     this.editSubmitting = true;
//     this.conceptSetsService.updateConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id, {
//       ...this.conceptSet,
//       name: this.editName,
//       description: this.editDescription
//     }).subscribe((updated) => {
//       this.conceptSet = updated;
//       this.editSubmitting = false;
//       this.editing = false;
//     }, () => {
//       // TODO(calbach): Handle errors.
//       this.editSubmitting = false;
//     });
//   }
//
//   closeConfirmDelete(): void {
//     this.confirmDeleting = false;
//   }
//
//   receiveDelete() {
//     this.conceptSetsService.deleteConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id)
//       .subscribe(() => {
//         navigate(['workspaces', this.wsNamespace, this.wsId, 'concepts', 'sets']);
//         this.closeConfirmDelete();
//       });
//   }
//
//   removeConcepts() {
//     this.removeSubmitting = true;
//     this.conceptSetsService.updateConceptSetConcepts(
//       this.wsNamespace, this.wsId, this.conceptSet.id, {
//         etag: this.conceptSet.etag,
//         removedIds: this.conceptTable.selectedConcepts.map(c => c.conceptId)
//       }).subscribe((cs) => {
//         this.conceptSet = cs;
//         this.removing = false;
//         this.removeSubmitting = false;
//       }, () => {
//         // TODO(calbach): Handle errors.
//         this.removeSubmitting = false;
//       });
//   }
//
//   onSelectConcepts(concepts) {
//     this.selectedConcepts = concepts;
//   }
//
//   get canEdit(): boolean {
//     return this.accessLevel === WorkspaceAccessLevel.OWNER
//         || this.accessLevel === WorkspaceAccessLevel.WRITER;
//   }
//
//   get selectedConceptsCount(): number {
//     return !this.conceptTable ? 0 : this.conceptTable.selectedConcepts.length;
//   }
//
//   get disableRemoveFab(): boolean {
//     return !this.canEdit || this.selectedConceptsCount === 0;
//   }
// }
