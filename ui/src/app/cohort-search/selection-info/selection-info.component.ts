
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {TreeType} from 'generated';
import {CohortSearchActions} from '../redux';
import {attributeDisplay, nameDisplay, typeDisplay} from '../utils';

@Component({
    selector: 'crit-selection-info',
    templateUrl: './selection-info.component.html',
    styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent {
    @Input() parameter;
    @Input() index;
    @Output() demoItems = new EventEmitter<any>();
    @Input() itemsSelected;
    @Input() parameterObj;
    treeType = TreeType;

    constructor(private actions: CohortSearchActions) {}


    remove(): void {
        const paramId = this.parameter.get('parameterId');
        const path = this.parameter.get('path');
        const id = this.parameter.get('id');
        const type = this._type;
        this.actions.removeParameter(paramId, path, id);
        this.demoItems.emit ({paramId,type});
    }

    get _type()     { return typeDisplay(this.parameter); }
    get name()      { return nameDisplay(this.parameter); }
    get attribute() { return attributeDisplay(this.parameter); }
}

