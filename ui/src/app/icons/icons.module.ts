import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {LeftScrollComponent} from './left-scroll/component';
import {RightScrollComponent} from './right-scroll/component';
import {ShareComponent} from './share/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    RightScrollComponent,
    ShareComponent
  ],
  exports: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    RightScrollComponent,
    ShareComponent
  ],
  providers: []
})
export class IconsModule {}
