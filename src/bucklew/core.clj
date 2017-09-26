(ns bucklew.core
  (:require [bucklew.udp :as udp]))

(def x 1)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def felis {:likes-dogs true :ocd-bathing true})
  (def morris (udp/beget {:likes-9lives true} felis))
  (def post-traumatic-morris (udp/beget {:likes-dogs nil} morris))
  (println (udp/udp-get felis :likes-dogs))
  (println (udp/udp-get morris :ocd-bathing))
  (println (udp/udp-get morris :likes-dogs))
  (println (udp/udp-get post-traumatic-morris :likes-dogs))
  (println (udp/udp-get post-traumatic-morris :likes-other-cats))
  )
