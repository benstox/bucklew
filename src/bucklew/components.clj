(ns bucklew.components
  (:require [bucklew.helpers :as help]
            [bucklew.events :as events]
            [bucklew.entities :as ents]))

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
    [this (assoc event :target (ents/receive-event target take-damage-event))]))

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

(def normal-equipment-data
  [{:nomen :right-hand :type :hand :desc "Right hand" :priority 25}
   {:nomen :left-hand :type :hand :desc "Left hand" :priority 35}
   {:nomen :feet :type :feet :desc "Feet" :priority 35}
   {:nomen :head :type :head :desc "Head" :priority 35}
   {:nomen :back :type :back :desc "Back" :priority 35}])

(defn inventory-add-item [this event component-i]
  "Try to add an item to an inventory."
  (if-let [item (:target event)]
    (let [inventory (get-in this [:components component-i])
          contents (:contents inventory)
          num-items (count contents)
          capacity (:capacity inventory)]
      (if (< num-items capacity)
        (let [new-event (assoc event :target nil)
              new-inventory (conj contents item)
              new-this (assoc-in this [:components component-i :contents] new-inventory)]
          [new-this new-event]) ; there's a free space so add the item, and send the event away empty
        [this event])) ; inventory full so no change
    [this event])) ; no item so no change
  
(defn equipment-add-item [this event component-i]
  (if-let [item (:target event)]
    (let [nomen-is-can-be-equipped (partial help/nomen-is :can-be-equipped)
          item-components-indexed (map vector (:components item) (range))
          can-be-equipped-components (filter (comp nomen-is-can-be-equipped first) item-components-indexed)]
      (if (not-empty can-be-equipped-components)
        (let [[can-be-equipped can-be-equipped-i] (first can-be-equipped-components)
              equipment (get-in this [:components component-i])
              possible-slot-types (:slot-types can-be-equipped)
              num-slots-required (:slots-required can-be-equipped)
              right-type-slots (filter #(contains? possible-slot-types (:type %)) (:slots equipment))
              items-already-equipped (:contents equipment)
              filled-slots (reduce #(into %1 (:equipped-in %2)) #{} (flatten (map :components items-already-equipped)))
              possible-slots (filter #(not (contains? filled-slots (:nomen %))) right-type-slots)
              num-possible-slots (count possible-slots)]
          (if (< num-slots-required num-possible-slots)
            (let [new-event (assoc event :target nil)
                  slots-to-use (take num-slots-required possible-slots)
                  nomen-slots-to-use (into [] (map :nomen slots-to-use))
                  new-item (assoc-in item [:components can-be-equipped-i :equipped-in] nomen-slots-to-use)
                  new-equipment (conj items-already-equipped new-item)
                  new-this (assoc-in this [:components component-i :contents] new-equipment)]
              [new-this new-event]) ; there is an appropriate slot so equip the item!
            [this event])) ; equipment full so no change
        [this event])) ; item cannot be equipped so no change
    [this event])) ; no item so no change

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

(defrecord BladeComponent [nomen priority strength type take-damage])
(defn Blade [& args] (map->BladeComponent (into args {:nomen :blade
                                                      :priority 500
                                                      :strength 2
                                                      :type :blade
                                                      :make-attack normal-boost-amount})))

(defrecord CanAttackComponent [nomen priority make-attack])
(defn CanAttack [& args] (map->CanAttackComponent (into args {:nomen :can-attack
                                                              :priority 900
                                                              :make-attack normal-make-attack})))

(defrecord InventoryComponent [nomen priority contents capacity add-item]
  Object
  (toString [this]
    "Override str method."
    (let [contents (:contents this)
          num-items (count contents)
          capacity (:capacity this)]
      (str "Inventory (" num-items "/" capacity ")\n"
        (apply str
          (for [item contents]
            (str "* " item "\n")))))))
(defn Inventory [& args] (map->InventoryComponent (into args {:nomen :inventory
                                                              :priority 200
                                                              :contents []
                                                              :capacity 10
                                                              :add-item inventory-add-item})))

(defrecord EquipmentSlotComponent [nomen type desc priority])
(defn EquipmentSlot [& args] (map->EquipmentSlotComponent (into args {:nomen :right-hand
                                                                      :type :hand
                                                                      :desc "Right hand"
                                                                      :priority 25})))

(defrecord EquipmentComponent [nomen priority contents slots add-item])
(defn Equipment [& args] (map->EquipmentComponent (into args {:nomen :equipment
                                                              :priority 25
                                                              :contents []
                                                              :slots (into [] (sort-by
                                                                                :priority
                                                                                (for [slot normal-equipment-data]
                                                                                  (EquipmentSlot slot))))
                                                              :add-item equipment-add-item})))

(defrecord CanBeEquippedComponent [nomen priority slot-types slots-required equipped-in])
(defn CanBeEquipped [& args] (map->CanBeEquippedComponent (into args {:nomen :can-be-equipped
                                                                      :priority 200
                                                                      :slot-types #{:hand}
                                                                      :slots-required 1
                                                                      :equipped-in []})))
