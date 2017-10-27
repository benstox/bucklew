(ns bucklew.core
  (:require
    [bucklew.ui.core :as ui]
    [bucklew.ui.input :as ui-input]
    [lanterna.screen :as s]))


; Data Structures -------------------------------------------------------------
(defrecord Game [world uis screen menu-position run-ui restarted debug-flags])

; Main ------------------------------------------------------------------------
(defn run-game [game]
  (loop [{:keys [input uis] :as game} game]
    (when (seq uis)
      (recur (ui-input/run-ui game)))))

(defn new-game [screen]
  (map->Game {:world nil
              :uis [(ui/->UI :play) (ui/->UI :menu)]
              :screen screen
              :menu-position 0
              :run-ui ui-input/run-ui
              :restarted false
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
