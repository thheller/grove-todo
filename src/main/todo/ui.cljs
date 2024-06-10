(ns todo.ui
  (:require
    [shadow.grove :as sg]
    [shadow.grove.history :as history]
    [todo.model :as-alias m]
    [todo.ui.views :as views]
    [todo.ui.db]))

(defonce rt-ref
  (sg/get-runtime :app))

(defonce root-el
  (js/document.getElementById "app"))

(defn render []
  (sg/render rt-ref root-el
    (views/ui-root)))

(defn init []
  (sg/add-kv-table rt-ref ::m/ui
    {}
    {::m/editing nil})

  (sg/add-kv-table rt-ref ::m/todo
    {:primary-key ::m/todo-id}
    (sorted-map-by >))

  (history/init! rt-ref
    {:use-fragment true
     :start-token "/all"})

  (render))

(defn ^:dev/after-load reload! []
  (render))