import {Component, Input} from '@angular/core';

import {MultiSelectFilterComponent} from '../multiselect-filter/multiselect-filter.component';
import {Participant} from '../participant.model';

import {CohortStatus, ParticipantCohortStatusColumns} from 'generated';

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
