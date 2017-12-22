(ns bucklew.experiments
  (:require [bucklew.components :as comps]
            [bucklew.creatures :as creats]
            [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.items :as items]
            [bucklew.world.core :as world-core]
            [com.rpl.specter :as specter :refer [select setval transform walker ALL FIRST INDEXED-VALS LAST MAP-VALS NONE]]
            [ebenbild.core :as eb]))

(require '[bucklew.components :as comps]
         '[bucklew.creatures :as creats]
         '[bucklew.entities :as ents]
         '[bucklew.events :as events]
         '[bucklew.items :as items]
         '[bucklew.world.core :as world-core]
         '[com.rpl.specter :as specter :refer [select select-one selected? setval transform walker ALL FIRST INDEXED-VALS LAST MAP-VALS NONE]]
         '[ebenbild.core :as eb])


(def player (creats/make-player {:x 1 :y 1}))

(def dervish (creats/whirling-dervish {:x 2 :y 1}))

(def wall (:wall world-core/tiles))
(def floor (:floor world-core/tiles))
(def tiles [[wall wall  wall  wall]
            [wall floor floor wall]
            [wall wall  floor wall]
            [wall wall  wall  wall]])

(def world (world-core/map->World {:entities [player dervish] :tiles tiles}))

(def this player)
(def entity-i 0)
(def event (assoc events/move :data {:world world, :direction :e}))
(def component-i 0)

(events/fire-event event game entity-i)


(def player-i 0)
(def player (ents/sort-components
  (ents/map->Entity {
    :id 1
    :nomen "Player"
    :components [
      (comps/Armour {:strength 2})
      (comps/Physics {:max-hp 20 :hp 20})
      (comps/CanAttack)
      (comps/Inventory)
      (comps/Equipment)]})))
(def boots (ents/map->Entity {:nomen "Boots"
                              :components [(comps/Armour)
                                           (comps/CanBeEquipped {:slot-types #{:feet}})
                                           (comps/Location)]}))
(def game {:world {:entities [player boots items/claymore]}})
; (def inventory (select-one [:components ALL (eb/like {:nomen :inventory}) :contents ALL] player))
(def add-next-item (events/map->Event {:nomen :add-item :data {:item-i 1}}))
(let [[game event] (events/fire-event add-next-item game player-i)
      [game event] (events/fire-event add-next-item game player-i)]
  (def player (get-in game [:world :entities player-i])))
(def equipment (specter/select-one [:components ALL (eb/like {:nomen :equipment})] player))

(specter/select-one
  [:components
   INDEXED-VALS
   (selected? 1 (eb/like {:nomen :can-be-equapped}))] items/sword)
