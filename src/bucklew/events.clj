(ns bucklew.events)

(defrecord Event [nomen amount type target])

;example 
(def take-damage-event (map->Event
  {:nomen :take-damage,
   :amount 5}))

;example
(def make-attack-event (map->Event
  {:nomen :make-attack,
   :amount 0}))

(def get-location (map->Event {:nomen :get-location}))

(def tick (map->Event {:nomen :tick}))

(def draw (map->Event {:nomen :draw :target {}}))
