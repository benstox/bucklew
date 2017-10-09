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

(defn find-physics-component [components]
  (let [nomen-is-physics (partial nomen-is :physics)
         physics (find-first nomen-is-physics components)]
    physics))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))
