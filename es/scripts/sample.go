/*
Find a reasnable modulo value on participant IDs for sub sample queries.
*/
package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"math"
	"os"
	"strconv"
)

var (
	in     = flag.String("in", "", "Input participant IDs CSV")
	target = flag.Int("target", 5, "target sample size")
)

func main() {
	flag.Parse()

	f, err := os.Open(*in)
	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()
	s := bufio.NewScanner(f)
	s.Scan()

	var ids []int64
	for s.Scan() {
		sid := s.Text()
		id, err := strconv.ParseInt(sid, 0, 0)
		if err != nil {
			log.Fatalf("bad id %q: %v", sid, err)
		}
		ids = append(ids, id)
	}

	mod, delta := findModulo(ids, *target)
	fmt.Printf("use modulo %d (+/- %d)\n", mod, delta)
}

func findModulo(ids []int64, t int) (int, int) {
	if len(ids) <= t {
		return 1, 0
	}

	mid := len(ids) / t
	best := mid
	var delta int = math.MaxInt32
	const k = 10
	for i := mid - k; i < mid+k && i < len(ids) && delta > 0; i++ {
		if i <= 0 {
			continue
		}
		var count int
		for _, id := range ids {
			if id%int64(i) == 0 {
				count++
			}
		}

		d := int(math.Abs(float64(t - count)))
		if d < delta {
			best = i
			delta = d
		}
	}
	return best, delta
}
