(ns bucklew.helpers)

(defn get-menu-options
  [game]
  "Generate a map of menu options for the main menu."
  (if (:world game)
    [{:text "Continue", :action :continue} {:text "New game", :action :new-game}]
    [{:text "New game", :action :new-game} {:text "~~Empty option~~" :action nil}]))

(def keys-to-directions
  {\k     :n
   :up    :n
   \l     :e
   :right :e
   \j     :s
   :down  :s
   \h     :w
   :left  :w
   \u     :ne
   \n     :se
   \b     :sw
   \y     :nw})

(defn valid-move?
  "Does towards the destination tile constitute a valid move?"
  [dest-coords tiles]
  (let [{:keys [x y]} dest-coords
        {kind :kind :as destination} (get-in tiles [y x])]
    (not (= :wall kind))))

(defn valid-move-direction?
  "Does this direction constitute a valid move?"
  [entity move-data])

(defn find-first
  "Find the first thing that satisfies a condition in a sequence."
  [f coll]
  (first (filter f coll)))

(defn find-first-with-index
  [f coll]
  (let [indexed (map vector coll (range))]
    (first (filter (comp f first) indexed))))

(defn nomen-is
  "Check whether the name of this object is a certain name."
  [nomen coll]
  (= (:nomen coll) nomen))

(defn find-physics-component [components]
  (let [nomen-is-physics (partial nomen-is :physics)
         physics (find-first nomen-is-physics components)]
    physics))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

; lolclojure
(defn abs [i]
  (if (neg? i)
    (- i)
    i))

(defn map2d
  "Map a function across a two-dimensional sequence."
  [f s]
  (map (partial map f) s))

(defn slice
  "Slice a sequence."
  [s start width]
  (->> s
    (drop start)
    (take width)))

(defn shear
  "Shear a two-dimensional sequence, returning a smaller one."
  [s x y w h]
  (map #(slice % x w)
       (slice s y h)))

(defn enumerate [s]
  (map-indexed vector s))
