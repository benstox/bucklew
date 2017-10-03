(ns bucklew.helpers)

(defn find-first
  "Find the first thing that satisfies a condition in a sequence."
  [f coll]
  (first (filter f coll)))

(defn find-first-with-index
  [f coll]
  (let [indexed (map vector coll (range))]
    (first (filter (comp f first) indexed))))

(defn nomen-is [nomen coll]
  "Check whether the name of this object is a certain name."
  (= (:nomen coll) nomen))
