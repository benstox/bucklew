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
        relevant-components (filter (comp #(contains? % nomen) second) indexed-components)]
    (println (str nomen " " (count relevant-components)))
    (println (str nomen " relevant components: " (vec relevant-components)))
    (if (not-empty relevant-components)
      (loop [game game
             event event
             relevant-components relevant-components]
        (let [[component-i component] (first relevant-components)
              rest-of-components (rest relevant-components)
              component-fn (nomen component)
              [new-game new-event] (component-fn game entity-i component-i event)]
          (println (str "just run " nomen " component-fn " (get-in new-game [:world :entities entity-i])))
          (if (empty? rest-of-components)
            (do (println "returning") [new-game new-event])  ; return a (possibly) changed entity and event
            (do (println "recurring") (recur new-game new-event rest-of-components)))))
      [game event]))) ; no change to either the entity or the event
