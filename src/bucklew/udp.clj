(ns bucklew.udp
  (:refer-clojure :exclude [get]))

"The Joy of Clojure, 2nd Edition, Section 9.2 Exploring Clojure multimethods with the Universal Design Pattern"

(defn beget [this proto]
  (assoc this ::prototype proto))

(defn get [m k]
  (when m
    (if-let [[_ v] (find m k)]
      v
      (recur (::prototype m) k))))

(def put assoc)

(def clone (partial beget {}))
