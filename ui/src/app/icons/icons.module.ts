import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {ExpandComponent} from './expand/component';
import {LeftScrollLightComponent} from './left-scroll-light/component';
import {LeftScrollComponent} from './left-scroll/component';
import {RightScrollLightComponent} from './right-scroll-light/component';
import {RightScrollComponent} from './right-scroll/component';
import {ShareComponent} from './share/component';
import {ShrinkComponent} from './shrink/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    ExpandComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
    EditComponent,
    ExpandComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent,
    ShrinkComponent
  ],
  providers: []
})
export class IconsModule {}
