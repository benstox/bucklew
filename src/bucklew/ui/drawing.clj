(ns bucklew.ui.drawing
  (:require [lanterna.screen :as s]
            [bucklew.entities :as ents]
            [bucklew.events :as events]
            [bucklew.helpers :as help]))


; Definitions -----------------------------------------------------------------
(defmulti draw-ui
  (fn [ui game]
    (:kind ui)))


(defn clear-screen [screen]
  (let [[cols rows] (s/get-size screen)
        blank (apply str (repeat cols \space))]
    (doseq [row (range rows)]
      (s/put-string screen 0 row blank))))


; Start -----------------------------------------------------------------------
(defmethod draw-ui :menu [ui game]
  (let [{:keys [screen menu-position]} game
        menu-options (help/get-menu-options game)]
    (do
      (clear-screen screen)
      (s/put-string screen 0 0 "Welcome to a Clojure Roguelike!")
      (doseq [[option-i option] (help/enumerate menu-options)]
        (s/put-string
          screen
          0
          (+ 3 option-i)
          (:text option)
          (when (= option-i menu-position) {:fg :black :bg :white}))))))


; Win -------------------------------------------------------------------------
(defmethod draw-ui :win [ui game]
  (let [screen (:screen game)]
    (s/put-sheet screen 0 0
                 ["Congratulations, you win!"
                  "Press escape to exit, anything else to restart."])))


; Lose ------------------------------------------------------------------------
(defmethod draw-ui :lose [ui game]
  (let [screen (:screen game)]
    (s/put-sheet screen 0 0
                 ["Sorry, better luck next time."
                  "Press escape to exit, anything else to restart."])))


; Play ------------------------------------------------------------------------
;
; The Play UI draws the world.  This is tricky, but hopefully not too bad.
;
; Imagine a 10 by 4 world with a 3 by 2 "viewport":
;
;  0123456789
; 0...OVV....
; 1...VVV....
; 2...VVV....
; 3..........
; 4..........
;
; The V is the viewport, and the O is the "viewport origin", which would be
; [3 0] in terms of the map's coordinates.

(defn get-viewport-coords
  "Find the top-left coordinates of the viewport in the overall map, centering on the player."
  [game player-location vcols vrows]
  (let [[center-x center-y] player-location

        tiles (:tiles (:world game))

        map-rows (count tiles)
        map-cols (count (first tiles))

        start-x (- center-x (int (/ vcols 2)))
        start-x (max 0 start-x)

        start-y (- center-y (int (/ vrows 2)))
        start-y (max 0 start-y)

        end-x (+ start-x vcols)
        end-x (min end-x map-cols)

        end-y (+ start-y vrows)
        end-y (min end-y map-rows)

        start-x (- end-x vcols)
        start-y (- end-y vrows)]
    {:x start-x :y start-y}))

(defn get-viewport-coords-of
  "Get the viewport coordiates for the given real coords, given the viewport origin."
  [origin coords]
  (let [{x0 :x y0 :y} origin
        {x1 :x y1 :y} coords]
    {:x (- x1 x0) :y (- y1 y0)}))


(defn draw-hud [screen game]
  (let [hud-row (dec (second (s/get-size screen)))
        player (get-in game [:world :entities :player])
        {:keys [location hp max-hp]} player
        [x y] location
        info (str "hp [" hp "/" max-hp "]")
        info (str info " loc: [" x "-" y "]")]
    (s/put-string screen 0 hud-row info)))


(defn draw-entity [screen vrows vcols x y glyph fg-colour]
  (let [max-x (dec vcols)
        max-y (dec vrows)]
    (when (and (<= 0 x max-x)
               (<= 0 y max-y))
      (s/put-string screen x y glyph fg-colour))))


(defn draw-world [screen vrows vcols o-coord tiles]
  (letfn [(render-tile [tile]
            [(:glyph tile) {:fg (:color tile)}])]
    (let [{ox :x oy :y} o-coord
          tiles (help/shear tiles ox oy vcols vrows)
          sheet (help/map2d render-tile tiles)]
      (s/put-sheet screen 0 0 sheet))))

(defn draw-regions [screen region-map vrows vcols [ox oy]]
  (letfn [(get-region-glyph [region-number]
            (str
              (nth
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                region-number)))]
    (doseq [x (range ox (+ ox vcols))
            y (range oy (+ oy vrows))]
      (let [region-number (region-map [x y])]
        (when region-number
          (s/put-string screen (- x ox) (- y oy)
                        (get-region-glyph region-number)
                        {:fg :blue}))))))


(defn highlight-player [screen origin player]
  (let [[x y] (get-viewport-coords-of origin (:location player))]
    (s/move-cursor screen x y)))


(defn draw-messages [screen messages]
  (doseq [[i msg] (help/enumerate messages)]
    (s/put-string screen 0 i msg {:fg :black :bg :white})))

(defmethod draw-ui :play [ui game]
  (let [{:keys [world screen]} game
        {:keys [tiles entities regions]} world
        [cols rows] (s/get-size screen)
        vcols cols
        vrows (dec rows)
        origin {:x 0 :y 0}]
    (draw-world screen vrows vcols origin tiles)
    (when (get-in game [:debug-flags :show-regions])
      (draw-regions screen regions vrows vcols origin))
    (doseq [entity entities]
      (let [[entity event] (ents/receive-event entity events/draw)
            display-info (:data event)
            {:keys [x y fg-colour bg-colour glyph]} display-info]
        (when (not-any? nil? '(x y fg-colour bg-colour glyph))
          (draw-entity screen vrows vcols x y glyph fg-colour))))
    (draw-hud screen game)
    ; (draw-messages screen (:messages player))
    ; (highlight-player screen origin player)
    ))


; Entire Game -----------------------------------------------------------------
(defn draw-game [game]
  (let [screen (:screen game)]
    (s/clear screen)
    (doseq [ui (:uis game)]
      (draw-ui ui game))
    (s/redraw screen)
    game))
