(ns bucklew.events
  (:require [bucklew.helpers :as help]))

(defrecord Event [nomen data])

;example
(def take-damage-event (map->Event
  {:nomen :take-damage
   :data {:amount 5}}))

;example
(def make-attack-event (map->Event
  {:nomen :make-attack
   :data {:amount 0}}))

(def get-location (map->Event {:nomen :get-location}))

(def tick (map->Event {:nomen :tick}))

(def draw (map->Event {:nomen :draw :data {}}))

(def move (map->Event {:nomen :move}))

(def get-interaction (map->Event {:nomen :get-interaction}))

(defn fire-event
  "Fire an event at an entity in the game world."
  [event game entity-i]
  (let [components (get-in game [:world :entities entity-i :components])
        nomen (:nomen event)
        indexed-components (help/enumerate components)
        relevant-components (filter (comp nomen second) indexed-components)]
    (if (not-empty relevant-components)
      (loop [game game
             event event
             relevant-components relevant-components]
        (let [[component-i component] (first relevant-components)
              lower-priority-components (rest relevant-components)
              component-fn (nomen component)
              [new-game new-event] (component-fn game entity-i component-i event)]
          (if (empty? lower-priority-components)
            [new-game new-event]  ; return a (possibly) changed entity and event
            (recur new-game new-event lower-priority-components))))
      [game event]))) ; no change to either the entity or the event
