import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
import {CohortEditService} from 'app/services/cohort-edit.service';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortBuilderPlaceholderComponent implements OnInit {
  user: User;
  adding = false;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }
  }

  continue(): void {
    if (this.adding) {
      this.router.navigate(['../create'], {relativeTo : this.route});
    } else {
      this.router.navigate(['../edit'], {relativeTo : this.route});
    }
  }


}
