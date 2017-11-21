import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/Rx'; //unlocks all rxjs operators, such as map()


@Injectable() //required if you want to inject a service into a service... such as the built-in http service
export class PersonService {
  race
  gender
  ethnicity
  graphItems
  constructor(private http: Http) {
  }

  getPersons() {
    return this.http.get('../assets/data/person.json')
      .map(
      (response) => {
        const data = response.json();
        this.race = data.RACE_DATA;
        this.gender = data.GENDER_DATA;
        this.ethnicity = data.ETHNICITY_DATA;
        this.race.name = "race";
        this.gender.name = "gender";
        this.ethnicity.name = "ethnicity";
        this.graphItems = [this.race, this.gender, this.ethnicity]
        // //
        return this.graphItems
      }
      )
  }
  getLocation() {
    return this.http.get('https://cpmdev.app.vumc.org/api/public/analysis_result?analysis_id=1101&dataType=counts')
      .map(
      (response) => {
        //
        return response.json()
      }
      )
  }

}
