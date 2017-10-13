(ns bucklew.world)

; Constants -------------------------------------------------------------------
(def world-size [50 11])

; Data structures -------------------------------------------------------------
(defrecord World [tiles])
(defrecord Tile [kind glyph color])

(def tiles
  {:floor (new Tile :floor "." :white)
   :wall  (new Tile :wall  "#" :white)
   :bound (new Tile :bound "X" :black)})

(defn get-tile [tiles x y]
  (get-in tiles [y x] (:bound tiles)))


; Debugging -------------------------------------------------------------------
(defn print-row [row]
  (println (clojure.string/join (map :glyph row))))

(defn print-world [world]
  (dorun (map print-row (:tiles world))))


; World generation ------------------------------------------------------------
(defn random-tiles []
  (let [[cols rows] world-size]
    (letfn [(random-tile []
              (tiles (rand-nth [:floor :wall])))
            (random-row []
              (vec (repeatedly cols random-tile)))]
      (vec (repeatedly rows random-row)))))


(defn empty-room-tiles []
  (let [[cols rows] world-size]
    (let [{:keys [floor wall]} tiles
          top-bottom-row (vec (repeat cols wall))
          middle-row (conj (into [wall] (repeat (- cols 2) floor)) wall)]
      (conj (into [top-bottom-row] (repeat (- rows 2) middle-row)) top-bottom-row))))


(defn get-smoothed-tile [block]
  (let [tile-counts (frequencies (map :kind block))
        floor-threshold 5
        floor-count (get tile-counts :floor 0)
        result (if (>= floor-count floor-threshold)
                 :floor
                 :wall)]
    (tiles result)))

(defn block-coords [x y]
  (for [dx [-1 0 1]
        dy [-1 0 1]]
    [(+ x dx) (+ y dy)]))

(defn get-block [tiles x y]
  (map (fn [[x y]]
         (get-tile tiles x y))
       (block-coords x y)))

(defn get-smoothed-row [tiles y]
  (mapv (fn [x]
          (get-smoothed-tile (get-block tiles x y)))
        (range (count (first tiles)))))

(defn get-smoothed-tiles [tiles]
  (mapv (fn [y]
          (get-smoothed-row tiles y))
        (range (count tiles))))

(defn smooth-world [{:keys [tiles] :as world}]
  (assoc world :tiles (get-smoothed-tiles tiles)))


(defn random-world []
  (let [world (new World (random-tiles))
        world (nth (iterate smooth-world world) 0)]
    world))

(defn empty-room-world []
  (let [world (new World (empty-room-tiles))]
    world))
