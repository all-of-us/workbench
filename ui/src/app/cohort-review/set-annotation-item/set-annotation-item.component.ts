import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnInit,
    Output,
    ViewChild,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import {FormControl, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {
    CohortAnnotationDefinition,
    CohortAnnotationDefinitionService,
    ModifyCohortAnnotationDefinitionRequest,
} from 'generated';

@Component({
    selector: 'app-set-annotation-item',
    templateUrl: './set-annotation-item.component.html',
    styleUrls: ['./set-annotation-item.component.css']
})
export class SetAnnotationItemComponent implements OnChanges{
    @Input() definition: CohortAnnotationDefinition;
    @Output() isPosting = new EventEmitter<boolean>();
    @Input() cancelEvent: false;
    @Input() saveEvent: false;
    editing = false;
    name = new FormControl('', Validators.required);

    @ViewChild('nameInput') nameInput;

    constructor(
        private route: ActivatedRoute,
        private annotationAPI: CohortAnnotationDefinitionService,
        private state: ReviewStateService,
        private ngZone: NgZone,
    ) {}

    ngOnChanges(changes: SimpleChanges){
        // if(this.cancelEvent){
        //     this.cancelEdit();
        // }

        if (this.saveEvent) {
            this.saveEdit();
        }
    }
    edit(): void {
        this.editing = true;
        this.name.setValue(this.definition.columnName);
        this.setFocus();
    }

    setFocus() {
        // tslint:disable
        /* For all the reasons behind this TOTALLY OBVIOUS solution to the
         * extremely complex problem of focusing an input, please see the
         * following:
         * https://stackoverflow.com/questions/41190075/how-do-i-programmatically-set-focus-to-dynamically-created-formcontrol-in-angula?rq=1
         * https://stackoverflow.com/questions/34502768/why-angular2-template-local-variables-are-not-usable-in-templates-when-using-ng?rq=1
         * https://github.com/angular/angular/issues/6179
         */
        // tslint:enable
        this.ngZone.runOutsideAngular(() => {
            setTimeout(() => this.nameInput.nativeElement.focus(), 0);
        });
    }

    saveEdit(): void {
        const columnName = this.name.value.trim();
        const oldColumnName = this.definition.columnName.trim();

        if (this.name.invalid || (columnName === oldColumnName)) {
         //   this.cancelEdit();
            return ;
        }

        const request = <ModifyCohortAnnotationDefinitionRequest>{columnName};
        const {ns, wsid, cid} = this.route.snapshot.params;
        const id = this.definition.cohortAnnotationDefinitionId;
        this.isPosting.emit(true);

        this.annotationAPI
            .updateCohortAnnotationDefinition(ns, wsid, cid, id, request)
            .switchMap(_ => this.annotationAPI
                .getCohortAnnotationDefinitions(ns, wsid, cid)
                .pluck('items'))
            .do((defns: CohortAnnotationDefinition[]) =>
                this.state.annotationDefinitions.next(defns))
            .subscribe(_ => {
                this.editing = false;
                this.isPosting.emit(false);
            });
    }

    cancelEdit(event?) {
       // const columnName = this.name.value.trim();
       // const oldColumnName = this.definition.columnName.trim();
       // console.log(columnName);
      //  console.log(oldColumnName)
        this.name.setValue(this.definition.columnName);
        this.editing = false;
        if (event) {
            event.stopPropagation();
        }
    }
    // cancelEdit(event?) {
    //       const columnName = this.name.value.trim();
    //       const oldColumnName = this.definition.columnName.trim();
    //      console.log(oldColumnName);
    //      console.log(columnName);
    //     // // if (this.name.invalid || (columnName === oldColumnName)) {
    //
    //         if(this.cancelEvent){
    //             console.log(this.cancelEvent);
    //             this.name.setValue(oldColumnName);
    //             console.log(oldColumnName);
    //         }
    //         this.editing = false;
    //         if (event) {
    //             event.stopPropagation();
    //         }
    //        // return ;
    //    // }
    //   // if(this.cancelEvent){
    //
    //   // } else {
    //   //   console.log('checking');
    //   // }
    //
    // }
    delete(): void {
        const {ns, wsid, cid} = this.route.snapshot.params;
        const id = this.definition.cohortAnnotationDefinitionId;
        this.isPosting.emit(true);

        this.annotationAPI
            .deleteCohortAnnotationDefinition(ns, wsid, cid, id)
            .switchMap(_ => this.annotationAPI
                .getCohortAnnotationDefinitions(ns, wsid, cid)
                .pluck('items'))
            .do((defns: CohortAnnotationDefinition[]) =>
                this.state.annotationDefinitions.next(defns))
            .subscribe(_ => this.isPosting.emit(false));
    }
}
