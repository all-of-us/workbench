# Test files for UpdateCdrConfig

## Validation tests: these files should all be rejected

Access Tier Duplicate ID: duplicate_tier_id.json

CDR Version Duplicate ID: duplicate_cdr_id.json

CDR Version Missing ID: missing_id_cdr.json

CDR Version Multiple Defaults: multi_default_cdr.json

CDR Version Archived Default: archived_default_cdr.json

CDR Version / Access Tier Mismatch: cdr_tier_mismatch.json

CDR Version has no Access Tier: cdr_no_tier.json

## Tier migration test

Confirm that it's possible to add Access Tiers concurrently with CDR Versions.
It is not clear at present whether we have a use case for removing tiers.
This will be a more involved process due to foreign key contraints.

At the time of initial creation of this tool (Jan 2021) we have a single tier: ID 1, short name 
'registered', represented by `tier_migration_1.json`.  The second file `tier_migration_2.json` 
adds a second tier and associated CDR Versions.  Removing the new tier by re-applying 
`tier_migration_1.json` should be possible because there will be no resources referring to 
this tier. 