(ns bucklew.ai
  (require [bucklew.entities :as ents]
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
  "Walk in a tight circle."
  [this event component-i]
  (let [takes-turn (-get this component-i)
        last-turn (get-in takes-turn [:data :last-turn])
        this-turn (mod (inc last-turn) 4)
        this-turn-direction (get dervish-direction-seq this-turn)
        move-event (events/map->Event {:nomen :move
                                       :data {:direction direction
                                              :tiles tiles
                                              :entities entities}})
        [new-this event] (ents/receive-event this move-event)]
    [new-this event]))
