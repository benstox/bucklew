(ns bucklew.core-test
  (:require [clojure.test :refer :all]
            [bucklew.components :as components]
            [bucklew.entities :as entities]
            [bucklew.events :as events]
            [bucklew.helpers :as help]))

(deftest a-test
    (def player (entities/map->Entity
      {:id 1
       :nomen :player
       :components [(components/physics-component 20)]}))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:id player) 1))
    (is (= (:nomen player) :player))
    (is (= (count (:components player)) 1))
    (is (= (:hp physics) 20))
    (is (= (:priority physics) 100))
    ; attack for 5
    (def player (entities/receive-event player events/attack-event))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 15))

    ; add a strength 2 armour component
    (def player (entities/add-component player (components/armour-component)))
    (def physics (components/get-physics-component (:components player)))
    (is (= (count (:components player)) 2))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:priority (first (:components player))) 50))
    (is (= (:nomen (last (:components player))) :physics))
    (is (= (:strength (first (:components player))) 2))
    (is (= (:hp physics) 15))

    ; attack for 5
    (def player (entities/receive-event player events/attack-event))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 12))

    ; put on another strength 2 armour component
    (def player (entities/add-component player (components/armour-component)))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 12))
    (is (= (count (:components player)) 3))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:nomen (nth (:components player) 1)) :armour))
    (is (= (:nomen (last (:components player))) :physics))
    
    ; attack for 5
    (def player (entities/receive-event player events/attack-event))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 11))

    ; put on a strength 1000 armour component
    (def player (entities/add-component player (components/armour-component 1000)))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 11))
    (is (= (count (:components player)) 4))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:nomen (nth (:components player) 1)) :armour))
    (is (= (:nomen (nth (:components player) 2)) :armour))
    (is (= (:nomen (last (:components player))) :physics))

    ; attack for 5
    (def player (entities/receive-event player events/attack-event))
    (def physics (components/get-physics-component (:components player)))
    (is (= (:hp physics) 11))
    )
