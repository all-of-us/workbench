import { Injectable } from '@angular/core';
import {Http} from '@angular/http';
import 'rxjs/Rx'; //unlocks all rxjs operators, such as map()




@Injectable()
export class MapService {

  baseUrl = 'assets/maps/'
  constructor(private http:  Http) {
      }
  getMap(name: string) {
    //Small sample
    let url = this.baseUrl + name;
    return this.http.get(url)
    //Json file with all 50 states and random counts
    // return this.http.get('http://beta.json-generator.com/api/json/get/4y1GnVBDm')
      .map(
        (response) => {
          const data = response.json();
          return data
        }
      )
  } //end of getMap()

}
