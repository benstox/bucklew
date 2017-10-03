(ns bucklew.core
  (:require
    [bucklew.udp :as udp]
    [bucklew.components :as components]
    [bucklew.entities :as entities]
    [bucklew.events :as events]))


(def player (entities/map->Entity
  {:id 1
   :nomen "Player"
   :components [(components/physics 20) (components/armour 2)]}))

(def warrior (entities/map->Entity
  {:id 2
   :nomen "Warrior"
   :components [(components/physics 20) (components/armour 1)]}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ; (def felis {:likes-dogs true :ocd-bathing true})
  ; (def morris (udp/beget {:likes-9lives true} felis))
  ; (def post-traumatic-morris (udp/beget {:likes-dogs nil} morris))
  ; (println (udp/get felis :likes-dogs))
  ; (println (udp/get morris :ocd-bathing))
  ; (println (udp/get morris :likes-dogs))
  ; (println (udp/get post-traumatic-morris :likes-dogs))
  ; (println (udp/get post-traumatic-morris :likes-other-cats))
  (println (str player))
  (println "Attack for 5")
  (def player (entities/receive-event player events/take-damage-event))
  (println (str player))

  (println "Put on strength 2 armour.")
  (def player (entities/add-component player (components/armour)))
  (println (str player))
  (println "Attack for 5")
  (def player (entities/receive-event player events/take-damage-event))
  (println (str player))
  
  (println "Put on another strength 2 armour.")
  (def player (entities/add-component player (components/armour)))
  (println (str player))
  (println "Attack for 5")
  (def player (entities/receive-event player events/take-damage-event))
  (println (str player))
  
  (println "Put on strength 1000 armour.")
  (def player (entities/add-component player (components/armour 1000)))
  (println (str player))
  (println "Attack for 5")
  (def player (entities/receive-event player events/take-damage-event))
  (println (str player))
  )
