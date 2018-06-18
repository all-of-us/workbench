import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'highlightSearch'
})
export class HighlightSearchPipe implements PipeTransform {

  transform(value: any, args?: any): any {
    let words = args.split(new RegExp(',| '));
    words = words.filter(w => w.length > 0 );
    const reString = words.join('|');
    const re = new RegExp(reString, 'gi');
    return value.replace(re, '<mark>$&</mark>');
  }

}
