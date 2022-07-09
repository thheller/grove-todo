(ns todo.ui.env
  (:require
    [shadow.grove.db :as db]
    [shadow.grove.runtime :as rt]
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
      (rt/prepare data-ref ::db)))

