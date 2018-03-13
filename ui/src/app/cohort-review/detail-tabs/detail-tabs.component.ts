import {Component, OnInit} from '@angular/core';

import {dummyData} from './dummy-data';

@Component({
  selector: 'app-detail-tabs',
  templateUrl: './detail-tabs.component.html',
  styleUrls: ['./detail-tabs.component.css']
})
export class DetailTabsComponent {
  DUMMY_CONDITIONS = dummyData;
}
