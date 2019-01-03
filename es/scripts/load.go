package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"flag"
	"io"
	"io/ioutil"
	"log"
	"math"
	"net/http"
	"path/filepath"
	"strings"
	"time"
)

var esBaseURL = flag.String("es", "http://localhost:9200", "ES Index URL with type")
var in = flag.String("in", "", "Input JSON participants")
var conceptsIn = flag.String("cin", "", "Input JSON concepts")
var jsonNewLines = flag.Bool("jsonNewLines", false, "Whether JSON files are newline delimited")
var preserveIndices = flag.Bool("preserveIndices", false, "Whether or not to recreate the indexes")
var c = &http.Client{}
var dry = false

const (
	personMappingJSON = `
{
  "mappings": {
  	"person": {
  		"properties": {
  			"age": {
  				"type": "integer"
  			},
  			"condition_ids": {
  				"type": "keyword"
  			},
  			"exp_conditions": {
  				"type": "keyword"
  			},
  			"condition_names": {
  				"type": "text",
  				"fields": {
  					"keyword": {
  						"type": "keyword",
  						"ignore_above": 256
  					}
  				}
  			},
  			"conditions": {
  				"type": "nested",
  				"properties": {
  					"condition_concept_id": {
  						"type": "keyword"
  					},
  					"condition_start_date": {
  						"type": "date"
  					}
  				}
  			},
  			"drug_ids": {
  				"type": "keyword"
  			},
  			"drug_names": {
  				"type": "text",
  				"fields": {
  					"keyword": {
  						"type": "keyword",
  						"ignore_above": 256
  					}
  				}
  			},
  			"drugs": {
  				"type": "nested",
  				"properties": {
  					"drug_concept_id": {
  						"type": "keyword"
  					},
  					"drug_exposure_start_date": {
  						"type": "date"
  					}
  				}
  			},
  			"gender": {
  				"type": "text",
  				"fields": {
  					"keyword": {
  						"type": "keyword",
  						"ignore_above": 256
  					}
  				}
  			},
  			"measurement_ids": {
  				"type": "keyword"
  			},
  			"measurement_names": {
  				"type": "text",
  				"fields": {
  					"keyword": {
  						"type": "keyword",
  						"ignore_above": 256
  					}
  				}
  			},
  			"measurements": {
  				"type": "nested",
  				"properties": {
  					"measurement_source_concept_id": {
  						"type": "keyword"
  					},
  					"measurement_source_value": {
  						"type": "text",
  						"fields": {
  							"keyword": {
  								"type": "keyword",
  								"ignore_above": 256
  							}
  						}
  					}
  				}
  			},
  			"person_id": {
  				"type": "keyword"
  			}
  		}
  	}
  }
}
`
	conceptMappingJSON = `
{
  "mappings": {
    "concepts": {
      "properties": {
        "name": {
          "type": "text"
        },
        "domain": {
          "type": "keyword"
        }
      }
    },
  }
}
`
)

func personURL() string {
	return *esBaseURL + "/person"
}

func conceptURL() string {
	return *esBaseURL + "/concept"
}

func send(method, url string, r io.Reader) (string, error) {
	req, err := http.NewRequest(method, url, r)
	if err != nil {
		return "", err
	}
	req.Header.Add("Content-Type", "application/json")
	response, err := c.Do(req)
	if err != nil {
		return "", err
	} else {
		defer response.Body.Close()
		contents, err := ioutil.ReadAll(response.Body)
		if err != nil {
			return "", err
		}
		// fmt.Printf("%q: %d\n", url, response.StatusCode)
		// fmt.Printf("contents: %s\n", contents)
		return string(contents), nil
	}
}

func sendBulk(bb [][]byte) (string, error) {
	if dry {
		return "", nil
	}
	var rb []byte
	for _, b := range bb {
		rb = append(rb, b...)
		rb = append(rb, '\n')
	}
	req, err := http.NewRequest("POST", *esBaseURL+"/_bulk", bytes.NewReader(rb))
	if err != nil {
		return "", err
	}
	req.Header.Add("Content-Type", "application/x-ndjson")
	response, err := c.Do(req)
	if err != nil {
		return "", err
	} else {
		defer response.Body.Close()
		contents, err := ioutil.ReadAll(response.Body)
		if err != nil {
			return "", err
		}
		// fmt.Printf("_bulk: %d\n", response.StatusCode)
		// fmt.Printf("contents: %s\n", contents)
		return string(contents), nil
	}
}

func doBulkImport(paths []string, index, itype, idPath string) {
	start := time.Now()
	for i, path := range paths {
		ffrac := float64(i) / float64(len(paths))
		ps := fileToJSON(path, *jsonNewLines)

		const batchSize = 500
		for j := 0; j < len(ps); j += batchSize {
			if j > 0 && j%1000 == 0 {
				elapsed := time.Since(start)
				pfrac := float64(j) / float64(len(ps))
				tfrac := ffrac + pfrac/float64(len(paths))
				eta := time.Duration(float64(elapsed)/tfrac - float64(elapsed))
				log.Printf("[file %d/%d] Imported %.2f%% docs (%d/%d) [total: %.2fh ~%.2f%% (ETA %dh%dm)]",
					i+1, len(paths), 100*pfrac, j, len(ps),
					elapsed.Hours(), 100*(tfrac), int(eta.Hours()), int(eta.Minutes())%60)
			}

			maxRow := 0
			var jLines [][]byte
			for k := 0; k < batchSize && j+k < len(ps); k++ {
				p := ps[j+k]
				id := p[idPath].(string)
				b, err := json.Marshal(map[string]map[string]string{
					"create": {
						"_index": index,
						"_type":  itype,
						"_id":    id,
					},
				})
				if err != nil {
					log.Fatal(err)
				}
				jLines = append(jLines, b)

				if b, err = json.Marshal(p); err != nil {
					log.Fatal(err)
				}
				jLines = append(jLines, b)
				if maxRow < len(b) {
					maxRow = len(b)
				}
			}
			for retry := 0; true; retry++ {
				if resp, err := sendBulk(jLines); err != nil && retry > 5 {
					log.Fatal(err)
				} else if err != nil {
					log.Printf("WARNING: retrying failed bulk insert (%d/5) %v", retry, err)
					time.Sleep(time.Second * time.Duration(math.Pow(4, float64(retry))))
				} else {
					rj := map[string]interface{}{}
					if err := json.Unmarshal([]byte(resp), &rj); err != nil {
						log.Fatal(err)
					}
					items := rj["items"].([]interface{})
					for ii := range items {
						item := items[ii].(map[string]interface{})
						if _, ok := item["create"]; ok {
							st := item["create"].(map[string]interface{})["status"].(float64)
							if st >= 400 {
								log.Fatalf("bulk insert entry %d failed with %d: %+v", ii, st, item)
							}
						}
					}
					break
				}
			}
		}
	}
}

func recreateIndex(index, j string) {
	if _, err := send("DELETE", index, bytes.NewReader(nil)); err != nil {
		log.Fatal(err)
	}
	if _, err := send("PUT", index, strings.NewReader(j)); err != nil {
		log.Fatal(err)
	}
}

func fileToJSON(name string, newLines bool) []map[string]interface{} {
	b, err := ioutil.ReadFile(name)
	if err != nil {
		log.Fatal(err)
	}
	const bufSize = 16 * 1024 * 1024
	buf := make([]byte, 0, bufSize)
	var j []map[string]interface{}
	if newLines {
		s := bufio.NewScanner(bytes.NewReader(b))
		s.Buffer(buf, bufSize)
		for s.Scan() {
			var jl map[string]interface{}
			if err := json.Unmarshal(s.Bytes(), &jl); err != nil {
				log.Fatal(err)
			}
			j = append(j, jl)
		}
		if err := s.Err(); err != nil {
			log.Fatal(err)
		}
	} else {
		if err := json.Unmarshal(b, &j); err != nil {
			log.Fatal(err)
		}
	}
	return j
}

func main() {
	flag.Parse()

	if len(*in) > 0 {
		paths, err := filepath.Glob(*in)
		if err != nil {
			log.Fatal(err)
		}

		if !*preserveIndices {
			recreateIndex(personURL(), personMappingJSON)
		}

		doBulkImport(paths, "person", "person", "person_id")
	}

	if *conceptsIn != "" {
		paths, err := filepath.Glob(*conceptsIn)
		if err != nil {
			log.Fatal(err)
		}

		if !*preserveIndices {
			recreateIndex(conceptURL(), conceptMappingJSON)
		}

		doBulkImport(paths, "concept", "concept", "concept_id")
	}
}
