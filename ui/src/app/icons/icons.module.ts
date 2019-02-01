import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {ExpandComponent} from './expand/component';
import {NotebookComponent} from './notebook/component';
import {ScrollComponent} from './scroll/component';
import {ShareComponent} from './share/component';
import {ShrinkComponent} from './shrink/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    ExpandComponent,
    NotebookComponent,
    ScrollComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
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
