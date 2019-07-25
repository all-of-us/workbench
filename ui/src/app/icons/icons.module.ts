import {NgModule} from '@angular/core';

import {ExpandComponent} from 'app/icons/expand/component';
import {NotebookComponent} from 'app/icons/notebook/component';
import {ReminderComponent} from 'app/icons/reminder';
import {ShareComponent} from 'app/icons/share/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {TrashComponent} from 'app/icons/trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    ExpandComponent,
    NotebookComponent,
    ReminderComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
    ExpandComponent,
    NotebookComponent,
    ReminderComponent,
    ShareComponent,
    ShrinkComponent
  ],
  providers: []
})
export class IconsModule {}
