(ns lamina.examples.chat-client
  (:require
   [lamina.core :as core]))

(defn attribute [name, msg]
  (str name ": " msg))

(defn unattributed? [name, msg]
  (not (re-find (re-pattern (str "^" name ":")) msg)))

;; With channels only -- no pipelines
(defn chat-handler
  "Example of use:

   (require '[lamina.examples.chat-client :as chat])
   (require '[lamina.core :as core])

   (def channel1 (core/channel \"michael\" \"lamina-chat\"))
   (chat/chat-handler channel1)
   (core/receive-all channel1 println)
   (core/enqueue channel1 \"Hello, world.\")
  "
  ([ch]
     (core/receive
      ch
      (fn [name]
	(core/receive
	 ch
	 (fn [chatroom-name]
	   (let [chatroom  (core/named-channel chatroom-name)
		 map-fn    (partial attribute name)
		 filter-fn (partial unattributed? name)]
	     (core/siphon (->> ch (core/filter* filter-fn) (core/map* map-fn)) chatroom)
	     (core/siphon chatroom ch))))))))


;; With pipelines
(defn pipelined-chat-handler
  " Example of use:
   (require '[lamina.examples.chat-client :as chat])
   (require '[lamina.core :as core])

   (def channelp (core/channel \"michael\" \"another-lamina-chat\"))
   (chat/pipelined-chat-handler channelp)
   (core/receive-all channelp println)
   (core/enqueue channelp \"Hello, world.\")
  "
  ([ch]
   (core/run-pipeline {}
     (core/read-merge #(core/read-channel ch) #(assoc %1 :name %2))
     (core/read-merge #(core/read-channel ch) #(assoc %1 :room %2))
     (fn [params]
       (let [chatroom-channel (core/named-channel (:room params))
	     map-fn           (partial attribute (:name params))
	     filter-fn        (partial unattributed? (:name params))]
	 (core/siphon (->> ch (core/filter* filter-fn) (core/map* map-fn)) chatroom-channel)
	 (core/siphon chatroom-channel ch))))))
