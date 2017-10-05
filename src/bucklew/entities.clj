(ns bucklew.entities
  (:require [bucklew.helpers :as help]))

(defprotocol EntityProtocol
  (add-component [this component])
  (sort-components [this])
  (clear-components [this])
  (remove-components-by-nomen [this nomen])
  (receive-event [this event]))

(defrecord Entity [id nomen desc components]
  EntityProtocol
  (add-component [this component]
    "Add a component to the entities component list and re-sort."
    (let [new-components (cons component components)
          prioritised (into [] (sort-by :priority new-components))]
    (assoc this :components prioritised)))
  (sort-components [this]
    "Resorts components by priority."
    (assoc this :components (into [] (sort-by :priority (:components this)))))
  (clear-components [this]
    "Clear all components."
    (assoc this :components []))
  (remove-components-by-nomen [this nomen]
    "Remove all components with a certain name."
    (let [nomen-is-nomen (partial help/nomen-is nomen)]
      (assoc this :components (into [] (remove nomen-is-nomen (:components this))))))
  (receive-event [this event]
    "Handle the reception of an event by the entity. Return the entity and the event."
    (let [nomen (:nomen event)
          indexed-components (map vector components (range))
          relevant-components (filter (comp nomen first) indexed-components)]
      (if (not-empty relevant-components)
        (loop [this this
               event event
               relevant-components relevant-components]
          (let [[component component-i] (first relevant-components)
                lower-priority-components (rest relevant-components)
                component-fn (nomen component)
                [new-this new-event] (component-fn this event component-i)]
            (if (empty? lower-priority-components)
              [new-this new-event]  ; return a (possibly) changed entity and event
              (recur new-this new-event lower-priority-components))))
        [this event])))             ; no change to either the entity or the event
  Object
  (toString [this]
    "Override str method."
    (let [nomen (:nomen this)
          desc (:desc this)
          physics (help/find-physics-component (:components this))
          hp (:hp physics)
          max-hp (:max-hp physics)]
      (str nomen "." (when desc (str " " desc)) (when physics (str " " hp "/" max-hp " HP."))))))
