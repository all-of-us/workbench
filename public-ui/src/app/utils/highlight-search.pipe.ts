import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'highlightSearch'
})
export class HighlightSearchPipe implements PipeTransform {

  transform(value: any, args?: string): any {
    let words = args.split(new RegExp(',| '));
    words = words.filter(w => w.length > 0 );
    words = words.map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
    const reString = words.join('|');
    const re = new RegExp(reString, 'gi');
    return value.replace(re, '<mark>$&</mark>');
  }

}
