# Test files for UpdateCdrConfig

## Validation tests: these files should all be rejected

### Access Tier

Duplicate ID: duplicate_tier_id.json

Duplicate Short Name: duplicate_tier_shortName.json

Duplicate Display Name: duplicate_tier_displayName.json

### CDR Version

CDR Version duplicate ID: duplicate_cdr_id.json

CDR Version missing ID: missing_id_cdr.json

CDR Version archived default: archived_default_cdr.json

### Tier/CDR Integration

CDR Version / Access Tier mismatch: cdr_tier_mismatch.json

CDR Version has no Access Tier: cdr_no_tier.json

Access Tier has no CDR Versions: no_cdrs_in_tier.json

CDR Version multiple defaults in a tier: multi_default_in_tier.json

CDR Version no default in a tier: no_default_in_tier.json

### Missing properties RW-8487

CDR has wgsBigqueryDataset but not wgsFilterSetName: cdr_wgsFilterSetName_not_defined.json

CDR has wgsFilterSetName but not wgsBigqueryDataset: cdr_wgsBigqueryDataset_not_defined.json

## Tier migration test

Confirm that it's possible to add Access Tiers concurrently with CDR Versions. It is not clear at
present whether we have a use case for removing tiers. This will be a more involved process due to
foreign key contraints.

At the time of initial creation of this tool (Jan 2021) we have a single tier: ID 1, short name
'registered', represented by `tier_migration_1.json`. The second file `tier_migration_2.json`
adds a second tier and associated CDR Versions. Removing the new tier by re-applying
`tier_migration_1.json` should be possible because there will be no resources referring to this
tier. 