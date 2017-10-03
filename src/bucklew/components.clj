(ns bucklew.components
  (:require [bucklew.helpers :as help]))

(defn get-physics-component [components]
  (let [nomen-is-physics (partial help/nomen-is :physics)
        physics (help/find-first nomen-is-physics components)]
    physics))

(defn normal-take-damage [this event component]
  "The normal way an event causes this' hp to take damage."
  (let [damage-amount (:amount event)
        nomen-is-physics (partial help/nomen-is :physics)
        [physics physics-i] (help/find-first-with-index nomen-is-physics (:components this))
        old-hp (:hp physics)
        new-hp (- old-hp damage-amount)]
      [(assoc-in this [:components physics-i :hp] new-hp) event]))

(def normal-physics {
  :nomen :physics
  :priority 100
  :hp 10
  :take-damage normal-take-damage})

(defn physics-component
  "Creates a physics component."
  ([] normal-physics)
  ([hp] (assoc normal-physics :hp hp))
  ([hp take-damage-function]
    (assoc normal-physics
      :hp hp
      :take-damage take-damage-function)))

(defn armour-defend [this event component]
  "Armour will reduce the damage that an event carries with it."
  (let [damage-amount (:amount event)
        strength (:strength component)
        reduced-damage (max (- damage-amount strength) 0)
        new-event (assoc event :amount reduced-damage)]
    [this new-event]))

(defn armour-component
  "Creates an armour component."
  ([] {
    :nomen :armour
    :strength 2
    :priority 50
    :take-damage armour-defend})
  ([strength] {
    :nomen :armour
    :strength strength
    :priority 50
    :take-damage armour-defend}))
