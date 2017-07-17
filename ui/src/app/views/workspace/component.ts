import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';

class Cohort {
  id: string;
  name: string;
  description: string;
  url: string;
  date: Date;
  notebook: string;
  constructor(id: string, name: string,
              description: string, url: string, date: Date,
              notebook: string) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.url = url;
    this.date = date;
    this.notebook = notebook;
  }
}

const cohort1 = new Cohort('cohort1', 'Cohort 1',
                          'Male, 40-50, Various ethnic groups, MA', 'URL to the cohort',
                          new Date('July 1, 2017'), 'Notebook name');
const cohort2 = new Cohort('cohort2', 'Cohort 2',
                          'Male, 10-50, Various ethnic groups, MA', 'URL to the cohort',
                          new Date('July 1, 2017'), 'Notebook name');
const cohortList = [cohort1, cohort2];


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit {
  repositories: Repository[] = [];
  user: User;
  cohortList = cohortList;
  constructor(
      private router: Router,
      private userService: UserService,
      private repositoryService: RepositoryService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
  }

  goToCohort(id: number): void {
    this.router.navigate(['/cohort', id]);
  }

  goToNotebook(id: number): void {

  }
}
