import {NgModule} from '@angular/core';

import {ExpandComponent} from 'app/icons/expand/component';
import {ShareComponent} from 'app/icons/share/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {TrashComponent} from 'app/icons/trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    ExpandComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
    ExpandComponent,
    ShareComponent,
    ShrinkComponent
  ],
  providers: []
})
export class IconsModule {}
