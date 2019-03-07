import {
  AfterContentChecked,
  ChangeDetectorRef,
  Component,
  HostListener,
  Input,
  OnChanges,
  OnInit
} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {dateValidator} from 'app/cohort-search/validators';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {
  AnnotationType,
  CohortAnnotationDefinition,
  ModifyParticipantCohortAnnotationRequest,
  ParticipantCohortAnnotation,
} from 'generated/fetch';
import * as moment from 'moment';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}


@Component({
  selector: 'app-annotation-item',
  templateUrl: './annotation-item.component.html',
  styleUrls: ['./annotation-item.component.css']
})
export class AnnotationItemComponent implements OnInit, OnChanges, AfterContentChecked {
  readonly kinds = AnnotationType;

  @Input() annotation: Annotation;
  @Input() onChange: Function;
  textSpinnerFlag = false;
  successIcon = false;
  form = new FormGroup({
    annotation: new FormControl(),
    formattedDate: new FormControl()
  });
  defaultAnnotation = false;
  annotationOption: any;
  oldValue: any;
  dateObj: any;
  dateBtns: any;
  subscription: Subscription;

  // if calendar icon is clicked, adjust position of datepicker
  @HostListener('document:mouseup', ['$event.target'])
  onClick(targetElement) {
    if (this.isDate) {
      const length = this.dateBtns.length;
      for (let i = 0; i < length; i++) {
        const dateBtn = <HTMLElement>this.dateBtns[i];
        if (dateBtn.contains(targetElement)) {
          this.datepickerPosition();
          break;
        }
      }
    }
  }

  constructor(
    private cdref: ChangeDetectorRef
  ) {}

  ngOnChanges() {
    if (this.annotation.value[this.valuePropertyName]) {
      this.defaultAnnotation = true;
      this.annotationOption = this.annotation.value[this.valuePropertyName];
    } else {
      this.defaultAnnotation = false;
    }
  }

  ngOnInit() {
    this.ngAfterContentChecked();
    this.successIcon = false;
    this.oldValue = this.annotation.value[this.valuePropertyName];
    this.subscription = this.form.controls.annotation.valueChanges.subscribe(val => {
      if (this.isInt) {
        if (val && val.toString().length > 9) {
          const value = parseFloat(val.toString().slice(0, 9));
          this.form.controls.annotation.setValue(value, {emitEvent: false});
        }
      }
      if (this.isDate) {
        this.successIcon = false;
        this.form.controls.formattedDate.setValue(
          moment(val).format('YYYY-MM-DD'),
          {emitEvent: false}
        );
        if (!this.form.controls.formattedDate.errors) {
          this.textSpinnerFlag = true;
          this.handleInput();
        }
      }
    });
    if (this.isDate) {
      this.form.controls.formattedDate.setValidators(dateValidator());
      this.dateBtns = document.getElementsByClassName('datepicker-trigger');
    }
    if (this.isInt) {
      this.form.controls.annotation.setValidators([Validators.max(999999999)]);
    }
    if (this.isText) {
      this.form.controls.annotation.setValidators([Validators.maxLength(4000)]);
    }
    if (this.oldValue !== undefined) {
      this.form.controls.annotation.setValue(this.oldValue);
      if (this.isDate) {
        this.form.controls.formattedDate.setValue(moment(this.oldValue).format('YYYY-MM-DD'));
        this.dateObj = new Date(this.form.controls.formattedDate.value + 'T08:00:00');
      }
    }
  }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }


  textBlur() {
    this.successIcon = false;
    if (!this.form.controls.annotation.errors) {
      this.textSpinnerFlag = true;
      this.handleInput();
    }
  }

  handleInput() {
    /* Parameters from the path */
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const pid = this.annotation.value.participantId;
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const newValue = this.isDate
      ? this.form.controls.formattedDate.value : this.form.controls.annotation.value;
    const defnId = this.annotation.definition.cohortAnnotationDefinitionId;
    const annoId = this.annotation.value.annotationId;

    let apiCall;

    // Nothing to see here - if there's no change, no need to hit the server
    if (newValue === this.oldValue) {
      this.textSpinnerFlag = false;
      return;
    }
    this.oldValue = newValue;
    // If there is an annotation ID then the annotation has already been
    // created, so this must be either delete or update
    if (annoId !== undefined) {
      // If the new value isn't anything, this is a delete
      if (newValue === '' || newValue === null) {
        apiCall = cohortReviewApi()
          .deleteParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId);
      } else {
        const request = <ModifyParticipantCohortAnnotationRequest>{
          [this.valuePropertyName]: newValue,
        };
        apiCall = cohortReviewApi()
          .updateParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId, request);
      }
    } else {
      if (newValue) {
        // There's no annotation ID so this must be a create
        const request = <ParticipantCohortAnnotation>{
          cohortAnnotationDefinitionId: defnId,
          ...this.annotation.value,
          [this.valuePropertyName]: newValue,
        };
        apiCall = cohortReviewApi()
          .createParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, request);
      }
    }
    if (apiCall) {
      apiCall.then((update) => {
        this.onChange(defnId, update);
        setTimeout(() => {
          this.textSpinnerFlag = false;
          this.successIcon = true;
          setTimeout(() => {
            this.successIcon = false;
          }, 2000);
        }, 1000);
      });
    } else {
      this.textSpinnerFlag = false;
    }
  }

  get valuePropertyName() {
    return {
      [AnnotationType.STRING]: 'annotationValueString',
      [AnnotationType.DATE]: 'annotationValueDate',
      [AnnotationType.ENUM]: 'annotationValueEnum',
      [AnnotationType.BOOLEAN]: 'annotationValueBoolean',
      [AnnotationType.INTEGER]: 'annotationValueInteger'
    }[this.annotation.definition.annotationType];
  }

  get machineName() {
    return this.annotation.definition.columnName.split(' ').join('-');
  }

  annotationOptionChange(value) {
    this.successIcon = false;
    this.textSpinnerFlag = true;
    this.annotationOption = value;
    this.defaultAnnotation = true;
    this.form.controls.annotation.patchValue(value);
    this.handleInput();

  }

  dateBlur() {
    this.successIcon = false;
    if (!this.form.controls.formattedDate.errors) {
      this.textSpinnerFlag = true;
      this.dateObj = new Date(this.form.controls.formattedDate.value + 'T08:00:00');
      this.form.controls.annotation.setValue(this.dateObj, {emitEvent: false});
      this.handleInput();
    }
  }

  datepickerPosition() {
    let datepicker: any;
    Observable.interval()
      .takeWhile((val, index) => !datepicker && index < 1000)
      .subscribe(() => {
        datepicker = <HTMLElement>document.getElementsByClassName('datepicker')[0];
        if (datepicker) {
          datepicker.style.left = '-9rem';
        }
      });
  }

  get isDate() {
    return this.annotation.definition.annotationType === AnnotationType.DATE;
  }

  get isInt() {
    return this.annotation.definition.annotationType === AnnotationType.INTEGER;
  }

  get isText() {
    return this.annotation.definition.annotationType === AnnotationType.STRING;
  }
}
