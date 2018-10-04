import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {ConceptTableComponent} from 'app/views/concept-table/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';

import {
  ConceptSet,
  ConceptSetsService,
  Domain,
  DomainInfo,
  StandardConceptFilter,
  WorkspaceAccessLevel,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptSetDetailsComponent {
  @ViewChild(ConfirmDeleteModalComponent) deleteModal;

  wsNamespace: string;
  wsId: string;
  accessLevel: WorkspaceAccessLevel;
  conceptSet: ConceptSet;

  editing = false;
  editHover = false;
  editSubmitting = false;
  editName: string;
  editDescription: string;

  constructor(
    private conceptSetsService: ConceptSetsService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.accessLevel = this.route.snapshot.data.workspace.accessLevel;
    this.conceptSet = this.route.snapshot.data.conceptSet;
    this.editName = this.conceptSet.name;
    this.editDescription = this.conceptSet.description;
  }

  validateEdits(): boolean {
    return !!this.editName;
  }

  submitEdits() {
    if (!this.validateEdits() || this.editSubmitting) {
      return;
    }
    this.editSubmitting = true;
    this.conceptSetsService.updateConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id, {
      ...this.conceptSet,
      name: this.editName,
      description: this.editDescription
    }).subscribe((updated) => {
      this.conceptSet = updated;
      this.editSubmitting = false;
      this.editing = false;
    }, () => {
      // TODO(calbach): Handle errors.
      this.editSubmitting = false;
    });
  }

  receiveDelete() {
    this.conceptSetsService.deleteConceptSet(this.wsNamespace, this.wsId, this.conceptSet.id)
      .subscribe(() => {
        this.router.navigate(['workspaces', this.wsNamespace, this.wsId, 'concepts']);
        this.deleteModal.close();
      });
  }

  openRemoveModal() {
    // TODO(calbach): Implement.
  }

  get canEdit(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
