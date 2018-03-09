import {Component, Input, OnInit} from '@angular/core';

import {Participant} from '../participant.model';

@Component({
  selector: 'app-sidebar-content',
  templateUrl: './sidebar-content.component.html',
  styleUrls: ['./sidebar-content.component.css']
})
export class SidebarContentComponent {
  @Input() participant: Participant;
}
