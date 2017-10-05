(ns bucklew.components
  (:require [bucklew.helpers :as help]
            [bucklew.events :as events]
            [bucklew.entities :as entities]))

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

(defrecord LocationComponent [nomen priority x y])
(defn Location [& args] (map->LocationComponent (into args {:nomen :location, :priority 1})))

(defrecord PhysicsComponent [nomen priority max-hp hp strength type take-damage make-attack])
(defn Physics [& args] (map->PhysicsComponent (into args {:nomen :physics
                                                          :priority 100
                                                          :max-hp 10
                                                          :hp 10
                                                          :strength 5
                                                          :type :physical
                                                          :take-damage normal-take-damage
                                                          :make-attack normal-boost-amount})))

(defrecord ArmourComponent [nomen priority strength type take-damage])
(defn Armour [& args] (map->ArmourComponent (into args {:nomen :armour
                                                        :priority 50
                                                        :strength 2
                                                        :type :physical
                                                        :take-damage normal-reduce-amount})))

(defrecord CanAttackComponent [nomen priority make-attack])
(defn CanAttack [& args] (map->CanAttackComponent (into args {:nomen :can-attack
                                                              :priority 900
                                                              :make-attack normal-make-attack})))
