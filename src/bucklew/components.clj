(ns bucklew.components
	(:require [bucklew.helpers :as help]))

(defn normal-take-damage [this damage-amount]
	(let [physics (help/find-first #(= (:nomen %) :physics) (:components this))
		  old-hp (:hp physics)
		  new-hp (- old-hp damage-amount)]
		)
	

(defn physics-component
	([] {
		:nomen :physics
		:priority 100
		:hp 10
		:take-damage normal-take-damage})
	([hp] {
		:nomen :physics
		:priority 100
		:hp hp
		:take-damage normal-take-damage}))
