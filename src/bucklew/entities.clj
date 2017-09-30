(ns bucklew.entities)

(defprotocol EntityProtocol
	())

(defrecord Entity [id nomen components]
	(receive-event [this event]))