(ns bucklew.components
  (:require [bucklew.helpers :as help]
            [bucklew.events :as events]))

;; COMPONENT FUNCTIONS
;; * always take [this event component-i] arguments,
;;  that is the relevant entity, the event happening to the entity,
;;  and the index of the current component
;; * always return a (possibly) modified [this event] value

(defn normal-take-damage [this event component-i]
  "The normal way an event causes this' hp to take damage."
  (let [damage-amount (:amount event)
        component (get-in this [:components component-i])
        old-hp (:hp component)
        new-hp (- old-hp damage-amount)]
      [(assoc-in this [:components component-i :hp] new-hp) event]))

(defn normal-make-attack [this event component-i]
  "The normal way an event causes this to make an attack."
  (let [take-damage-data {:nomen :take-damage, :amount (:amount event), :type (:type event)}
        take-damage-event (events/map->Event take-damage-data)
        target (:target event)]
    [this (assoc event :target (entities/receive-event target take-damage-event))]))

(defn normal-reduce-amount [this event component-i]
  "Component strength will reduce the amount that an event carries with it."
  (let [component (get-in this [:components component-i])
        amount (:amount event)
        strength (:strength component)
        reduced-amount (max (- amount strength) 0)
        new-event (assoc event :amount reduced-amount)]
    [this new-event]))

(defn normal-boost-amount [this event component-i]
  "Component strength will boost the amount that an event carries with it."
  (let [component (get-in this [:components component-i])
        amount (:amount event)
        strength (:strength component)
        boosted-amount (max (+ amount strength) 0)
        new-event (assoc event :amount boosted-amount)]
    [this new-event]))

;; COMPONENTS

(def normal-physics {
  :nomen :physics
  :priority 100
  :max-hp 10
  :hp 10
  :strength 5
  :type :physical
  :take-damage normal-take-damage
  :make-attack normal-boost-amount})

(defn physics
  "Creates a physics component."
  ([] normal-physics)
  ([hp] (assoc normal-physics :hp hp :max-hp hp))
  ([hp take-damage-fn]
    (assoc normal-physics
      :hp hp
      :max-hp hp
      :take-damage take-damage-fn)))

(def normal-armour {
  :nomen :armour
  :priority 50
  :strength 2
  :type :physical
  :take-damage normal-reduce-amount})

(defn armour
  "Creates an armour component."
  ([] normal-armour)
  ([strength] (assoc normal-armour :strength strength))
  ([strength type] (assoc normal-armour :strength strength :type type))
  ([strength type reduce-damage-fn]
     (assoc normal-armour
       :strength strength
       :type type
       :take-damage reduce-damage-fn)))

(def normal-can-attack {
  :nomen :can-attack
  :priority 900
  :strength 5
  :make-attack normal-make-attack})

(defn can-attack
  "Creates a 'can attack' component."
  ([] normal-can-attack)
  ([strength] (assoc normal-can-attack :strength strength))
  ([strength make-attack-fn]
     (assoc normal-can-attack
       :strength strength
       :make-attack make-attack-fn)))
