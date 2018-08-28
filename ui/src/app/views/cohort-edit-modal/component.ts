import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

import {Cohort, CohortsService, Workspace} from 'generated';

@Component({
  selector: 'app-cohort-edit-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css'],
  templateUrl: './component.html'
})
export class CohortEditModalComponent {
  /* Properties */
  loading = false;
  form: FormGroup;
  @Input() cohort: any;
  @Output() updateFinished = new EventEmitter<any>();
  editing = false;

  /* Convenience property aliases */
  get name(): FormControl         { return this.form.get('name') as FormControl; }
  get description(): FormControl  { return this.form.get('description') as FormControl; }
  get workspace(): Workspace      { return this.route.snapshot.data.workspace; }

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private cohortService: CohortsService,
  ) {
    this.form = fb.group({
      name: ['', Validators.required],
      description: '',
    });
  }

  open(): void {
    this.editing = true;
    this.loading = false;
    this.form.setValue({name: this.cohort.name, description: this.cohort.description});
  }

  close(): void {
    this.editing = false;
  }

  save(): void {
    this.loading = true;

    const newCohort = <Cohort>{
      ...this.cohort,
      name: this.name.value,
      description: this.description.value,
    };

    const call = this.cohortService.updateCohort(
      this.workspace.namespace,
      this.workspace.id,
      newCohort.id,
      newCohort
    );

    call.do(_ => this.loading = false)
      .subscribe(_ => this.updateFinished.emit());
  }

  get canSave(): boolean {
    if (this.editing) {
      const nameHasChanged = this.name.value !== this.cohort.name;
      const descHasChanged = this.description.value !== this.cohort.description;
      return this.form.valid && (nameHasChanged || descHasChanged) && !this.loading;
    } else {
      return false;
    }
  }
}
