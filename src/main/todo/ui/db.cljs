(ns todo.ui.db
  (:require
    [shadow.grove.eql-query :as eql]
    [shadow.grove.db :as db]
    [shadow.grove :as sg]
    [todo.ui.env :as env]
    [todo.model :as-alias m]
    ))

(defmethod eql/attr ::m/num-active
  [env db current _ params]
  (->> (db/all-of db ::m/todo)
       (remove ::m/completed?)
       (count)))

(defmethod eql/attr ::m/num-completed
  [env db current _ params]
  (->> (db/all-of db ::m/todo)
       (filter ::m/completed?)
       (count)))

(defmethod eql/attr ::m/num-total
  [env db current _ params]
  (count (db/all-of db ::m/todo)))

(defmethod eql/attr ::m/editing?
  [env db current _ params]
  (= (::m/editing db) (:db/ident current)))

(defmethod eql/attr ::m/filtered-todos
  [env {::m/keys [current-filter] :as db} current _ params]
  (let [filter-fn
        (case current-filter
          :all
          (fn [x] true)
          :active
          #(not (::m/completed? %))
          :completed
          #(true? (::m/completed? %)))]

    (->> (db/all-of db ::m/todo)
         (filter filter-fn)
         (map :db/ident)
         (sort)
         (vec))))

(defn without [items del]
  (into [] (remove #{del}) items))

(defn r-> [init rfn coll]
  (reduce rfn init coll))

(sg/reg-event env/rt-ref :ui/route!
  (fn [env {:keys [token] :as e}]

    ;; not much routing in this app, this will suffice
    (let [filter
          (case token
            "/completed" :completed
            "/active" :active
            :all)]

      (assoc-in env [:db ::m/current-filter] filter))))

(sg/reg-event env/rt-ref ::m/create-new!
  (fn [env {::m/keys [todo-text]}]
    (update env :db
      (fn [db]
        (let [{::m/keys [id-seq]} db]
          (let [new-todo {::m/todo-id id-seq ::m/todo-text todo-text}]
            (-> db
                (update ::m/id-seq inc)
                (db/add ::m/todo new-todo [::m/todos]))))))))

(sg/reg-event env/rt-ref ::m/delete!
  (fn [env {:keys [todo]}]
    (update env :db
      (fn [db]
        (-> db
            (dissoc todo)
            (update ::m/todos without todo))))))

(sg/reg-event env/rt-ref ::m/toggle-completed!
  (fn [env {:keys [todo]}]
    (update-in env [:db todo ::m/completed?] not)))

(sg/reg-event env/rt-ref ::m/edit-start!
  (fn [env {:keys [todo]}]
    (assoc-in env [:db ::m/editing] todo)))

(sg/reg-event env/rt-ref ::m/edit-save!
  (fn [env {:keys [todo text]}]
    (update env :db
      (fn [db]
        (-> db
            (assoc-in [todo ::m/todo-text] text)
            (assoc ::m/editing nil))))))

(sg/reg-event env/rt-ref ::m/edit-cancel!
  (fn [env _]
    (assoc-in env [:db ::m/editing] nil)))

(sg/reg-event env/rt-ref ::m/clear-completed!
  (fn [env _]
    (update env :db
      (fn [db]
        (-> db
            (r->
              (fn [db {::m/keys [completed?] :as todo}]
                (if-not completed?
                  db
                  (db/remove db todo)))
              (db/all-of db ::m/todo))
            (update ::m/todos (fn [current]
                                (into [] (remove #(get-in db [% ::m/completed?])) current))))
        ))))

(sg/reg-event env/rt-ref ::m/toggle-all!
  (fn [env {:keys [completed?]}]
    (update env :db
      (fn [db]
        (reduce
          (fn [db ident]
            (assoc-in db [ident ::m/completed?] completed?))
          db
          (db/all-idents-of db ::m/todo))))))

