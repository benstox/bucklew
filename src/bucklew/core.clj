(ns bucklew.core
  (:use [bucklew.ui.core :only [->UI]]
        [bucklew.ui.drawing :only [draw-game]])
  (:require
    [bucklew.components :as comps]
    [bucklew.creatures :as creats]
    [bucklew.entities :as ents]
    [bucklew.events :as events]
    [bucklew.helpers :as help]
    [bucklew.items :as items]
    [bucklew.ui.input :as input]
    [bucklew.world.core :as world-core]
    [bucklew.world.generation :as gen]
    [lanterna.screen :as s]))


; Data Structures -------------------------------------------------------------
(defrecord Game [world uis screen menu-position debug-flags])

; Main ------------------------------------------------------------------------
(defn run-game [game]
  (loop [{:keys [input uis] :as game} game]
    (when (seq uis)
      (recur (input/run-ui game)))))

 ; (defn run-game [game screen]
 ;   (loop [{:keys [input uis] :as game} game]
 ;     (when (seq uis)
 ;       (recur (if input
 ;                (-> game
 ;                  (dissoc :input)
 ;                  (process-input input))
 ;                (-> game
 ;                  (update-in [:world] tick-all)
 ;                  (draw-game screen)
 ;                  (get-input screen)))))))

(defn new-game [screen]
  (map->Game {:world nil
              :uis [(->UI :play) (->UI :menu)]
              :screen screen
              :menu-position 0
              :debug-flags {:show-regions false}}))

(defn main
  ([] (main :swing false))
  ([screen-type] (main screen-type false))
  ([screen-type block?]
   (letfn [(go []
             (let [screen (s/get-screen screen-type)]
               (s/in-screen screen
                            (run-game (new-game screen)))))]
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
  (main :swing true))
