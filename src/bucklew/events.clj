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
