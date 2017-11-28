(ns bucklew.ai
  (:require [bucklew.entities :as ents]
           [bucklew.events :as events]))

(def dervish-direction-seq [:n :e :s :w])

(defn dervish
  "Walk in a tight circle. Store the direction of the last move made in
  behind the data key of the TakeTurn component."
  [game entity-i component-i event]
  (let [{{tiles :tiles entities :entities} :world :as game} game
        takes-turn-comp (get-in entities [entity-i :components component-i])
        last-turn (get-in takes-turn-comp [:data :last-turn])
        this-turn (mod (inc last-turn) 4)
        this-turn-direction (get dervish-direction-seq this-turn)
        move-event (events/map->Event {:nomen :move
                                       :data {:direction this-turn-direction
                                              :tiles tiles
                                              :entities entities}})
        [new-game move-event] (events/fire-event move-event game entity-i)
        new-takes-turn-comp (assoc-in takes-turn-comp [:data :last-turn] this-turn)
        new-game (assoc-in game [:world :entities entity-i :components component-i] new-takes-turn-comp)]
    [new-game event]))
