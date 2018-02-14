import {Component, Input} from '@angular/core';

import {ChoiceFilterComponent} from '../choice-filter/choice-filter.component';
import {Participant} from '../participant.model';

import {CohortStatus, ParticipantCohortStatusColumns} from 'generated';

@Component({
  selector: 'app-status-filter',
  templateUrl: './status-filter.component.html',
})
export class StatusFilterComponent extends ChoiceFilterComponent {
  @Input() property = ParticipantCohortStatusColumns.Status;
  @Input() options = [
    CohortStatus.INCLUDED,
    CohortStatus.EXCLUDED,
    CohortStatus.NEEDSFURTHERREVIEW,
    CohortStatus.NOTREVIEWED,
  ];
  CohortStatus = CohortStatus;
  formatStatus = Participant.formatStatusForText;
}
