(ns bucklew.world.core
  (:require [bucklew.coords :as coords]
            [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.helpers :as help]
            [com.rpl.specter :as sp :refer [select selected? ALL FIRST INDEXED-VALS]]
            [ebenbild.core :as eb]))


; Constants -------------------------------------------------------------------
(def world-size [80 29])

; Data structures -------------------------------------------------------------
(defprotocol WorldProtocol
  (get-entities-by-location [this location]
    "Return the indices of any entities at a given location.")
  (get-interacting-entities-by-location [this locadtion]
    "Return the indices of any entities at a given location that have the CanInteract component.")
  (get-interaction-from-location [this interactor location]
    "Get all the entities at a certain location, check whether any of them
    have an interaction, decide whether they are an ally or enemy of the
    interactor and return the relevant entity and interaction.")
  (get-entity-by-id [this id]
    "Return the entity with a certain id."))

(defrecord World [tiles entities entity-i]
  WorldProtocol
  (get-entities-by-location [this location]
    (let [{:keys [x y]} location]
      (select [:entities
               INDEXED-VALS ; creates [entity-i entity]
               (selected? 1 :components ALL (eb/like {:nomen :location :x x :y y})) ; declarative query
               FIRST] ; navigate to the first item
         world)))
  (get-interacting-entities-by-location [this location]
    (let [{:keys [x y]} location]
      (select [:entities
               INDEXED-VALS ; creates [entity-i entity]
               (selected? 1 :components ALL (eb/like {:nomen :location :x x :y y}))
               (selected? 1 :components ALL (eb/like {:nomen :can-interact}))
               FIRST] ; navigate to the first item
         world)))
  (get-interaction-from-location [this interactor location]
    (let [entities-at-dest (get-entities-by-location this location)
          interactor-team (-> interactor
            (ents/get-components-by-nomen :can-interact)
            (first)
            (:team))
          get-interaction-event (assoc-in events/get-interaction [:data :interactor-team] interactor-team)
          results (map (fn [e] [(first e) (ents/receive-event get-interaction-event (second e))]) entities-at-dest)
          results-w-interaction (filter #(nil? (get-in (second %) [:data :interaction])) results)]
      (if (empty? results-w-interaction)
        nil
        (let [[target-i [target {{interaction :interaction} :data}]] (first results-w-interaction)]
          {:target-i target-i :target target :interaction interaction}))))
  (get-interaction-from-location [this interactor-team location]
    (let [indices-at-dest (get-interacting-entities-by-location this location)]
      (if (empty? indices-at-dest)
        nil
        (let [target-i (first indices-at-dest)
              interaction-comp (select [:entities
                                        target-i
                                        :components
                                        ALL
                                        (selected? (eb/like {:nomen :can-interact}))]
                                  this)
              target-team (:team interaction-comp)
              same-team (= interactor-team target-team)
              ]))))
  (get-entity-by-id [this id]
    (let [indexed-entities (help/enumerate entities)
          relevant-entities (filter (comp :id last) indexed-entities)]
      (if (empty? relevant-entities)
        (throw (Exception. (str "No entities in world with ID " id)))
        (first relevant-entities)))))


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

(defn find-empty-neighbour [world coord]
  (let [candidates (filter #(is-empty? world %) (coords/neighbours coord))]
    (when (seq candidates)
      (rand-nth candidates))))


(defn check-tile
  "Check that the tile at the destination passes the given predicate."
  [world dest pred]
  (pred (get-tile-kind world dest)))
