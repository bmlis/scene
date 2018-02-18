(ns scene.utils
  (:require [clojure.core.async :refer [<! >! chan close! onto-chan put!]]
            [taoensso.timbre :refer-macros [error info]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn logger
  "logger for `->` and `->>` macros"
  [x]
  (info (js->clj x))
  x)


(defn logger-fn [desc]
  (fn [data]
    (info desc)
    data))


(defn callback->clj
  "convert data from node style callback function to clojure"
  [err d]
  (when err
    (error (.toString err)))
  {:data d
   :error err})


(defn callback-chan-fn
  "return node style callback function"
  [ch]
  (fn [error data]
    (put! ch (callback->clj error data))))


(defn promise->chan
  "return channel with result from promise"
  ([promise]
   (promise->chan promise (chan 1)))
  ([promise ch]
    (-> promise
        (.then (fn [data]
                 (put! ch data :keywordize-keys true)))
        (.catch (fn [err]
                  (error (.toString err))
                  (onto-chan ch [{:error err}]))))
    ch))


(defn js->json
  "convert js object to json"
  [obj]
  (.stringify js/JSON obj))

(defn json->js
  "convert json to js object"
  [s]
  (.parse js/JSON s))


(defn clj->json
  "convert clojure object to json"
  [m]
  (-> m
      clj->js
      js->json))

(defn json->clj
  "convert json to clojure object"
  [s]
  (-> s
      json->js
      (js->clj :keywordize-keys true)))


(defn cursor->chan
  "conver mongdb cursor to chan"
  ([cur]
   (cursor->chan cur (chan 1)))
  ([cur ch]
   (go
     (while (-> cur
             (.hasNext)
             promise->chan
             <!)
       (->> cur
            (.next)
            promise->chan
            <!
            (>! ch)))
     (close! ch))))


(defn int->hex
  ([n]
   (int->hex n 0))
  ([n pad-to]
   (.padStart (.toString n 16)
              pad-to
              "0")))
