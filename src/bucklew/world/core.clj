(ns bucklew.world.core
  (:use [bucklew.coords :only [neighbors radial-distance]])
  (:require [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.helpers :as help]))


; Constants -------------------------------------------------------------------
(def world-size [80 29])

; Data structures -------------------------------------------------------------
(defprotocol WorldProtocol
  (get-entities-by-location [this location]
    "Return any entities at a certain location:
    * located-entities: `([entity event-containing-location-data], ...)`
    * indexed-entities and output: `([entity-i [entity event-containing-location-data]], ...)`")
  (get-entity-by-id [this id]
    "Return the entity with a certain id."))

(defrecord World [tiles entities]
  WorldProtocol
  (get-entities-by-location [this location]
    (let [{:keys [x y]} location
          located-entities (map
                             #(ents/receive-event % events/get-location)
                             entities)
          indexed-entities (help/enumerate located-entities)
          relevant-entities (filter
                              (comp #(and (= (:x %) x) (= (:y %) y)) :target last last)
                              indexed-entities)]
      relevant-entities))
  (get-entity-by-id [this id]
    (let [indexed-entities (help/enumerate entities)
          relevant-entities (filter (comp :id last) indexed-entities)]
      (if (empty? relevant-entities)
        (throw (Exception. (str "No entities in world with ID " id)))
        (first relevant-entities))))
  ents/EntityProtocol
  (receive-event [this event]
    (let [relevant-entities (get-entities-by-location this (:target event))]
      (if (not-empty relevant-entities)
        (loop [this this
               event event
               relevant-entities relevant-entities]
          (let [[entity-i [entity {:keys [target]}]] (first relevant-entities)
                other-entities (rest relevant-entities)
                [new-entity new-event] (ents/receive-event entity event)
                new-this (assoc-in this [:entities entity-i] new-entity)]
            (if (empty? other-entities)
              [new-this new-event]  ; return a (possibly) changed entity and event
              (recur new-this new-event other-entities))))
        [this event])))             ; no change to either the entity or the event)
  )

(defrecord Tile [kind glyph color])

(def tiles
  {:floor (->Tile :floor "." :white)
   :wall  (->Tile :wall  "#" :white)
   :up    (->Tile :up    "<" :white)
   :down  (->Tile :down  ">" :white)
   :bound (->Tile :bound "X" :black)})


; Convenience functions -------------------------------------------------------
(defn get-tile-from-tiles [tiles coord]
  (let [{:keys [x y]} coord]
    (get-in tiles [y x] (:bound tiles))))

(defn random-coordinate []
  (let [[cols rows] world-size]
    {:x (rand-int cols), :y (rand-int rows)}))

(defn tile-walkable?
  "Return whether a (normal) entity can walk over this type of tile."
  [tile]
  (#{:floor :up :down} (:kind tile)))


; Querying a world ------------------------------------------------------------
(defn get-tile [world coord]
  (get-tile-from-tiles (:tiles world) coord))

(defn get-tile-kind [world coord]
  (:kind (get-tile world coord)))

(defn set-tile [world coord tile]
  (let [{:keys [x y]} coord]
    (assoc-in world [:tiles y x] tile)))

(defn set-tile-floor [world coord]
  (set-tile world coord (:floor tiles)))

; (defn get-entities-around
;   ([world coord] (get-entities-around world coord 1))
;   ([world coord radius]
;      (filter #(<= (radial-distance coord (:location %))
;                   radius)
;              (vals (:entities world)))))

(defn is-empty? [world coord]
  (and (tile-walkable? (get-tile world coord))
       (empty? (get-entities-by-location world coord))))

(defn find-empty-tile [world]
  (loop [coord (random-coordinate)]
    (if (is-empty? world coord)
      coord
      (recur (random-coordinate)))))

(defn find-empty-neighbor [world coord]
  (let [candidates (filter #(is-empty? world %) (neighbors coord))]
    (when (seq candidates)
      (rand-nth candidates))))


(defn check-tile
  "Check that the tile at the destination passes the given predicate."
  [world dest pred]
  (pred (get-tile-kind world dest)))

