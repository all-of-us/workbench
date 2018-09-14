
import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
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
    readonly treeType = TreeType;

    constructor(private actions: CohortSearchActions) {}


    remove(): void {
        if (this.treeType.DEMO) {
            const paramId = this.parameter.get('parameterId');
            const type = this._type;
            this.demoItems.emit({paramId,type});
            this.actions.removeParameter(paramId);

        } else {
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

