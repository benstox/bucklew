(ns bucklew.core-test
  (:require [clojure.test :refer :all]
      [bucklew.components :as comps]
      [bucklew.entities :as ents]
      [bucklew.events :as events]
      [bucklew.items :as items]
      [bucklew.helpers :as help]
      [bucklew.creatures :as creats]
      [bucklew.world.core :as world-core]
      [com.rpl.specter :as specter :refer [select select-one selected? setval transform walker ALL FIRST INDEXED-VALS LAST MAP-VALS NONE]]
      [ebenbild.core :as eb]))

(deftest basic-test
  ; Test some basics surrounding entities, components and events.
  ; create the character
  (def player-i 0)
  (def player (ents/map->Entity
    {:id 1
     :nomen "Player"
     :components [(comps/Physics {:hp 20 :max-hp 20})]}))
  (def game {:world {:entities [player]}})
  (def physics (help/find-physics-component (:components player)))
  (is (= (str player) "Player.\nStrength: 5; HP: 20/20\n"))
  (is (= (:id player) 1))
  (is (= (:nomen player) "Player"))
  (is (= (count (:components player)) 1))
  (is (= (:max-hp physics) 20))
  (is (= (:hp physics) 20))
  (is (= (:priority physics) 100))
  (def player (assoc player :desc "You, the player character."))
  (is (= (str player) "Player. You, the player character.\nStrength: 5; HP: 20/20\n"))

  ; attack for 5
  (let [[game event] (events/fire-event events/take-damage-event game player-i)
        attacked-player (get-in game [:world :entities player-i])
        physics (help/find-physics-component (:components attacked-player))]
    (is (not= player attacked-player))
    (is (= (:max-hp physics) 20))
    (is (= (:hp physics) 15))

    ; add a strength 2 armour component
    (let [game (assoc-in game [:world :entities player-i] (ents/add-component attacked-player (comps/Armour)))
          player (get-in game [:world :entities player-i])
          physics (help/find-physics-component (:components player))]
      (is (= (count (:components player)) 2))
      (is (= (:nomen (first (:components player))) :armour))
      (is (= (:priority (first (:components player))) 50))
      (is (= (:nomen (last (:components player))) :physics))
      (is (= (:strength (first (:components player))) 2))
      (is (= (:hp physics) 15))

      ; attack for 5
      (let [[game event] (events/fire-event events/take-damage-event game player-i)
            player (get-in game [:world :entities player-i])
            physics (help/find-physics-component (:components player))]
        (is (= (:hp physics) 12))

        ; put on another strength 2 armour component
        (let [game (assoc-in game [:world :entities player-i] (ents/add-component player (comps/Armour)))
              player (get-in game [:world :entities player-i])
              physics (help/find-physics-component (:components player))]
          (is (= (:hp physics) 12))
          (is (= (count (:components player)) 3))
          (is (= (:nomen (first (:components player))) :armour))
          (is (= (:nomen (nth (:components player) 1)) :armour))
          (is (= (:nomen (last (:components player))) :physics))

          ; attack for 5
          (let [[game event] (events/fire-event events/take-damage-event game player-i)
                player (get-in game [:world :entities player-i])
                physics (help/find-physics-component (:components player))]
            (is (= (:hp physics) 11))

            ; put on a strength 1000 armour component
            (let [game (assoc-in game [:world :entities player-i] (ents/add-component player (comps/Armour {:strength 1000})))
                  player (get-in game [:world :entities player-i])
                  physics (help/find-physics-component (:components player))]
              (is (= (:hp physics) 11))
              (is (= (count (:components player)) 4))
              (is (= (:nomen (first (:components player))) :armour))
              (is (= (:nomen (nth (:components player) 1)) :armour))
              (is (= (:nomen (nth (:components player) 2)) :armour))
              (is (= (:nomen (last (:components player))) :physics))

              ; attack for 5
              (let [[game event] (events/fire-event events/take-damage-event game player-i)
                    player (get-in game [:world :entities player-i])
                    physics (help/find-physics-component (:components player))]
                (is (= (:hp physics) 11))

                ; remove armour
                (let [game (assoc-in game [:world :entities player-i] (ents/remove-components-by-nomen player :armour))
                      player (get-in game [:world :entities player-i])]
                  (is (= (count (:components player)) 1))

                  ; attack for 5
                  (let [[game event] (events/fire-event events/take-damage-event game player-i)
                        player (get-in game [:world :entities player-i])
                        physics (help/find-physics-component (:components player))]
                    (is (= (:hp physics) 6))

                    ; remove all components
                    (def empty-player (ents/clear-components player))
                    (let [game (assoc-in game [:world :entities player-i] empty-player)
                          empty-player (get-in game [:world :entities player-i])]
                      (is (= (str empty-player) "Player.\n"))

                      ; attack for 5
                      (let [[game event] (events/fire-event events/take-damage-event game player-i)
                            empty-attacked-player (get-in game [:world :entities player-i])]
                        (is (= empty-player empty-attacked-player))
  )))))))))))
  )

(deftest sort-components
  ; Test whether components sort properly by priority.
  (def player (ents/map->Entity
    {:id 1
     :nomen "Player"
     :components [(comps/Physics)
                  (comps/Armour)]}))
  (is (= (:priority (first (:components player))) 100))
  (is (= (:priority (last (:components player))) 50))
  (def player (ents/sort-components player))
  (is (= (:priority (last (:components player))) 100))
  (is (= (:priority (first (:components player))) 50)))

(deftest basic-attack
  ; Test basic attacking.
  (def player-i 0)
  (def warrior-i 1)
  (def player (ents/sort-components
    (ents/map->Entity {
      :id 1
      :nomen "Player"
      :components [
        (comps/Armour {:strength 2})
        (comps/Physics {:max-hp 20 :hp 20})
        (comps/CanAttack)]})))
  (def warrior (ents/sort-components
    (ents/map->Entity {
      :id 2
      :nomen "Warrior"
      :components [
        (comps/Armour {:strength 1})
        (comps/Physics {:max-hp 20 :hp 20})
        (comps/CanAttack)]})))
  (def game {:world {:entities [player warrior]}})
  ; make the player attack the warrior
  (let [make-attack-event (assoc-in events/make-attack-event [:data :target-i] warrior-i)
        [game make-attack-event-after] (events/fire-event make-attack-event game player-i)
        warrior (get-in game [:world :entities warrior-i])
        player (get-in game [:world :entities player-i])
        player-physics (help/find-physics-component (:components player))
        player-hp (:hp player-physics)
        warrior-physics (help/find-physics-component (:components warrior))
        warrior-hp (:hp warrior-physics)]
    (is (= warrior-hp 16))
    (is (= player-hp 20)))
  )

(deftest add-item
  ; Test add-item events
  (def player-i 0)
  (def item-i 1)
  (def player (ents/sort-components
    (ents/map->Entity {
      :id 1
      :nomen "Player"
      :components [
        (comps/Armour {:strength 2})
        (comps/Physics {:max-hp 20 :hp 20})
        (comps/CanAttack)
        (comps/Inventory)
        (comps/Equipment)]})))
  ; there are four pizzas and a sword in the game
  (def game {:world {:entities [player
                                items/pizza
                                items/pizza
                                items/pizza
                                items/pizza
                                items/sword]}})
  (is (= (count (get-in game [:world :entities])) 6))
  (def add-next-item (events/map->Event {:nomen :add-item :data {:item-i item-i}}))
  ; add a pizza to the player's inventory
  (let [[game exhausted-event] (events/fire-event add-next-item game player-i)
        player (get-in game [:world :entities player-i])
        inventory-contents (select [:components ALL (eb/like {:nomen :inventory}) :contents ALL] player)]
    (is (nil? (get-in [:data :item] exhausted-event)))
    (is (= (count inventory-contents) 1))
    (is (= (:nomen (first inventory-contents)) "Pizza"))
    (is (= (count (get-in game [:world :entities])) 5))
    ; add three more pizzas to the player's inventory
    (let [[game event] (events/fire-event add-next-item game player-i)
          [game event] (events/fire-event add-next-item game player-i)
          [game event] (events/fire-event add-next-item game player-i)
          player (get-in game [:world :entities player-i])
          inventory-contents (select [:components ALL (eb/like {:nomen :inventory}) :contents ALL] player)]
      (is (= (count inventory-contents) 4))
      (is (= (count (get-in game [:world :entities])) 2))
      ; add and equip a sword into the right hand
      (let [[game event] (events/fire-event add-next-item game player-i)
            player (get-in game [:world :entities player-i])
            inventory-contents (select [:components ALL (eb/like {:nomen :inventory}) :contents ALL] player)
            equipment (select-one [:components ALL (eb/like {:nomen :equipment})] player)]
        (is (= (count inventory-contents) 4))
        (is (= (count (:contents equipment)) 1))
        (is (= (count (get-in game [:world :entities])) 1))
        (is (= (:nomen (first (:contents equipment))) "Sword"))
        (let [location-components (select [:contents FIRST :components ALL (eb/like {:nomen :location})] equipment)
              sword-equipped-in (select-one [:contents FIRST :components ALL (selected? #(contains? % :equipped-in)) :equipped-in] equipment)]
          (is (empty? location-components))
          (is (= (count sword-equipped-in) 1))
          (is (= (first sword-equipped-in) :right-hand))
          (println "#############################")
          (println "####### ADD ITEM TEST #######")
          (println "#############################")
          (println (str player))
          (println "#############################")
  )))))

(deftest drawing-entities
  ; test getting the drawing data from an entity
  (def player-i 0)
  (def player (creats/make-player {:x 185 :y -33}))
  (def game {:world {:entities [player]}})
  (let [[game draw-event] (events/fire-event events/draw game player-i)
        {:keys [x y glyph fg-colour bg-colour]} (:data draw-event)]
    (is (= x 185))
    (is (= y -33))
    (is (= glyph "@"))
    (is (= fg-colour "ffffff"))
    (is (= bg-colour "000000")))
  )

(deftest move-player
  ; test issuing a move command to a player on a simple map
  ; ####
  ; #@.#
  ; ##.#
  ; ####
  (def wall (:wall world-core/tiles))
  (def floor (:floor world-core/tiles))
  (def tiles [[wall wall  wall  wall]
              [wall floor floor wall]
              [wall wall  floor wall]
              [wall wall  wall  wall]])
  (def player (creats/make-player {:x 1 :y 1}))
  (def entities [player])
  (def world (world-core/map->World {:entities entities :tiles tiles}))
  (def game {:world world})
  (defn move-event
    [direction]
    (events/map->Event {:nomen :move :data {:direction direction}}))
  (def nomen-is-location (partial help/nomen-is :location))
  (def location (select-one [:components ALL (eb/like {:nomen :location})] player))
  (is (= (:x location) 1))
  (is (= (:y location) 1))
  ; move east and you should get {:x 2 :y 1}
  (println "Firing the first MOVE EVENT ....")
  (let [[game event] (events/fire-event (move-event :e) game player-i)
        player (get-in game [:world :entities player-i])
        location (select-one [:components ALL (eb/like {:nomen :location})] player)]
    (println (str "after the move " player))
    (is (= (:x location) 2))
    (is (= (:y location) 1))
    ; try to move north and you should stay put
    (let [[game event] (events/fire-event (move-event :n) game player-i)
          player (get-in game [:world :entities player-i])
          location (select-one [:components ALL (eb/like {:nomen :location})] player)]
      (is (= (:x location) 2))
      (is (= (:y location) 1))
      ; move south and you should get {:x 2 :y 2}
      (let [[game event] (events/fire-event (move-event :s) game player-i)
            player (get-in game [:world :entities player-i])
            location (select-one [:components ALL (eb/like {:nomen :location})] player)]
        (is (= (:x location) 2))
        (is (= (:y location) 2))
        ; try to move west and you should stay put
        (let [[game event] (events/fire-event (move-event :w) game player-i)
              player (get-in game [:world :entities player-i])
              location (select-one [:components ALL (eb/like {:nomen :location})] player)]
          (is (= (:x location) 2))
          (is (= (:y location) 2))
  ))))
  )

; (deftest whirling-dervish
;   ; test out the whirling dervish AI, should "spin" in a tight circle
;   (def wall (:wall world-core/tiles))
;   (def floor (:floor world-core/tiles))
;   (def tiles [[wall wall  wall  wall]
;               [wall floor floor wall]
;               [wall floor  floor wall]
;               [wall wall  wall  wall]])
;   (def dervish (creats/whirling-dervish {:x 2 :y 2}))
;   (def entities [dervish])
;   (def game {:world {:entities entities :tiles tiles}})
;   (def tick-event (assoc events/tick :data game))
;   (def nomen-is-location (partial help/nomen-is :location))
;   (def location (first (filter nomen-is-location (:components dervish))))
;   (is (= (:x location) 2))
;   (is (= (:y location) 2))
;   ; time passes, the dervish tries to whirl east but can't
;   (let [[dervish event] (ents/receive-event dervish tick-event)
;         location (first (filter nomen-is-location (:components dervish)))]
;     (is (= (:x location) 2))
;     (is (= (:y location) 2))
;     ; time passes, the dervish tries to whirl east but can't
;     (let [[dervish event] (ents/receive-event dervish tick-event)
;           location (first (filter nomen-is-location (:components dervish)))]
;       (is (= (:x location) 2))
;       (is (= (:y location) 2))
;       ; time passes, the dervish whirls west
;       (let [[dervish event] (ents/receive-event dervish tick-event)
;             location (first (filter nomen-is-location (:components dervish)))]
;         (is (= (:x location) 1))
;         (is (= (:y location) 2))
;         ; time passes, the dervish whirls north
;         (let [[dervish event] (ents/receive-event dervish tick-event)
;               location (first (filter nomen-is-location (:components dervish)))]
;           (is (= (:x location) 1))
;           (is (= (:y location) 1))
;           ; time passes, the dervish whirls east
;           (let [[dervish event] (ents/receive-event dervish tick-event)
;                 location (first (filter nomen-is-location (:components dervish)))]
;             (is (= (:x location) 2))
;             (is (= (:y location) 1))
;             ; time passes, the dervish whirls back south to his starting point
;             (let [[dervish event] (ents/receive-event dervish tick-event)
;                   location (first (filter nomen-is-location (:components dervish)))]
;               (is (= (:x location) 2))
;               (is (= (:y location) 2))))))))
;   )
