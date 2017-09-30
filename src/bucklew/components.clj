(ns bucklew.components
	(:require [bucklew.helpers :as help]))

(defn normal-take-damage [this] (help/find-first #(= (:nomen %) :physics) (:components this)))

(defn physics-component
	([] {
		:nomen :physics
		:hp 10
		:take-damage normal-take-damage})
	([hp]))