(ns bucklew.entities)

(defprotocol EntityProtocol
	(add-component [this component])
	(receive-event [this event]))

(defrecord Entity [id nomen components]
	EntityProtocol
	(receive-event [this event] "blaah"))
