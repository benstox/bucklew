(ns bucklew.core-test
  (:require [clojure.test :refer :all]
      [bucklew.components :as components]
      [bucklew.entities :as entities]
      [bucklew.events :as events]
      [bucklew.helpers :as help]))

(deftest basic-test
  "Test some basics surrounding entities, components and events."
  ; create the character
  (def player (entities/map->Entity
    {:id 1
     :nomen "Player"
     :components [(components/Physics {:hp 20 :max-hp 20})]}))
  (def physics (help/find-physics-component (:components player)))
  (is (= (str player) "Player. 20/20 HP."))
  (is (= (:id player) 1))
  (is (= (:nomen player) "Player"))
  (is (= (count (:components player)) 1))
  (is (= (:max-hp physics) 20))
  (is (= (:hp physics) 20))
  (is (= (:priority physics) 100))
  (def player (assoc player :desc "You, the player character."))
  (is (= (str player) "Player. You, the player character. 20/20 HP."))

  ; attack for 5
  (let [[attacked-player event] (entities/receive-event player events/take-damage-event)]
    (is (not= player attacked-player))
    (def physics (help/find-physics-component (:components attacked-player)))
    (is (= (:max-hp physics) 20))
    (is (= (:hp physics) 15))

    ; add a strength 2 armour component
    (let [player (entities/add-component attacked-player (components/Armour))]
      (def physics (help/find-physics-component (:components player)))
      (is (= (count (:components player)) 2))
      (is (= (:nomen (first (:components player))) :armour))
      (is (= (:priority (first (:components player))) 50))
      (is (= (:nomen (last (:components player))) :physics))
      (is (= (:strength (first (:components player))) 2))
      (is (= (:hp physics) 15))

      ; attack for 5
      (let [[player event] (entities/receive-event player events/take-damage-event)]
        (def physics (help/find-physics-component (:components player)))
        (is (= (:hp physics) 12))

        ; put on another strength 2 armour component
        (let [player (entities/add-component player (components/Armour))]
          (def physics (help/find-physics-component (:components player)))
          (is (= (:hp physics) 12))
          (is (= (count (:components player)) 3))
          (is (= (:nomen (first (:components player))) :armour))
          (is (= (:nomen (nth (:components player) 1)) :armour))
          (is (= (:nomen (last (:components player))) :physics))
              
          ; attack for 5
          (let [[player event] (entities/receive-event player events/take-damage-event)]
            (def physics (help/find-physics-component (:components player)))
            (is (= (:hp physics) 11))

            ; put on a strength 1000 armour component
            (let [player (entities/add-component player (components/Armour {:strength 1000}))]
              (def physics (help/find-physics-component (:components player)))
              (is (= (:hp physics) 11))
              (is (= (count (:components player)) 4))
              (is (= (:nomen (first (:components player))) :armour))
              (is (= (:nomen (nth (:components player) 1)) :armour))
              (is (= (:nomen (nth (:components player) 2)) :armour))
              (is (= (:nomen (last (:components player))) :physics))

              ; attack for 5
              (let [[player event] (entities/receive-event player events/take-damage-event)]
                (def physics (help/find-physics-component (:components player)))
                (is (= (:hp physics) 11))
                          
                ; remove armour
                (let [player (entities/remove-components-by-nomen player :armour)]
                  (is (= (count (:components player)) 1))
                              
                  ; attack for 5
                  (let [[player event] (entities/receive-event player events/take-damage-event)]
                    (def physics (help/find-physics-component (:components player)))
                    (is (= (:hp physics) 6))
                                  
                    ; remove all components
                    (def empty-player (entities/clear-components player))
                    (is (= (str empty-player) "Player. You, the player character."))
                                  
                    ; attack for 5
                    (let [[empty-attacked-player event] (entities/receive-event empty-player events/take-damage-event)]
                      (is (= empty-player empty-attacked-player))
  ))))))))))
  )

(deftest sort-components
  "Test whether components sort properly by priority."
  (def player (entities/map->Entity
    {:id 1
     :nomen "Player"
     :components [(components/Physics)
                  (components/Armour)]}))
  (is (= (:priority (first (:components player))) 100))
  (is (= (:priority (last (:components player))) 50))
  (def player (entities/sort-components player))
  (is (= (:priority (last (:components player))) 100))
  (is (= (:priority (first (:components player))) 50)))

(deftest basic-attack
  "Test basic attacking."
  (def player (entities/sort-components
    (entities/map->Entity {
      :id 1
      :nomen "Player"
      :components [
        (components/Armour {:strength 2})
        (components/Physics {:max-hp 20 :hp 20})
        (components/CanAttack)]})))
  (def warrior (entities/sort-components
    (entities/map->Entity {
      :id 2
      :nomen "Warrior"
      :components [
        (components/Armour {:strength 1})
        (components/Physics {:max-hp 20 :hp 20})
        (components/CanAttack)]})))
  ; make the player attack the warrior
  (let [make-attack-event (assoc events/make-attack-event :target warrior)
        [player {:keys [nomen amount type target] :as make-attack-event-after} :as result] (entities/receive-event player make-attack-event)
        [target-entity take-damage-event] target
        player-physics (help/find-physics-component (:components player))
        player-hp (:hp player-physics)
        target-physics (help/find-physics-component (:components target-entity))
        target-hp (:hp target-physics)]
    (is (= target-hp 16))
    (is (= player-hp 20)))
  )
