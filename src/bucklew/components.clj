(ns bucklew.components
  (:require [bucklew.coords :as coords]
            [bucklew.helpers :as help]
            [bucklew.events :as events]
            [bucklew.entities :as ents]))

;; COMPONENT FUNCTIONS
;; * always take [this event component-i] arguments,
;;  that is the relevant entity, the event happening to the entity,
;;  and the index of the current component
;; * always return a (possibly) modified [this event] value

(defn -get
  "Given 'this' and 'component-i' get the current component."
  [this component-i]
  (get-in this [:components component-i]))

(defn -set
  "Given 'this' and 'component-i' set the current component to one with new values in it."
  [this component-i new-component]
  (assoc-in this [:components component-i] new-component))

(defn normal-take-damage
  "The normal way an event causes this' hp to take damage."
  [this event component-i]
  (let [damage-amount (get-in event [:data :amount])
        physics (-get this component-i)
        old-hp (:hp physics)
        new-hp (- old-hp damage-amount)
        new-physics (assoc physics :hp new-hp)
        new-this (-set this component-i new-physics)]
      [new-this event]))

(defn normal-make-attack
  "The normal way an event causes this to make an attack on the CanAttack component.
  Get the data that the make-attack event has accumulated and create a take-damage event
  which will be applied to the target."
  [this event component-i]
  (let [{:keys [amount type target] :as event-data} (:data event)
        take-damage-data {:nomen :take-damage, :data {:amount amount :type type}}
        take-damage-event (events/map->Event take-damage-data)
        [attacked-entity take-damage-event] (ents/receive-event target take-damage-event)
        new-event-data (assoc event-data :target attacked-entity, :take-damage-event take-damage-event)
        new-event (assoc event :data new-event-data)]
    [this new-event]))

(defn normal-reduce-amount
  "Component strength will reduce the amount that an event carries with it."
  [this event component-i]
  (let [component (-get this component-i)
        amount (get-in event [:data :amount])
        strength (:strength component)
        reduced-amount (max (- amount strength) 0)
        new-event (assoc-in event [:data :amount] reduced-amount)]
    [this new-event]))

(defn normal-boost-amount
  "Component strength will boost the amount that an event carries with it."
  [this event component-i]
  (let [component (-get this component-i)
        amount (get-in event [:data :amount])
        strength (:strength component)
        boosted-amount (max (+ amount strength) 0)
        new-event (assoc-in event [:data :amount] boosted-amount)]
    [this new-event]))

(def normal-equipment-data
  [{:nomen :right-hand :type :hand :desc "Right hand" :priority 25}
   {:nomen :left-hand :type :hand :desc "Left hand" :priority 35}
   {:nomen :feet :type :feet :desc "Feet" :priority 35}
   {:nomen :head :type :head :desc "Head" :priority 35}
   {:nomen :back :type :back :desc "Back" :priority 35}])

(defn inventory-add-item
  "Try to add an item to an inventory."
  [this event component-i]
  (if-let [item (get-in event [:data :item])]
    (let [inventory (-get this component-i)
          contents (:contents inventory)
          num-items (count contents)
          capacity (:capacity inventory)]
      (if (< num-items capacity)
        (let [new-event (assoc-in event [:data :item] nil)
              new-item (ents/remove-components-by-nomen item :location)
              new-contents (conj contents new-item)
              new-inventory (assoc inventory :contents new-contents)
              new-this (-set this component-i new-inventory)]
          [new-this new-event]) ; there's a free space so add the item, and send the event away empty
        [this event])) ; inventory full so no change
    [this event])) ; no item so no change
  
(defn equipment-add-item
  "Try to equip an item."
  [this event component-i]
  (if-let [item (get-in event [:data :item])]
    (let [get-equip-info-event (events/map->Event {:nomen :get-equip-info})
          [item finished-equip-info-event] (ents/receive-event item get-equip-info-event)
          equip-info (:data finished-equip-info-event)]
      (if equip-info
        (let [{:keys [slot-types slots-required can-be-equipped-i]} equip-info
              equipment (-get this component-i)
              right-type-slots (filter #(contains? slot-types (:type %)) (:slots equipment))
              items-already-equipped (:contents equipment)
              filled-slots (reduce #(into %1 (:equipped-in %2)) #{} (flatten (map :components items-already-equipped)))
              possible-slots (filter #(not (contains? filled-slots (:nomen %))) right-type-slots)
              num-possible-slots (count possible-slots)]
          (if (<= slots-required num-possible-slots)
            (let [new-event (assoc event :data nil)
                  slots-to-use (take slots-required possible-slots)
                  nomen-slots-to-use (vec (map :nomen slots-to-use))
                  new-item (assoc-in item [:components can-be-equipped-i :equipped-in] nomen-slots-to-use)
                  new-item-minus-loc (ents/remove-components-by-nomen new-item :location)
                  new-contents (conj items-already-equipped new-item-minus-loc)
                  new-equipment (assoc equipment :contents new-contents)
                  new-this (-set this component-i new-equipment)]
              [new-this new-event]) ; there is an appropriate slot so equip the item!
            [this event])) ; equipment full so no change
        [this event])) ; item cannot be equipped so no change
    [this event])) ; no item so no change

(defn normal-get-equip-info
  "Gets called by a CanBeEquipped component and send back relevant info and index for equipping
  this item."
  [this event component-i]
  (if (nil? (:data event))
    (let [can-be-equipped-i component-i
          can-be-equipped (-get this component-i)
          {:keys [slot-types slots-required equipped-in]} can-be-equipped
          info {:slot-types slot-types
                :slots-required slots-required
                :equipped-in equipped-in
                :can-be-equipped-i can-be-equipped-i}
          new-event (assoc event :data info)]
      [this new-event]) ; returns data under the new-event :data key
    [this event])) ; only returns info if target is nil

(defn unequip
  [this event component-i]
  (if-let [slot (get-in event [:data :slot])]
    (let [equipment (-get this component-i)
          equipped-items (:contents equipment)
          get-equip-info-event (events/map->Event {:nomen :get-equip-info})
          items-equip-info (map vector (map #(get-in (second (ents/receive-event % get-equip-info-event)) [:data :item]) equipped-items) (range))
          [item-in-slot item-in-slot-i] (first (filter #(some #{slot} (:equipped-in (first %))) items-equip-info))
          new-item (get equipped-items item-in-slot-i)
          new-item-unequipped (assoc-in new-item [:components (:can-be-equipped-i item-in-slot) :equipped-in] [])
          new-equipped-items (help/vec-remove equipped-items item-in-slot-i)
          new-equipment (assoc equipment :contents new-equipped-items)
          new-this (-set this component-i new-equipment)
          new-event (events/map->Event {:nomen :add-item :data {:item new-item}})]
      [new-this new-event])
    [this event])) ; no slot so no change

(defn normal-set-location
  "Replace the location coordinates with some new ones."
  [this event component-i]
  (if-let [{:keys [new-x new-y] :as new-coords} (:data event)]
    (let [location (get-in this [:components component-i])
          new-event (assoc event :data nil)
          new-location (assoc location :x new-x :y new-y)
          new-this (-set this component-i new-location)]
      [new-this new-event])
    [this event])) ; no location to update to

(defn normal-get-location
  "Gets called by a Location component and send back relevant info and index for this.."
  [this event component-i]
  (if (nil? (:data event))
    (let [location-i component-i
          location (-get this component-i)
          {:keys [x y]} location
          info {:x x :y y :location-i location-i}
          new-event (assoc event :data info)]
      [this new-event]) ; returns data under the new-event :data key
    [this event])) ; only returns info if target is nil

(defn normal-move
  "Move the entity by an amount in each direction (dx, dy)."
  [this event component-i]
  (if-let [move-data (:data event)]
    (let [{:keys [tiles entities direction]} move-data
          {dx :x dy :y} (direction coords/directions)
          {:keys [x y] :as location} (-get this component-i)
          [new-x new-y] (map + [dx dy] [x y])]
      (if (not= (get-in tiles [new-y new-x :kind]) :wall)
        (let [new-event (assoc event :data nil)
              new-location (assoc location :x new-x :y new-y)
              new-this (-set this component-i new-location)]
          [new-this new-event])
        [this event])) ; wall tile, don't move
    [this event])) ; no move data, don't move

(defn normal-debug
  "Just prints something."
  [this event component-i]
  (do
    (println "DEBUG!!")
    [this event]))

(defn normal-tick
  "A normal AI??"
  [this event component-i]
  [this event])

(defn players-tick
  "The player's turn."
  [this event component-i]
  [this event])

(defn give-draw-event-location
  "Add location from the Location component to the draw event that happens to be passing by."
  [this event component-i]
  (let [{:keys [x y]} (-get this component-i)
        display-data (:data event)
        new-display-data (assoc display-data :x x, :y y)
        new-event (assoc event :data new-display-data)]
    [this new-event]))

(defn normal-draw
  "Adds colours and glyph from the Display component to the draw event that happens to be passing by."
  [this event component-i]
  (let [{:keys [glyph fg-colour bg-colour]} (-get this component-i)
        display-data (:data event)
        new-display-data (assoc display-data :glyph glyph, :fg-colour fg-colour, :bg-colour bg-colour)
        new-event (assoc event :data new-display-data)]
    [this new-event]))

;; COMPONENTS

(defrecord DisplayComponent [nomen priority glyph fg-colour bg-colour draw])
(defn Display [& args] (map->DisplayComponent (into args {:nomen :display
                                                          :priority 2
                                                          :fg-colour "ffffff"
                                                          :bg-colour "000000"
                                                          :draw normal-draw})))

(defrecord LocationComponent [nomen priority x y set-location get-location move]
  Object
  (toString [this]
    (str "Location: (" x ", " y ")")))
(defn Location [& args] (map->LocationComponent (into args {:nomen :location
                                                            :priority 1
                                                            :set-location normal-set-location
                                                            :get-location normal-get-location
                                                            :move normal-move
                                                            :draw give-draw-event-location})))

(defrecord PhysicsComponent [nomen priority max-hp hp strength type take-damage make-attack]
  Object
  (toString [this]
    (str "Strength: " strength "; HP: " hp "/" max-hp)))
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
    (let [num-items (count contents)]
      (str "Inventory (" num-items "/" capacity ")\n"
        (clojure.string/join
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

(defrecord EquipmentComponent [nomen priority contents slots add-item remove-item]
  Object
  (toString [this]
    (let [item-equip-info-temp (map #(ents/receive-event % (events/map->Event {:nomen :get-equip-info})) contents)
          items (map first item-equip-info-temp)
          places-equipped (map (comp :equipped-in :data second) item-equip-info-temp)
          item-locations (into {} (map #(into {} (for [place %1] {place %2})) places-equipped items))]
      (str
        "Equipment\n"
        (clojure.string/join
          (for [slot slots]
            (if (contains? item-locations (:nomen slot))
              (str "* " (:desc slot) " -- " ((:nomen slot) item-locations) "\n")
              (str "* " (:desc slot) " -- [empty]\n"))))))))
(defn Equipment [& args] (map->EquipmentComponent (into args {:nomen :equipment
                                                              :priority 25
                                                              :contents []
                                                              :slots (into [] (sort-by
                                                                                :priority
                                                                                (for [slot normal-equipment-data]
                                                                                  (EquipmentSlot slot))))
                                                              :add-item equipment-add-item
                                                              :remove-item unequip})))

(defrecord CanBeEquippedComponent [nomen priority slot-types slots-required equipped-in get-equip-info])
(defn CanBeEquipped [& args] (map->CanBeEquippedComponent (into args {:nomen :can-be-equipped
                                                                      :priority 200
                                                                      :slot-types #{:hand}
                                                                      :slots-required 1
                                                                      :equipped-in []
                                                                      :get-equip-info normal-get-equip-info})))

(defrecord TakesTurnComponent [nomen priority tick data])
(defn TakesTurn [& args] (map->TakesTurnComponent (into args {:nomen :takes-turn, :priority 5, :tick normal-tick, :data {}})))

(defrecord DebugComponent [nomen priority debug])
(defn Debug [& args] (map->DebugComponent (into args {:nomen :debug, :priority 30, :debug normal-debug})))

(defrecord TeamComponent [nomen priority team])
(defn Team [& args] (map->TeamComponent (into args {:nomen :team})))
