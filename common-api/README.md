# Common API

Common utilities, services, data models between the Workbench and Public Data Browser.

## Data model distinction

Some models, e.g. cdr/model/Concept.java, may be used with either a registered CDR
database or a public CDR database (depending on which app it is being used in). For the
public DB, sensitive tables go through additional deidentification, e.g. for Concept the
registered DB contains exact participant counts where the public DB has binned counts. 
