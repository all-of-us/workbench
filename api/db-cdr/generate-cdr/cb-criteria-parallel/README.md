# Parallel Process for Generating Criteria Tables

### Overview
This directory contains scripts that are used to build the criteria tables. These scripts were 
created by breaking up `make-bq-criteria-tables.sh`. By splitting up this file, it allows scripts 
to be run in parallel which significantly reduces the run time of the process. It will also 
hopefully make debugging easier since individual scripts can be run without going through the 
whole process.

### CI Diagram
A diagram of how the scripts can be run in parallel in a CI environment can be found 
[here](https://docs.google.com/drawings/d/117ubB54P006hQp5lo-k1-gBOApf5lCdq0B_Mm9Gi1fI/edit).

