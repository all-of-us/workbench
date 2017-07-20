import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';


import {CohortsService} from 'generated';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
import {CohortEditService} from 'app/services/cohort-edit.service'

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortEditComponent implements OnInit {
  user: User;

  cohortId: number;
  cohortName: string;
  cohortDescription: string = "Hello Cohort description";

  currentUrl: string;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private cohortsService: CohortsService,
      private CohortEditService: CohortEditService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
    this.currentUrl = this.router.url;
  }

  testClick(): void {
    this.CohortEditService.add(this.cohortName, this.cohortDescription).then(
      cohorts => this.router.navigate(['../..'], {relativeTo: this.route}));
  }

}
