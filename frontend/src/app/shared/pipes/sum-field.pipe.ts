import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'sumField', standalone: true })
export class SumFieldPipe implements PipeTransform {
  transform(arr: any[], field: string): number {
    if (!arr || !field) return 0;
    return arr.reduce((acc, item) => acc + (Number(item[field]) || 0), 0);
  }
}
