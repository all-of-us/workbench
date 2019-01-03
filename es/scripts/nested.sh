#!/bin/bash

curl -X GET "localhost:9200/person/person/_search?pretty" -H 'Content-Type: application/json' -d'
{
    "query": {
      "function_score": {
        "query": {
        "nested" : {
            "path" : "conditions",
            "score_mode" : "sum",
            "query" : {
              "constant_score": {
              "filter": {
                "bool" : {
                    "must" : [
                    { "match" : {"conditions.condition_concept_id" : "138525"} },
                    { "range" : {"conditions.condition_start_date" : {"gt" : "20140101", "format": "basic_date"}} }
                    ]
                }
              },
              "boost": 1
              }
            }
        }
    },
    "min_score": 3
  }
  }
}
'
