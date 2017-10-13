(ns bucklew.core
  (:use [bucklew.ui.core :only [->UI]]
        [bucklew.ui.drawing :only [draw-game]]
        [bucklew.ui.input :only [get-input process-input]])
  (:require
    [bucklew.components :as comps]
    [bucklew.creatures :as creats]
    [bucklew.entities :as ents]
    [bucklew.events :as events]
    [bucklew.helpers :as help]
    [bucklew.items :as items]
    [bucklew.world.core :as world-core]
    [lanterna.screen :as s]))


; Data Structures -------------------------------------------------------------
(defrecord Game [world uis input debug-flags])

; Main ------------------------------------------------------------------------
(defn tick-entity [world indexed-entity]
  (let [[entity-i entity] indexed-entity]
    (ents/tick entity world entity-i)))

(defn tick-all [world]
  (let [entities (:entities world)
        indexed-entities (help/enumerate entities)]
    (reduce tick-entity world indexed-entities)))

(defn clear-messages [game]
  (assoc-in game [:world :entities :player :messages] nil))


(defn run-game [game screen]
  (loop [{:keys [input uis] :as game} game]
    (when (seq uis)
      (recur (if input
               (-> game
                 (dissoc :input)
                 (process-input input))
               (-> game
                 (update-in [:world] tick-all)
                 (draw-game screen)
                 (clear-messages)
                 (get-input screen)))))))

(defn new-game []
  (map->Game {:world nil
              :uis [(->UI :start)]
              :input nil
              :debug-flags {:show-regions false}}))

(defn main
  ([] (main :swing false))
  ([screen-type] (main screen-type false))
  ([screen-type block?]
   (letfn [(go []
             (let [screen (s/get-screen screen-type)]
               (s/in-screen screen
                            (run-game (new-game) screen))))]
     (if block?
       (go)
       (future (go))))))


(defn -main [& args]
  (let [args (set args)
        screen-type (cond
                      (args ":swing") :swing
                      (args ":text")  :text
                      :else           :auto)]
    (main screen-type true)))


(comment
  (main :swing false)
  (main :swing true)
  )
