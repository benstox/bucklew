(ns bucklew.entities
  (:require [bucklew.helpers :as help]))

(defprotocol EntityProtocol
  (add-component [this component])
  (receive-event [this event]))

(defrecord Entity [id nomen components]
  EntityProtocol
  (add-component [this component]
    "Add a component to the entities component list and re-sort."
    (let [new-components (cons component components)
          prioritised (into [] (sort-by :priority new-components))]
    (assoc this :components prioritised)))
  (receive-event [this event]
    "Handle the reception of an event by the entity."
    (let [nomen (:nomen event)]
      (if-let [relevant-components (filter nomen components)]
        (loop [this this
               event event
               relevant-components (filter nomen components)]
          (let [component (first relevant-components)
                lower-priority (rest relevant-components)
                component-fn (nomen component)
                [new-this new-event] (component-fn this event component)]
            (if (empty? lower-priority)
              new-this
              (recur new-this new-event lower-priority))))
        this)))
  Object
  (toString [this]
    "Override str method."
    (let [nomen (:nomen this)
          nomen-is-physics (partial help/nomen-is :physics)
          physics (help/find-first nomen-is-physics (:components this))
          hp (:hp physics)]
      (str nomen ": " hp " HP"))))
