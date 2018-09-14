
import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import {TreeType} from 'generated';
import {CohortSearchActions} from '../redux';
import {attributeDisplay, nameDisplay, typeDisplay} from '../utils';

@Component({
    selector: 'crit-selection-info',
    templateUrl: './selection-info.component.html',
    styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent implements OnChanges{
    @Input() parameter;
    @Input() index;
    @Output() demoItems = new EventEmitter<boolean>();
    @Input() itemsSelected;
    readonly treeType = TreeType;

    constructor(private actions: CohortSearchActions) {}

    ngOnChanges() {
        if(this.name){
            console.log("checking")
            // this.demoItems.emit(false);
        }
        else{
            console.log("nothing")
        }
    }

    remove(): void {
        if(this.treeType.DEMO){
            const paramId = this.parameter.get('parameterId');
            console.log(paramId);
            this.demoItems.emit(paramId);
            this.actions.removeParameter(paramId);

        } else{
            const paramId = this.parameter.get('parameterId');
            const path = this.parameter.get('path');
            const id = this.parameter.get('id');
            this.actions.removeParameter(paramId, path, id);
        }
    }

    get _type()     { return typeDisplay(this.parameter); }
    get name()      { return nameDisplay(this.parameter); }
    get attribute() { return attributeDisplay(this.parameter); }
}

