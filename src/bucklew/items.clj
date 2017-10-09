(ns bucklew.items
  (:require [bucklew.entities :as ents]
            [bucklew.components :as comps]))

(def sword (ents/map->Entity {:nomen "Sword"
                              :components [(comps/Blade)
                                           (comps/CanBeEquipped)
                                           (comps/Location)]}))

(def claymore (ents/map->Entity {:nomen "Claymore"
                              :components [(comps/Blade {:strength 10})
                                           (comps/CanBeEquipped {:slots-required 2})
                                           (comps/Location)]}))

(def shield (ents/map->Entity {:nomen "Shield"
                               :components [(comps/Armour)
                                            (comps/CanBeEquipped)
                                            (comps/Location)]}))

(def pizza (ents/map->Entity {:nomen "Pizza"
	                          :components [comps/Location]}))
