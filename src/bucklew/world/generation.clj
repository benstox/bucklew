(ns bucklew.world.generation
  (:use [bucklew.world.core :only [tiles get-tile-from-tiles random-coordinate
                                   world-size ->World tile-walkable?
                                   find-empty-tile]])
  (:require [clojure.set]
            [bucklew.coords :as coords]
            [bucklew.creatures :as creats]))


; Convenience -----------------------------------------------------------------
(def all-coords
  (let [[cols rows] world-size]
    (for [x (range cols)
          y (range rows)]
      {:x x :y y})))

(defn get-tile-from-level [level coord]
  (let [{:keys [x y]} coord]
    (get-in level [y x] (:bound tiles))))


; Region Mapping --------------------------------------------------------------
(defn filter-walkable
  "Filter the given coordinates to include only walkable ones."
  [level coords]
  (set (filter #(tile-walkable? (get-tile-from-level level %))
               coords)))


(defn walkable-neighbours
  "Return the neighbouring coordinates walkable from the given coord."
  [level coord]
  (filter-walkable level (coords/neighbours coord)))

(defn walkable-from
  "Return all coordinates walkable from the given coord (including itself)."
  [level coord]
  (loop [walked #{}
         to-walk #{coord}]
    (if (empty? to-walk)
      walked
      (let [current (first to-walk)
            walked (conj walked current)
            to-walk (disj to-walk current)
            candidates (walkable-neighbours level current)
            to-walk (clojure.set/union to-walk (clojure.set/difference candidates walked))]
        (recur walked to-walk)))))


(defn get-region-map
  "Get a region map for the given level.

  A region map is a map of coordinates to region numbers.  Unwalkable
  coordinates will not be included in the map.  For example, the map:

  .#.
  ##.

  Would have a region map of:

    x y  region
  {[0 0] 0
   [2 0] 1
   [2 1] 1}

  "
  [level]
  (loop [remaining (filter-walkable level all-coords)
         region-map {}
         n 0]
    (if (empty? remaining)
      region-map
      (let [next-coord (first remaining)
            next-region-coords (walkable-from level next-coord)]
        (recur (clojure.set/difference remaining next-region-coords)
               (into region-map (map vector
                                     next-region-coords
                                     (repeat n)))
               (inc n))))))


; Random World Generation -----------------------------------------------------
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
    {:x (+ x dx) :y (+ y dy)}))

(defn get-block [tiles x y]
  (map (partial get-tile-from-tiles tiles)
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


; Creatures -------------------------------------------------------------------
(defn add-creature [world make-creature]
  (let [creature (make-creature (find-empty-tile world))
        entities (:entities world)
        new-entities (conj entities creature)]
    (assoc world :entities new-entities)))

(defn add-creatures [world make-creature n]
  (nth (iterate #(add-creature % make-creature)
                world)
       n))

(defn populate-world [world]
  (let [world (assoc world :entities [(creats/make-player (find-empty-tile world))])]
    (-> world
      (add-creatures creats/whirling-dervish 5))))
    ;   (add-creatures make-bunny 20)
    ;   (add-creatures make-silverfish 4))))
    ; world))


; Actual World Creation -------------------------------------------------------
(defn random-world []
  (let [world (->World (random-tiles) [] 0)
        world (nth (iterate smooth-world world) 3)
        world (populate-world world)
        world (assoc world :regions (get-region-map (:tiles world)))]
    world))

(defn empty-room-world []
  (let [world (->World (empty-room-tiles) [])
        world (populate-world world)]
    world))
