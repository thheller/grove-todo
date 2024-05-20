(ns todo.ui.db
  (:require
    [shadow.grove.kv :as kv]
    [shadow.grove :as sg]
    [todo.model :as-alias m]
    ))

(sg/reg-event :app :ui/route!
  (fn [env {:keys [token] :as e}]

    ;; not much routing in this app, this will suffice
    (let [filter
          (case token
            "/completed" :completed
            "/active" :active
            :all)]

      (assoc-in env [::m/ui ::m/current-filter] filter))))

(defonce id-seq-ref (atom 0))

(sg/reg-event :app ::m/create-new!
  (fn [env {::m/keys [todo-text]}]
    (let [next-id (swap! id-seq-ref inc)
          new-todo {::m/todo-id next-id ::m/todo-text todo-text}]
      (-> env
          (kv/add ::m/todo new-todo)
          ))))

(sg/reg-event :app ::m/delete!
  (fn [env {:keys [todo-id]}]
    (update env ::m/todo dissoc todo-id)))

(sg/reg-event :app ::m/toggle-completed!
  (fn [env {:keys [todo-id]}]
    (update-in env [::m/todo todo-id ::m/completed?] not)))

(sg/reg-event :app ::m/edit-start!
  (fn [env {:keys [todo-id]}]
    (assoc-in env [::m/ui ::m/editing] todo-id)))

(sg/reg-event :app ::m/edit-save!
  (fn [env {:keys [todo-id text]}]
    (-> env
        (assoc-in [::m/todo todo-id ::m/todo-text] text)
        (assoc-in [::m/ui ::m/editing] nil))))

(sg/reg-event :app ::m/edit-cancel!
  (fn [env _]
    (assoc-in env [::m/ui ::m/editing] nil)))

(sg/reg-event :app ::m/clear-completed!
  (fn [env _]
    (update env ::m/todo
      (fn [table]
        (reduce-kv
          (fn [table todo-id {::m/keys [completed?] :as todo}]
            (if-not completed?
              table
              (dissoc table todo-id)))
          table
          table)))))

(sg/reg-event :app ::m/toggle-all!
  (fn [env {:keys [completed?]}]
    (update env ::m/todo
      (fn [table]
        (reduce-kv
          (fn [table todo-id todo]
            (if (not= completed? (::m/completed? todo))
              table
              (assoc-in table [todo-id ::m/completed?] completed?)))
          table
          table)))))