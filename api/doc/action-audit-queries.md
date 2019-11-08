# Action Audit Queries

## Schema
The Action Audit schema is a flattened schema where each row of the database 

## Target Properties
The target properties are key-value string pairs and are not in a strictly
controlled schema, but are rather enforced by application constraints. Each target type 
supports one or more properties, and these are outlined below:

| Target Type | Property  | Actions |
|-------------|----------|---------|
| WORKSPACE   | NAME     | CREATE, EDIT, DUPLICATE |
| WORKSPACE | INTENDED_STUDY | CREATE, EDIT, DUPLICATE |
| WORKSPACE | CREATOR |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | ADDITIONAL_NOTES |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | ANTICIPATED_FINDINGS |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | DISEASE_OF_FOCUS |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | REASON_FOR_ALL_OF_US |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | NAMESPACE |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | CDR_VERSION_ID |  CREATE, EDIT, DUPLICATE |



