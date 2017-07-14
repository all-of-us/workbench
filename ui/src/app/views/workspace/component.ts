import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';

class cohort {
  id: string;
  name: string;
  description: string;
  url: string;
  date: Date;
  notebook: string;
  constructor(id:string, name:string, description:string,url:string,date:Date,notebook:string){
    this.id = id;
    this.name = name;
    this.description = description;
    this.url = url;
    this.date = date;
    this.notebook = notebook;
  }
}
let cohort1 = new cohort("cohort1", "Cohort 1", "Male, 40-50, Various ethnic groups, MA", "URL to the cohort", new Date("July 1, 2017"), "Notebook name");
let cohort2 = new cohort("cohort2", "Cohort 2", "Male, 10-50, Various ethnic groups, MA", "URL to the cohort", new Date("July 1, 2017"), "Notebook name");
let cohortList = [cohort1, cohort2];


class someComponent {
  public cohortList:Array<cohort>;

  constructor(){
    this.cohortList.concat(cohort1);
    this.cohortList.concat(cohort2);
  }
}


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


// WorkspaceComponent.controller('CohortController', function CohortController($scope){
//   $scope.cohorts = [
//     {
//       name: 'Cohort 1',
//       description: 'Male, 40-50, Various ethnic groups, MA',
//       url: 'URL to the cohort',
//       date: Date("July 1, 2017"),
//       notebook: 'Notebook name'
//     }
//   ]
// })

/*
WANT TO DO THIS ON PAGE LOAD, NOT INIT
for(let c of cohortList){
  console.log("Adding Cohort");
  console.log(document.getElementById("cohortTable"));
  document.getElementById("cohortTable").innerHTML +=
    "<tr>"
    "<td><a href='/'>" + c.name + "</a></td>" +
    "<td>" + c.description + "</td>" +
    "<td>" + c.url + "</td>" +
    "<td>" + c.date.toString() + "</td>" +
    "<td><a href='/'>" + c.notebook + "</a></td>" +
    "</tr>";
}
*/
