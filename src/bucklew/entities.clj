(ns bucklew.entities
  (:require [bucklew.helpers :as help]))

(defprotocol EntityProtocol
  (add-component [this component])
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
  (clear-components [this]
    "Clear all components."
    (assoc this :components []))
  (remove-components-by-nomen [this nomen]
    "Remove all components with a certain name."
    (let [nomen-is-nomen (partial help/nomen-is nomen)]
      (assoc this :components (into [] (remove nomen-is-nomen (:components this))))))
  (receive-event [this event]
    "Handle the reception of an event by the entity."
    (let [nomen (:nomen event)
          target (:target event)
          relevant-components (filter nomen components)]
      (if (not-empty relevant-components)
        (loop [this this
               event event
               target target
               relevant-components relevant-components]
          (let [component (first relevant-components)
                lower-priority (rest relevant-components)
                component-fn (nomen component)
                [new-this new-event new-target] (component-fn this event component)]
            (if (empty? lower-priority)
              new-this
              (recur new-this new-event lower-priority))))
        this)))
  Object
  (toString [this]
    "Override str method."
    (let [nomen (:nomen this)
          desc (:desc this)
          physics (help/find-physics-component (:components this))
          hp (:hp physics)
          max-hp (:max-hp physics)]
      (str nomen "." (when desc (str " " desc)) (when physics (str " " hp "/" max-hp " HP."))))))
