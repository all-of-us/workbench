import {NgModule} from '@angular/core';

import {CardMenuIconComponent} from 'app/icons/card-menu-icon/component';
import {EditComponent} from 'app/icons/edit/component';
import {ExpandComponent} from 'app/icons/expand/component';
import {NotebookComponent} from 'app/icons/notebook/component';
import {ScrollComponent} from 'app/icons/scroll/component';
import {ShareComponent} from 'app/icons/share/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {TrashComponent} from 'app/icons/trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    CardMenuIconComponent,
    EditComponent,
    ExpandComponent,
    NotebookComponent,
    ScrollComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
    CardMenuIconComponent,
    EditComponent,
    ExpandComponent,
    NotebookComponent,
    ScrollComponent,
    ShareComponent,
    ShrinkComponent
  ],
  providers: []
})
export class IconsModule {}
