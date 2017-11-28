(ns bucklew.experiments
  (:require [bucklew.creatures :as creats]
            [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.world.core :as world-core]
            [com.rpl.specter :as specter :refer [select transform ALL]]))

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
