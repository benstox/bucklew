(ns bucklew.core
  (:require
    [bucklew.udp :as udp]
    [bucklew.components :as components]
    [bucklew.entities :as entities]
    [bucklew.events :as events]))


(def player 
  (entities/sort-components
    (entities/map->Entity {:id 1
                           :nomen "Player"
                           :components [(components/Armour {:strength 2})
                                        (components/Physics {:max-hp 20 :hp 20})
                                        (components/CanAttack)]})))

(def warrior (entities/map->Entity
  {:id 2
   :nomen "Warrior"
   :components [(components/Armour {:strength 1})
                (components/Physics {:max-hp 20 :hp 20})]}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  )
