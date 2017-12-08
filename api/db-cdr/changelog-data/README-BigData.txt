# Liquibase does not like big .csv imports. So for now, until we build the permanent data copy solutions,
# to fill the cdr.concept and cdr.concept_relationship tables  for data browser follow these steps :
# Note , you don’t need do this if you are not running the data browser in the ui or using the api

# Todo  -- finalize big data imports solution for cdr releases outside of liquibase, ie automate this as part of cdr builds and  test
1. download  concept.csv and concept_relationship.csv to your local computer

https://storage.googleapis.com/all-of-us-ehr-dev-peter-speltz/concept.csv
https://storage.googleapis.com/all-of-us-ehr-dev-peter-speltz/concept_relationship.csv

2. Copy them to the docker engine for mysql import

peters-imac:api peter$ docker cp ~/Downloads/concept.csv api_db_1:/var/lib/mysql-files/.
peters-imac:api peter$ docker cp ~/Downloads/concept_relationship.csv api_db_1:/var/lib/mysql-files/.


#  Connect to db and use Mysql load data infile to fill the tables
peters-imac:api peter$ ./project.rb connect-to-db


# in mysql terminal
use cdr;

load data infile '/var/lib/mysql-files/concept.csv' into table concept fields terminated by ',' enclosed by '"' ignore 1 rows;

load data infile '/var/lib/mysql-files/concept_relationship.csv' into table concept_relationship fields terminated by ',' enclosed by '"' ignore 1 rows;


###
