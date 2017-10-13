(ns bucklew.creatures
  (:require [bucklew.entities :as ents]
            [bucklew.components :as comps]))

(def player-components
  [(comps/Physics {:max-hp 20 :hp 20})
   (comps/CanAttack)
   (comps/Inventory)
   (comps/Equipment)
   (comps/TakesTurn {:tick comps/players-tick})
   (comps/Display {:glyph \@})
   (comps/Debug)])

(defn make-player
  "Given a location, create a new player."
  [location]
  (let [without-location (ents/map->Entity {:id 1, :nomen "Player", :components player-components})
        player (ents/add-component {comps/Location location})]
    player))
