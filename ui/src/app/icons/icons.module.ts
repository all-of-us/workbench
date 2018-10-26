import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {LeftScrollLightComponent} from './left-scroll-light/component';
import {LeftScrollComponent} from './left-scroll/component';
import {NotebookComponent} from './notebook/component';
import {RightScrollLightComponent} from './right-scroll-light/component';
import {RightScrollComponent} from './right-scroll/component';
import {ShareComponent} from './share/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    NotebookComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent
  ],
  exports: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    NotebookComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent
  ],
  providers: []
})
export class IconsModule {}
