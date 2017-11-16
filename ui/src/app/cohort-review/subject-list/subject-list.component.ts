import {Component, OnInit} from '@angular/core';
import {StringFilter} from 'clarity-angular';

import {Subject} from '../model';

class SubjectFilter implements StringFilter<Subject> {
  accepts(subject: Subject, search: string): boolean {
    return '' + subject.id === search
      || subject.status.toLowerCase().indexOf(search) >= 0;
  }
}

@Component({
  selector: 'app-subject-list',
  templateUrl: 'subject-list.component.html',
  styleUrls: ['subject-list.component.css']
})
export class SubjectListComponent implements OnInit {

  public subjects: Subject[];

  /* tslint:disable-next-line:no-unused-variable */
  private subjectFilter = new SubjectFilter();

  ngOnInit() {
    this.subjects = [new Subject(127736, 'NR'),
      new Subject(228375, 'NR'),
      new Subject(362134, 'NR'),
      new Subject(423987, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR'),
      new Subject(526534, 'NR')];
  }

}
