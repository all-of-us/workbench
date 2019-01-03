#!/bin/bash

# This script demonstrates a temporal query via ES. It's hairy.

curl -X GET "localhost:9200/person/person/_search?pretty" -H 'Content-Type: application/json' -d'{
 "query": {
    "bool": {
      "filter": [
        {
          "nested": {
            "path": "conditions",
            "query": {
              "bool": {
                "filter": [
                  {
                    "match": {"conditions.condition_concept_id": "318800"}
                  }
                ]
              }
            }
          }
        },
        {
          "nested": {
            "path": "conditions",
            "query": {
              "bool": {
                "filter": [
                  {
                    "match": {"conditions.condition_concept_id": "373503"}
                  }
                ]
              }
            }
          }
        },
        {
          "script": {
            "script": {
              "source": "/*This is a Painless (similar to Groovy) script for performing an ES temporalquery. This requires a custom encoding of recurring events in ElasticSearchas it does not natively support complex JOIN queries like this, and nested datahas limited availability from within scripts. Finally, parallel arrays withconserved order are also not possible in the ES filtering layer, as the docavailable to the script is a Lucene indexed representation (sorted).*/def sf = new SimpleDateFormat(\"yyyy-MM-dd\");ArrayList aDates = new ArrayList();ArrayList bDates = new ArrayList();for (def c : doc.exp_conditions) {  int sep = c.indexOf(\":\");  def cid = c.substring(0, sep);  long dateMillis = sf.parse(c.substring(sep+1)).getTime();  if (params.a.equals(cid)) {    aDates.add(dateMillis);  } else if (params.b.equals(cid)) {    bDates.add(dateMillis);  }}/* a/b are symmetric - sort/index into the shorter of the two for performance. */if (aDates.size() > bDates.size()) {  def tmp = aDates;  aDates = bDates;  bDates = tmp;}Collections.sort(aDates);long diff = params.diffDays * 24 * 60 * 60 * 1000;for (def t : bDates) {  /* Find the nearest candidate to our lower bound and test it. */  int left = -Collections.binarySearch(aDates, t-diff) - 1;  if (left < 0 || (left < aDates.size() && aDates.get(left)-t <= diff)) {    return true;  }  /* Find the nearest candidate to our upper bound and test it. */  int right = -Collections.binarySearch(aDates, t+diff) - 2;  if (right < -1 || (right < aDates.size() && t-aDates.get(right) <= diff)) {    return true;  }}return false;",
              "params": {
                "a": "318800",
                "b": "373503",
                "diffDays": 3
              }
            }
          }
        }
      ]
    }
  }
}'


# fromDate=Date.parse('yyyy-MM-dd',fromDateParam);toDate=Date.parse('yyyy-MM-dd',toDateParam);count=0;for(d in _source.transactions){docsDate=Date.parse('yyyy-MM-dd',d.get('date'));if(docsDate>=fromDate && docsDate<=toDate){count++};if(count==3){return true;}};return false;

#        "source": "Debug.explain(params._source.conditions.get(0).condition_start_date);"

#        "source": "def sf = new SimpleDateFormat(\"yyyy-MM-dd\");ArrayList aDates = new ArrayList();for (def i; i < params._source.conditions.size(); i++) {  def c = params._source.conditions.get(i);  if (params.a.equals(c.condition_concept_id)) {    aDates.add(sf.parse(c.condition_start_date));  }}Collections.sort(aDates);Debug.explain(aDates.subList(0,2));",



# Debug.explain([(long) left, (long) right, t, aDates.size()]);}}return false;
