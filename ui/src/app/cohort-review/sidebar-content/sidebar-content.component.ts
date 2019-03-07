import {Component, Input} from '@angular/core';

import {Participant} from 'app/cohort-review/participant.model';
import {CohortAnnotationDefinition, ParticipantCohortAnnotation} from 'generated/fetch';

@Component({
  selector: 'app-sidebar-content',
  templateUrl: './sidebar-content.component.html',
  styleUrls: ['./sidebar-content.component.css']
})
export class SidebarContentComponent {
  @Input() participant: Participant;
  @Input() annotations: ParticipantCohortAnnotation[];
  @Input() annotationDefinitions: CohortAnnotationDefinition[];
  @Input() setAnnotations: Function;
  @Input() openCreateDefinitionModal: Function;
  @Input() openEditDefinitionsModal: Function;
}
