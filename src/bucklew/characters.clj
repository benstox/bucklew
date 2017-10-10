(ns bucklew.characters
  (:require [bucklew.entities :as ents]
            [bucklew.components :as comps]))

(def player
  (ents/sort-components
    (ents/map->Entity {:id 1
                       :nomen "Player"
                       :components [(comps/Physics {:max-hp 20 :hp 20})
                                    (comps/CanAttack)
                                    (comps/Inventory)
                                    (comps/Location {:x 1 :y 5})
                                    (comps/Equipment)
                                    (comps/IsPlayer)
                                    (comps/Display {:glyph \@})]})))
