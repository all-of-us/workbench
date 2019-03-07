import {Component, Input} from '@angular/core';

import {MultiSelectFilterComponent} from 'app/cohort-review/multiselect-filter/multiselect-filter.component';
import {Participant} from 'app/cohort-review/participant.model';

import {CohortStatus, ParticipantCohortStatusColumns} from 'generated/fetch';

@Component({
  selector: 'app-status-filter',
  templateUrl: './status-filter.component.html',
})
export class StatusFilterComponent extends MultiSelectFilterComponent {
  @Input() property = ParticipantCohortStatusColumns.STATUS;
  @Input() options = [
    CohortStatus.INCLUDED,
    CohortStatus.EXCLUDED,
    CohortStatus.NEEDSFURTHERREVIEW,
    CohortStatus.NOTREVIEWED,
  ];
  CohortStatus = CohortStatus;
  formatStatus = Participant.formatStatusForText;

}
