import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class WorkspaceNotFoundComponent implements OnInit {
  wsNamespace: string;
  wsId: string;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

}
