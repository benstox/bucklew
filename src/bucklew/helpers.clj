(ns bucklew.helpers)

(defn find-first
	"Find the first thing that satisfies a condition in a sequence."
    [f coll]
    (first (filter f coll)))
