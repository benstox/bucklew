(ns bucklew.items
  (:require [bucklew.entities :as ents]
            [bucklew.components :as comps]))

(def sword (ents/map->Entity {:nomen "Sword"
                              :components [(comps/Blade)
                                           (comps/CanBeEquipped)]}))

(def shield (ents/map->Entity {:nomen "Shield"
                               :components [(comps/Armour)
                                            (comps/CanBeEquipped)]}))
