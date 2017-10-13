(ns bucklew.coords
  (:require [bucklew.helpers :as help]))


(defn radial-distance
  "Return the radial distance between two points.
  
  There might be a better name for this, but in a nutshell:

      3333333
      3222223
      3211123
      321.123
      3211123
      3222223
      3333333
  
  "
  [[x1 y1] [x2 y2]]
  (max (help/abs (- x1 x2))
       (help/abs (- y1 y2))))


(def directions
  {:w  {:x -1 :y  0}
   :e  {:x  1 :y  0}
   :n  {:x  0 :y -1}
   :s  {:x  0 :y  1}
   :nw {:x -1 :y -1}
   :ne {:x  1 :y -1}
   :sw {:x -1 :y  1}
   :se {:x  1 :y  1}})

(defn offset-coords
  "Offset the starting coordinate by the given amount, returning the result coordinate."
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn dir-to-offset
  "Convert a direction to the offset for moving 1 in that direction."
  [dir]
  (directions dir))

(defn destination-coords
  "Take an origin's coords and a direction and return the destination's coords."
  [origin dir]
  (offset-coords origin (dir-to-offset dir)))

(defn neighbors
  "Return the coordinates of all neighboring squares of the given coord."
  [origin]
  (map offset-coords (vals directions) (repeat origin)))
