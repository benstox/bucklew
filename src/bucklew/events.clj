(ns bucklew.events)

(defrecord Event [nomen amount type])

(def attack-event (map->Event
	{:nomen :take-damage,
	 :amount 5
	 :type nil}))
