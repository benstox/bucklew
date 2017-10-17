(ns bucklew.entities
  (:require [bucklew.events :as events]
            [bucklew.helpers :as help]))

(defprotocol EntityProtocol
  (add-component [this component]
    "Add a component to the entities component list and re-sort.")
  (sort-components [this]
    "Resorts components by priority.")
  (clear-components [this]
    "Clear all components.")
  (remove-components-by-nomen [this nomen]
    "Remove all components with a certain name.")
  (get-components-by-nomen [this nomen]
    "Return a sequence of components that share the given name.")
  (get-indexed-components-by-nomen [this nomen]
    "Return a sequence of components that share the given name.")
  (receive-event [this event]
    "Handle the reception of an event by this. Return the possibly modified this and event.
    Tends to involve passing the event through all the entities or components of this.
    
    For world (for now...):
    * located-entities: `([entity event-containing-location-data], ...)`
    * indexed-entities: `([entity-i [entity event-containing-location-data]], ...)`")
  (tick [this world entity-i]
    "Update the world to handle the passing of a tick for this entity."))

(defrecord Entity [id nomen desc components]
  EntityProtocol
  (add-component [this component]
    (let [new-components (cons component components)
          prioritised (vec (sort-by :priority new-components))]
    (assoc this :components prioritised)))
  (sort-components [this]
    (assoc this :components (vec (sort-by :priority (:components this)))))
  (clear-components [this]
    (assoc this :components []))
  (remove-components-by-nomen [this nomen]
    (let [nomen-is-nomen (partial help/nomen-is nomen)]
      (assoc this :components (vec (remove nomen-is-nomen (:components this))))))
  (get-components-by-nomen [this nomen]
    (let [nomen-is-nomen (partial help/nomen-is nomen)]
      (filter nomen-is-nomen (:components this))))
  (get-indexed-components-by-nomen [this nomen]
    (let [nomen-is-nomen (partial help/nomen-is nomen)]
      (filter (comp nomen-is-nomen first) (map vector (:components this) (range)))))
  (receive-event [this event]
    (let [nomen (:nomen event)
          indexed-components (help/enumerate components)
          relevant-components (filter (comp nomen second) indexed-components)]
      (if (not-empty relevant-components)
        (loop [this this
               event event
               relevant-components relevant-components]
          (let [[component-i component] (first relevant-components)
                lower-priority-components (rest relevant-components)
                component-fn (nomen component)
                [new-this new-event] (component-fn this event component-i)]
            (if (empty? lower-priority-components)
              [new-this new-event]  ; return a (possibly) changed entity and event
              (recur new-this new-event lower-priority-components))))
        [this event])))             ; no change to either the entity or the event
  (tick [this world entity-i]
    (let [[new-this new-event] (receive-event this events/tick)
          new-world (assoc-in world [:entities entity-i] new-this)]
      new-world))
  Object
  (toString [this]
    (str
      nomen "." (when desc (str " " desc)) "\n"
      (clojure.string/join (for [component components] (str component "\n"))))))
