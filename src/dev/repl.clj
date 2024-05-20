(ns repl
  (:require
    [shadow.cljs.devtools.api :as shadow]
    ))

(defn start
  {:shadow/requires-server true}
  []

  ;; optional, could also do this from the UI
  (shadow/watch :app)
  ::started)

(defn stop []
  ::stopped)

(defn go []
  (stop)
  (start))