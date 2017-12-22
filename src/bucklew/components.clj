(ns bucklew.components
  (:require [bucklew.coords :as coords]
            [bucklew.helpers :as help]
            [bucklew.events :as events]
            [bucklew.entities :as ents]
            [bucklew.ui.core :as ui]
            [bucklew.ui.drawing :as draw]
            [bucklew.world.core :as world-core]
            [com.rpl.specter :as specter :refer [select select-one selected? transform setval ALL INDEXED-VALS]]
            [ebenbild.core :as eb]
            [lanterna.screen :as s]))

;; COMPONENT FUNCTIONS -----------------------------------------------------------------------------
;; * always take [game entity-i component-i event] arguments,
;;  that is the relevant entity, the event happening to the entity,
;;  and the index of the current component
;; * always return a (possibly) modified [this event] value

(defn normal-take-damage
  "The normal way an event causes this' hp to take damage."
  [game entity-i component-i event]
  (let [damage-amount (get-in event [:data :amount])
        reduce-hp #(- % damage-amount)
        new-game (update-in game [:world :entities entity-i :components component-i :hp] reduce-hp)]
      [new-game event]))

(defn normal-make-attack
  "The normal way an event causes this to make an attack on the CanAttack component.
  Get the data that the make-attack event has accumulated and create a take-damage event
  which will be applied to the target."
  [game entity-i component-i event]
  (let [{:keys [amount type target-i] :as event-data} (:data event)
        take-damage-event (events/map->Event {:nomen :take-damage, :data {:amount amount :type type}})
        [new-game take-damage-event] (events/fire-event take-damage-event game target-i)]
    [new-game event]))

(defn normal-reduce-amount
  "Component strength will reduce the amount that an event carries with it."
  [game entity-i component-i event]
  (let [strength (get-in game [:world :entities entity-i :components component-i :strength])
        new-event (update-in event [:data :amount] #(max (- % strength) 0))]
    [game new-event]))

(defn normal-boost-amount
  "Component strength will boost the amount that an event carries with it."
  [game entity-i component-i event]
  (let [strength (get-in game [:world :entities entity-i :components component-i :strength])
        new-event (update-in event [:data :amount] #(max (+ % strength) 0))]
    [game new-event]))

(def normal-equipment-data
  [{:nomen :right-hand :type :hand :desc "Right hand" :priority 25}
   {:nomen :left-hand :type :hand :desc "Left hand" :priority 35}
   {:nomen :feet :type :feet :desc "Feet" :priority 35}
   {:nomen :head :type :head :desc "Head" :priority 35}
   {:nomen :back :type :back :desc "Back" :priority 35}])

(defn inventory-add-item
  "Try to add an item to an inventory."
  [game entity-i component-i event]
  (if-let [item-i (get-in event [:data :item-i])]
    (let [item (get-in game [:world :entities item-i])
          inventory-comp (get-in game [:world :entities entity-i :components component-i])
          num-items (count (:contents inventory-comp))
          capacity (:capacity inventory-comp)]
      (if (< num-items capacity)
        (let [new-event (assoc-in event [:data :item-i] nil)
              new-item (ents/remove-components-by-nomen item :location)
              ; put the item into the inventory
              new-game (update-in game [:world :entities entity-i :components component-i :contents] #(conj % new-item))
              ; remove the item from the list of world entities
              new-entities (help/vec-remove (get-in new-game [:world :entities]) item-i)
              new-game (assoc-in new-game [:world :entities] new-entities)]
          [new-game new-event]) ; there's a free space so add the item, and send the event away empty
        [game event])) ; inventory full so no change
    [game event])) ; no item so no change

(defn equipment-add-item
  "Try to equip an item."
  [game entity-i component-i event]
  (if-let [item-i (get-in event [:data :item-i])]
    (let [item (get-in game [:world :entities item-i])
          can-be-equipped (specter/select-one
                            [:components
                             INDEXED-VALS
                             (selected? 1 (eb/like {:nomen :can-be-equipped}))] item)]
      (if can-be-equipped
        (let [[can-be-equipped-i {:keys [slot-types slots-required equipped-in]}] can-be-equipped
              equipment-comp (get-in game [:world :entities entity-i :components component-i])
              right-type-slots (filter #(contains? slot-types (:type %)) (:slots equipment-comp))
              items-already-equipped (:contents equipment-comp)
              filled-slots (reduce #(into %1 (:equipped-in %2)) #{} (flatten (map :components items-already-equipped)))
              possible-slots (filter #(not (contains? filled-slots (:nomen %))) right-type-slots)
              num-possible-slots (count possible-slots)]
          (if (<= slots-required num-possible-slots)
            (let [new-event (assoc-in event [:data :item-i] nil)
                  slots-to-use (take slots-required possible-slots)
                  nomen-slots-to-use (vec (map :nomen slots-to-use))
                  new-item (assoc-in item [:components can-be-equipped-i :equipped-in] nomen-slots-to-use)
                  new-item (ents/remove-components-by-nomen new-item :location)
                  ; equip the item
                  new-game (update-in game [:world :entities entity-i :components component-i :contents] #(conj % new-item))
                  ; remove the item from the list of world entities
                  new-entities (help/vec-remove (get-in new-game [:world :entities]) item-i)
                  new-game (assoc-in new-game [:world :entities] new-entities)]
              [new-game new-event]) ; there is an appropriate slot so equip the item!
            [game event])) ; equipment full so no change
        [game event])) ; item cannot be equipped so no change
    [game event])) ; no item so no change

(defn unequip
  [game entity-i component-i event]
  (if-let [slot (get-in event [:data :slot])]
    (let [equipment-comp (get-in game [:world :entities entity-i :components component-i])
          equipped-items (:contents equipment-comp)
          get-equip-info-event (events/map->Event {:nomen :get-equip-info})
          items-equip-info (map vector (map #(get-in (second (ents/receive-event % get-equip-info-event)) [:data :item]) equipped-items) (range))
          [item-in-slot item-in-slot-i] (first (filter #(some #{slot} (:equipped-in (first %))) items-equip-info))
          new-item (get equipped-items item-in-slot-i)
          new-item-unequipped (assoc-in new-item [:components (:can-be-equipped-i item-in-slot) :equipped-in] [])
          new-equipped-items (help/vec-remove equipped-items item-in-slot-i)
          new-game (assoc-in game [:world :entities entity-i :components component-i :contents] new-equipped-items)
          new-event (events/map->Event {:nomen :add-item :data {:item new-item}})]
      [new-game new-event])
    [game event])) ; no slot so no change

(defn normal-set-location
  "Replace the location coordinates with some new ones."
  [game entity-i component-i event]
  (if-let [{:keys [new-x new-y] :as new-coords} (:data event)]
    (let [location-comp (get-in game [:world :entities entity-i :components component-i])
          new-event (assoc event :data nil)
          new-location-comp (assoc location-comp :x new-x :y new-y)
          new-game (assoc-in game [:world :entities entity-i :components component-i] new-location-comp)]
      [new-game new-event])
    [game event])) ; no location to update to

(defn normal-get-location
  "Gets called by a Location component and send back relevant info and index for the entity."
  [game entity-i component-i event]
  (if (nil? (:data event))
    (let [location (get-in game [:world :entities entity-i :components component-i])
          {:keys [x y]} location
          info {:x x :y y :location-i component-i}
          new-event (assoc event :data info)]
      [game new-event]) ; returns data under the new-event :data key
    [game event])) ; only returns info if target is nil

(defn normal-move
  "Move the entity by an amount in each direction (dx, dy)."
  [game entity-i component-i event]
  (if-let [direction (get-in event [:data :direction])]
    (let [{{tiles :tiles entities :entities :as world} :world :as game} game
          {dx :x dy :y} (direction coords/directions)
          {:keys [x y] :as location-comp} (get-in world [:entities entity-i :components component-i])
          [new-x new-y] (map + [dx dy] [x y])
          destination {:x new-x :y new-y}
          entity (get-in world [:entities entity-i])]
      (println x y)
      (println new-x new-y)
      (if-let [{:keys [interaction target-i]} (world-core/get-interaction-from-location world entity destination)]
        (let [make-attack-event (assoc-in events/make-attack-event [:data :target-i] target-i)
              [new-game new-event] (events/fire-event make-attack-event game entity-i)]
          [new-game new-event])
        (if (not= (get-in tiles [new-y new-x :kind]) :wall)
          (let [new-event (assoc event :data nil)
                new-location-comp (assoc location-comp :x new-x :y new-y)
                new-game (assoc-in game [:world :entities entity-i :components component-i] new-location-comp)]
            (println new-location-comp)
            (println (str "normal move " (get-in new-game [:world :entities entity-i])))
            [new-game new-event])
          [game event])) ; wall tile, don't move
    [game event]))) ; no move data, don't move

(defn normal-debug
  "Just prints something."
  [game entity-i component-i event]
  (do
    (println "DEBUG!!")
    [game event]))

(defn normal-tick
  "A normal AI??"
  [game entity-i component-i event]
  [game event])

(defn players-tick
  "The player's turn."
  [game entity-i component-i event]
  (let [{screen :screen run-ui :run-ui :as game} game]
    (loop [game game]
      (if (:restarted game)
        [game (assoc event :data game)] ; send the restarted game straight back
        (do                             ; otherwise proceed as normal
          ; (println "###")
          (draw/draw-game game)
          (let [input (s/get-key-blocking screen)
                {:keys [world uis]} game]
            (if-let [direction (get help/keys-to-directions input)]
              (let [move-data {:direction direction}
                    [new-game move-event] (events/fire-event (assoc events/move :data move-data) game entity-i)]
                ; (println (str "players-tick " (get-in new-game [:world :entities entity-i])))
                [new-game event])
              (recur (case input
                ; menu stuff, quit, etc. or unused keys
                :escape (run-ui (update game :uis #(conj % (ui/->UI :menu))))
                game)))))))))

(defn give-draw-event-location
  "Add location from the Location component to the draw event that happens to be passing by."
  [game entity-i component-i event]
  (let [{:keys [x y]} (get-in game [:world :entities entity-i :components component-i])
        display-data (:data event)
        new-display-data (assoc display-data :x x, :y y)
        new-event (assoc event :data new-display-data)]
    [game new-event]))

(defn normal-draw
  "Adds colours and glyph from the Display component to the draw event that happens to be passing by."
  [game entity-i component-i event]
  (let [{:keys [glyph fg-colour bg-colour]} (get-in game [:world :entities entity-i :components component-i])
        display-data (:data event)
        new-display-data (assoc display-data :glyph glyph, :fg-colour fg-colour, :bg-colour bg-colour)
        new-event (assoc event :data new-display-data)]
    [game new-event]))

(defn normal-get-interaction
  "Decide whether this is an enemy or an ally of the interactor and send back the
  relevant interaction."
  [game entity-i component-i event]
  (if-let [interactor-team (get-in event [:data :interactor-team])]
    (let [{:keys [team ally-interaction enemy-interaction] :as can-interact} (get-in game [:world :entities entity-i :components component-i])
          event (assoc-in event [:data :interactor-team] nil)]
      (if (= interactor-team team)
        [game (assoc-in event [:data :interaction] ally-interaction)]
        [game (assoc-in event [:data :interaction] enemy-interaction)]))
    [game event]))

;; COMPONENTS --------------------------------------------------------------------------------------

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
    (letfn [(transform-item-locations [item-location]
              (let [[item-name locations] item-location]
                (for [location locations]
                  {location item-name})))]
      (let [; new way [["Boots" [:feet]] ["Claymore" [:right-hand :left-hand]]]
            item-locations (select [ALL (specter/collect-one :nomen) :components ALL (eb/like {:nomen :can-be-equipped}) :equipped-in] contents)
            item-locations (apply merge (flatten (map transform-item-locations item-locations)))]
        (str
          "Equipment\n"
          (clojure.string/join
            (for [slot slots]
              (if (contains? item-locations (:nomen slot))
                (str "* " (:desc slot) " -- " ((:nomen slot) item-locations) "\n")
                (str "* " (:desc slot) " -- [empty]\n")))))))))
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
                                                                      :equipped-in []})))

(defrecord TakesTurnComponent [nomen priority tick data])
(defn TakesTurn [& args] (map->TakesTurnComponent (into args {:nomen :takes-turn, :priority 5, :tick normal-tick, :data {}})))

(defrecord DebugComponent [nomen priority debug])
(defn Debug [& args] (map->DebugComponent (into args {:nomen :debug, :priority 30, :debug normal-debug})))

(defrecord CanInteractComponent [nomen priority team get-interaction ally-interaction enemy-interaction])
(defn CanInteract [& args] (map->CanInteractComponent (into args {:nomen :can-interact
                                                                  :priority 10
                                                                  :team :enemies
                                                                  :get-interaction normal-get-interaction
                                                                  :enemy-interaction :attack
                                                                  :ally-interaction :speak})))
