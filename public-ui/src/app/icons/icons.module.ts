import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {ShareComponent} from './share/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    ShareComponent
  ],
  exports: [
    TrashComponent,
    EditComponent,
    ShareComponent
  ],
  providers: []
})
export class IconsModule {}
