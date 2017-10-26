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
  (let [fresh-world (empty-room-world)]
    (-> game
      (assoc :world fresh-world)
      (assoc :uis [(->UI :play)]))))

(defn tick-entity [game indexed-entity]
  (let [[entity-i entity] indexed-entity]
    (ents/tick entity entity-i game)))

(defn tick-all [game]
  (let [{{entities :entities :as world}
         :world uis
         :uis screen
         :screen :as game} game
        indexed-entities (help/enumerate entities)]
    (reduce tick-entity game indexed-entities)))


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
              :new-game (-> game (update :uis pop) (reset-game))
              :continue (update game :uis pop)
              game))
          (recur
            (case input ; move cursor, stay in this loop for now
              :down (update game :menu-position #(mod (inc %) num-options))
              \j (update game :menu-position #(mod (inc %) num-options))
              :up (update game :menu-position #(mod (dec %) num-options))
              \k (update game :menu-position #(mod (dec %) num-options))
              game)))))))

; (defmethod run-ui :play [game screen]
;   (update-in game [:world] tick-all screen))

(defmethod run-ui :play [game]
  (tick-all game))

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
