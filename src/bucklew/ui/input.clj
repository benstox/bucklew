(ns bucklew.ui.input
  (:use [bucklew.world.generation :only [empty-room-world random-world]]
        [bucklew.ui.core :only [->UI]])
  (:require [bucklew.entities :as ents]
            [bucklew.events :as events]
            [lanterna.screen :as s]
            [bucklew.world.core :as world-core]))


(defn reset-game [game]
  (let [fresh-world (empty-room-world)]
    (-> game
      (assoc :world fresh-world)
      (assoc :uis [(->UI :play)]))))

(defn move-player [world direction]
  (let [[player-i player] (world-core/get-entity-by-id world 1)
        {:keys [tiles entities]} world
        move-event (events/map->Event {:nomen :move
                                       :data {:direction direction
                                              :tiles tiles
                                              :entities entities}})
        [moved-player event] (ents/receive-event player move-event)
        new-world (assoc-in world [:entities player-i] moved-player)]
    new-world))


(defmulti process-input
  (fn [game input]
    (:kind (last (:uis game)))))

(defmethod process-input :start [game input]
  (reset-game game))


(defmethod process-input :play [game input]
  (case input
    :enter     (assoc game :uis [(->UI :win)])
    :backspace (assoc game :uis [(->UI :lose)])
    \q         (assoc game :uis [])

    \h (update-in game [:world] move-player :w)
    \j (update-in game [:world] move-player :s)
    \k (update-in game [:world] move-player :n)
    \l (update-in game [:world] move-player :e)
    \y (update-in game [:world] move-player :nw)
    \u (update-in game [:world] move-player :ne)
    \b (update-in game [:world] move-player :sw)
    \n (update-in game [:world] move-player :se)

    \R (update-in game [:debug-flags :show-regions] not)

    game))

(defmethod process-input :win [game input]
  (if (= input :escape)
    (assoc game :uis [])
    (assoc game :uis [(->UI :start)])))

(defmethod process-input :lose [game input]
  (if (= input :escape)
    (assoc game :uis [])
    (assoc game :uis [(->UI :start)])))


(defn get-input [game screen]
  (assoc game :input (s/get-key-blocking screen)))
