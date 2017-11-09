import {Directive, ViewContainerRef} from '@angular/core';

@Directive({selector: '[critAttrFormHost]'})
export class AttributesDirective {
  constructor(public container: ViewContainerRef) { }
}
