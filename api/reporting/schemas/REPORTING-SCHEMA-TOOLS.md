# Reporting Schema Tools & Process
This directory and subdirectories archive schema changes
and tools to generate and translate them into code. There
are a few different stages, but the grunt work is mostly automated
for repeatability and consistency.

The ruby script `generate_all_tables.rb` will operate on all of the input CSV files (described below)
and produce output files in this directory structure 
```
$ ls ~/scratch/reportingOut5
big_query_json       projection_interface projection_query     swagger_yaml
```

## EXPLAIN Application DB Table
The `explain table` MySql [statement](https://dev.mysql.com/doc/refman/8.0/en/explain.html) generates a tabular
set of attributes for table. These are then saved as CSV files and placed into the `mysql_describe_csv` directory.

For example, the output of the `explain address;` is shown below.

| Field | Type | Null | Key | Default | Extra |
| :--- | :--- | :--- | :--- | :--- | :--- |
| id | bigint\(20\) | NO | PRI | NULL | auto\_increment |
| street\_address\_1 | varchar\(95\) | NO |  | NULL |  |
| street\_address\_2 | varchar\(95\) | YES |  | NULL |  |
| zip\_code | varchar\(10\) | YES |  | NULL |  |
| city | varchar\(95\) | NO |  | NULL |  |
| state | varchar\(95\) | NO |  | NULL |  |
| country | varchar\(95\) | NO |  | NULL |  |
| user\_id | bigint\(20\) | NO | MUL | NULL |  |

## Mapping to BigQuery Schema JSON Format
The next task is to translate the MySql table description into the [BigQuery schema format](https://cloud.google.com/bigquery/docs/schemas#specifying_a_json_schema_file). Given this CSV input, we only really care about the field name and types. The
relational constraints won't be preserved in BigQuery, so they're basically ignored,
but we do want the primary key (which would be renamed to `address_id` in this example,
though in this case we actually merge `address` columns into the
`user` table). The script generates the for each table, with the example
of the address table shown. All fields are marked nullable, as to do otherwise would involve
adding constraints on the source data and/or its snapshot & upload processes.

```json
[
  {
    "name": "id",
    "description": "",
    "type": "INT64",
    "mode": "NULLABLE"
  },
  {
    "name": "street_address_1",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "street_address_2",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "zip_code",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "city",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "state",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "country",
    "description": "",
    "type": "STRING",
    "mode": "NULLABLE"
  },
  {
    "name": "user_id",
    "description": "",
    "type": "INT64",
    "mode": "NULLABLE"
  }
]
```

## Spring Data Projection Interface
The Ruby script builds projection interface definitions and query
annotations for the DAO method to fetch the projection. Since these must match exactly, it makes sense
to generate them in the same script.

We use a projection for reporting snapshots for a couple of reasons. First, it allows the schema used
in reporting to evolve independently of the main application schema (up to a point). Second, when doing
very large reads, avoiding the overhead of constructing full-fledged entities is useful. Finally, it
allows us to decouple the upload snapshot types (DTOs) from the MySQL return values. MapStruct should
allow clean convergence from the projection types to the DTOs, though the reverse isn't possible, as
we can't actually instantiate the projection interfaces.

### Projection Interface
The output for the Address table (assuming it's a stand-alone query) is below. In practice, these fields
are appended (by hand) to the `PrjUser` interface and query to denormalize it.
```java
interface PrjAddress {
  String getCity();
  String getCountry();
  long getId();
  String getState();
  String getStreetAddress1();
  String getStreetAddress2();
  long getUserId();
  String getZipCode();
}
```

### Projection Query Annotation
The projection interface query must match exactly in terms of type, name, and order of columns referenced.
Failing to do this seems to lead to runtime exceptions but rarely any compile-time warnings. Thus, it's important
to unit test these.

The following annotation is generated and should be attached to a DAO method such as `List<PrjAddress> findAllAddresses();`
```java
@Query("SELECT
+ "  a.city,"
+ "  a.country,"
+ "  a.id,"
+ "  a.state,"
+ "  a.streetAddress1,"
+ "  a.streetAddress2,"
+ "  a.userId,"
+ "  a.zipCode"
+ "FROM DbAddress a")
```

## Swagger DTO Classes
Currently the classes for the `ReportingSnapshot` object used
buy the upload service are specified in the API Swagger file. This was expedient,
but it means we're exposing internal details of the application in the API documentation.

The output should look something like this:
```yaml
---
ReportingAddress:
  type: object
  properties:
    id:
      description: ''
      type: integer
      format: int64
    streetAddress1:
      description: ''
      type: string
    streetAddress2:
      description: ''
      type: string
    zipCode:
      description: ''
      type: string
    city:
      description: ''
      type: string
    state:
      description: ''
      type: string
    country:
      description: ''
      type: string
    userId:
      description: ''
      type: integer
      format: int64
```
