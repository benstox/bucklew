(ns bucklew.core
  (:require
    [bucklew.udp :as udp]
    [bucklew.components :as components]
    [bucklew.events :as events]
    [bucklew.entities :as entities]))

(def x 1)

(def player (entities/map->Entity
  {:id 1
   :nomen :player
   :components [(components/physics-component 20)]}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def felis {:likes-dogs true :ocd-bathing true})
  (def morris (udp/beget {:likes-9lives true} felis))
  (def post-traumatic-morris (udp/beget {:likes-dogs nil} morris))
  (println (udp/get felis :likes-dogs))
  (println (udp/get morris :ocd-bathing))
  (println (udp/get morris :likes-dogs))
  (println (udp/get post-traumatic-morris :likes-dogs))
  (println (udp/get post-traumatic-morris :likes-other-cats))
  )
