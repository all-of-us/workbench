import {NgModule} from '@angular/core';

import {EditComponent} from './edit/component';
import {LeftScrollComponent} from './left-scroll/component';
import {LeftScrollLightComponent} from './left-scroll-light/component';
import {RightScrollComponent} from './right-scroll/component';
import {RightScrollLightComponent} from './right-scroll-light/component';
import {ShareComponent} from './share/component';
import {TrashComponent} from './trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent
  ],
  exports: [
    TrashComponent,
    EditComponent,
    LeftScrollComponent,
    LeftScrollLightComponent,
    RightScrollComponent,
    RightScrollLightComponent,
    ShareComponent
  ],
  providers: []
})
export class IconsModule {}
