(ns bucklew.ai
  (:require [bucklew.entities :as ents]
           [bucklew.events :as events]))

(defn -get
  "Given 'this' and 'component-i' get the current component."
  [this component-i]
  (get-in this [:components component-i]))

(defn -set
  "Given 'this' and 'component-i' set the current component to one with new values in it."
  [this component-i new-component]
  (assoc-in this [:components component-i] new-component))

(def dervish-direction-seq [:n :e :s :w])

(defn dervish
  "Walk in a tight circle. Store the direction of the last move made in
  behind the data key of the TakeTurn component."
  [this event component-i]
  (let [{:keys [tiles entities]} (:data event)
        takes-turn (-get this component-i)
        last-turn (get-in takes-turn [:data :last-turn])
        this-turn (mod (inc last-turn) 4)
        this-turn-direction (get dervish-direction-seq this-turn)
        move-event (events/map->Event {:nomen :move
                                       :data {:direction this-turn-direction
                                              :tiles tiles
                                              :entities entities}})
        [new-this move-event] (ents/receive-event this move-event)
        new-takes-turn (assoc-in takes-turn [:data :last-turn] this-turn)
        new-this (-set new-this component-i new-takes-turn)]
    [new-this event]))
