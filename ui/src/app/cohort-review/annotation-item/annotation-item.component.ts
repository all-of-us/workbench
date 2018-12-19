import {
  AfterContentChecked,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnInit,
  Output
} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortReviewService,
  ModifyParticipantCohortAnnotationRequest,
  ParticipantCohortAnnotation,
} from 'generated';
import * as moment from 'moment';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {dateValidator} from '../../cohort-search/validators';

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
  @Input() showDataType: boolean;
  @Output() annotationUpdate: EventEmitter<ParticipantCohortAnnotation> =
    new EventEmitter<ParticipantCohortAnnotation>();
  textSpinnerFlag = false;
  successIcon = false;
  control = new FormControl();
  formattedDate = new FormControl();
  private expandText = false;
  defaultAnnotation = false;
  annotationOption: any;
  oldValue: any;
  dateObj: any;
  dateBtns: any;
  subscription: Subscription;
  error: string;

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
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
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
    if (this.oldValue !== undefined) {
      this.control.setValue(this.oldValue);
      if (this.isDate) {
        this.formattedDate.setValue(moment(this.oldValue).format('YYYY-MM-DD'));
        this.dateObj = new Date(this.formattedDate.value + 'T08:00:00');
      }
    }
    if (this.isDate) {
      this.formattedDate.setValidators(dateValidator());
      this.subscription = this.control.valueChanges.subscribe(val => {
        this.successIcon = false;
        this.formattedDate.setValue(
          moment(val).format('YYYY-MM-DD'),
          {emitEvent: false}
          );
        if (!this.formattedDate.errors) {
          this.textSpinnerFlag = true;
          this.handleInput();
        }
      });
      this.dateBtns = document.getElementsByClassName('datepicker-trigger');
    }
  }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }


  textBlur() {
    this.successIcon = false;
    this.textSpinnerFlag = true;
    this.handleInput();
  }

  integerOnly(event): boolean {
    const charCode = (event.charCode) ? event.charCode : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      return false;
    }
    return true;
  }

  handleInput() {
    /* Parameters from the path */
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const pid = this.annotation.value.participantId;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    const newValue = this.isDate ? this.formattedDate.value : this.control.value;
    const defnId = this.annotation.definition.cohortAnnotationDefinitionId;
    const annoId = this.annotation.value.annotationId ;

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
        apiCall = this.reviewAPI
          .deleteParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId);
      } else {
        const request = <ModifyParticipantCohortAnnotationRequest>{
          [this.valuePropertyName]: newValue,
        };
        apiCall = this.reviewAPI
          .updateParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId, request);
      }
    } else {
      if (newValue) {
        // There's no annotation ID so this must be a create
        const request = <ParticipantCohortAnnotation> {
          cohortAnnotationDefinitionId: defnId,
          ...this.annotation.value,
          [this.valuePropertyName]: newValue,
        };
        apiCall = this.reviewAPI
          .createParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, request);
      }
    }
    if (apiCall) {
      apiCall.subscribe((update) => {
        this.annotationUpdate.emit(update);
        if (update && !annoId) {
          this.annotation.value.annotationId = update.annotationId;
        }
        setTimeout (() => {
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

  toggleExpandText() {
    this.expandText = !this.expandText;
  }

  get valuePropertyName() {
    return {
      [AnnotationType.STRING]:   'annotationValueString',
      [AnnotationType.DATE]:     'annotationValueDate',
      [AnnotationType.ENUM]:     'annotationValueEnum',
      [AnnotationType.BOOLEAN]:  'annotationValueBoolean',
      [AnnotationType.INTEGER]:  'annotationValueInteger'
    }[this.annotation.definition.annotationType];
  }

  get machineName() {
    return this.annotation.definition.columnName.split(' ').join('-');
  }

  get datatypeDisplay() {
    return this.showDataType
      ? ` (${this.annotation.definition.annotationType})`
      : '';
  }

  annotationOptionChange(value) {
    this.successIcon = false;
    this.textSpinnerFlag = true;
    this.annotationOption = value;
    this.defaultAnnotation = true;
    this.control.patchValue(value);
    this.handleInput();

  }

  dateBlur() {
    this.successIcon = false;
    if (!this.formattedDate.errors) {
      this.textSpinnerFlag = true;
      this.dateObj = new Date(this.formattedDate.value + 'T08:00:00');
      this.control.setValue(this.dateObj, {emitEvent: false});
      this.handleInput();
    }
  }

  datepickerPosition() {
    let datepicker;
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
}

