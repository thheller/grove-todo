(ns todo.ui.env
  (:require
    [shadow.grove :as sg]
    [shadow.grove.db :as db]
    [todo.model :as-alias m]))

(def schema
  {::m/todo
   {:type :entity
    :primary-key ::m/todo-id
    :attrs {}
    :joins {}}
   })

(defonce data-ref
  (-> {::m/id-seq 0
       ::m/editing nil}
      (db/configure schema)
      (atom)))

(defonce rt-ref
  (-> {}
      (sg/prepare data-ref ::db)))

