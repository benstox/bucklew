(ns bucklew.ui.input
  (:use [bucklew.world.generation :only [empty-room-world random-world]]
        [bucklew.ui.core :only [->UI]])
  (:require [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.helpers :as help]
            [bucklew.ui.drawing :as draw]
            [bucklew.world.core :as world-core]
            [lanterna.screen :as s]))


(defn reset-game [game]
  (let [fresh-world (random-world)
        restarted (-> game :world nil? not)]
    (-> game
      (assoc :world fresh-world)
      (assoc :restarted restarted)
      (assoc :uis [(->UI :play)]))))

(defn tick-entity [game]
  )

      ; (tick [this entity-i game]
      ;   (let [[new-this new-event] (receive-event this (assoc events/tick :data game))
      ;         updated-game (:data new-event)]
      ;     (if (:restarted updated-game)
      ;       updated-game ; send the restared game straight back
      ;       (assoc-in updated-game [:world :entities entity-i] new-this))))


(defmulti run-ui
  (fn [game]
    (:kind (last (:uis game)))))

(defmethod run-ui :menu [game]
  (let [{:keys [screen menu-position]} game
        menu-options (help/get-menu-options game)
        num-options (count menu-options)]
    (loop [game game] ; menu loop
      (draw/draw-game game)
      (let [input (s/get-key-blocking screen)]
        (if (= input :enter) ; moves things on, does some action like return to game
          (let [menu-position (:menu-position game)
                choice (get-in menu-options [menu-position :action])]
            (case choice
              :new-game (reset-game game)
              :continue (update game :uis pop)
              :quit (-> game (assoc :uis []) (assoc :restarted true))
              game))
          (recur
            (case input ; move cursor, stay in this loop for now
              :down (update game :menu-position #(mod (inc %) num-options))
              \j (update game :menu-position #(mod (inc %) num-options))
              :up (update game :menu-position #(mod (dec %) num-options))
              \k (update game :menu-position #(mod (dec %) num-options))
              game)))))))

(defmethod run-ui :play [game]
  (let [entity-i (get-in game [:world :entity-i])
        [updated-game tick-event] (events/fire-event events/tick game entity-i) ; the entity takes a turn
        entities (get-in game [:world :entities])
        num-entities (count entities)
        updated-game (update-in updated-game [:world :entity-i] #(mod (inc %) num-entities))]
    updated-game))

(defmethod run-ui :win [game]
  (let [screen (:screen game)]
    (draw/draw-game game)
    (let [input (s/get-key-blocking screen)]
      (if (= input :escape)
        (assoc game :uis [])
        (assoc game :uis [(->UI :start)])))))

(defmethod run-ui :lose [game]
  (let [screen (:screen game)]
    (draw/draw-game game)
    (let [input (s/get-key-blocking screen)]
      (if (= input :escape)
        (assoc game :uis [])
        (assoc game :uis [(->UI :start)])))))
