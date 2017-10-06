(ns bucklew.core
  (:require
    [bucklew.udp :as udp]
    [bucklew.components :as comps]
    [bucklew.entities :as ents]
    [bucklew.events :as events]
    [bucklew.items :as items]))


(def player 
  (ents/sort-components
    (ents/map->Entity {:id 1
                           :nomen "Player"
                           :components [(comps/Armour {:strength 2})
                                        (comps/Physics {:max-hp 20 :hp 20})
                                        (comps/CanAttack)
                                        (comps/Inventory)
                                        (comps/Location {:x 1 :y 1})
                                        (comps/Equipment)]})))

(def warrior (ents/map->Entity
  {:id 2
   :nomen "Warrior"
   :components [(comps/Armour {:strength 1})
                (comps/Physics {:max-hp 20 :hp 20})]}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  )
