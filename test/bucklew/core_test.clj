(ns bucklew.core-test
  (:require [clojure.test :refer :all]
            [bucklew.components :as components]
            [bucklew.entities :as entities]
            [bucklew.events :as events]
            [bucklew.helpers :as help]))

(deftest a-test
    ; create the character
    (def player (entities/map->Entity
      {:id 1
       :nomen "Player"
       :components [(components/physics 20)]}))
    (def physics (help/find-physics-component (:components player)))
    (is (= (str player) "Player. 20/20 HP."))
    (is (= (:id player) 1))
    (is (= (:nomen player) "Player"))
    (is (= (count (:components player)) 1))
    (is (= (:hp physics) 20))
    (is (= (:priority physics) 100))
    (def player (assoc player :desc "You, the player character."))
    (is (= (str player) "Player. You, the player character. 20/20 HP."))

    ; attack for 5
    (def attacked-player (entities/receive-event player events/take-damage-event))
    (is (not= player attacked-player))
    (def physics (help/find-physics-component (:components attacked-player)))
    (is (= (:hp physics) 15))

    ; add a strength 2 armour component
    (def player (entities/add-component attacked-player (components/armour)))
    (def physics (help/find-physics-component (:components player)))
    (is (= (count (:components player)) 2))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:priority (first (:components player))) 50))
    (is (= (:nomen (last (:components player))) :physics))
    (is (= (:strength (first (:components player))) 2))
    (is (= (:hp physics) 15))

    ; attack for 5
    (def player (entities/receive-event player events/take-damage-event))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 12))

    ; put on another strength 2 armour component
    (def player (entities/add-component player (components/armour)))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 12))
    (is (= (count (:components player)) 3))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:nomen (nth (:components player) 1)) :armour))
    (is (= (:nomen (last (:components player))) :physics))
    
    ; attack for 5
    (def player (entities/receive-event player events/take-damage-event))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 11))

    ; put on a strength 1000 armour component
    (def player (entities/add-component player (components/armour 1000)))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 11))
    (is (= (count (:components player)) 4))
    (is (= (:nomen (first (:components player))) :armour))
    (is (= (:nomen (nth (:components player) 1)) :armour))
    (is (= (:nomen (nth (:components player) 2)) :armour))
    (is (= (:nomen (last (:components player))) :physics))

    ; attack for 5
    (def player (entities/receive-event player events/take-damage-event))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 11))
    
    ; remove armour
    (def player (entities/remove-components-by-nomen player :armour))
    (is (= (count (:components player)) 1))
    
    ; attack for 5
    (def player (entities/receive-event player events/take-damage-event))
    (def physics (help/find-physics-component (:components player)))
    (is (= (:hp physics) 6))
    
    ; remove all components
    (def empty-player (entities/clear-components player))
    (is (= (str empty-player) "Player. You, the player character."))
    
    ; attack for 5
    (def empty-attacked-player (entities/receive-event empty-player events/take-damage-event))
    (is (= empty-player empty-attacked-player))
    )
