(ns todo.ui.views
  (:require
    [shadow.grove :as sg :refer (<< defc)]
    [todo.model :as-alias m]))

(defc todo-item [todo-id]
  (bind {::m/keys [completed? todo-text] :as data}
    (sg/kv-lookup ::m/todo todo-id))

  (bind editing?
    (= todo-id (sg/kv-lookup ::m/ui ::m/editing)))

  (render
    (<< [:li {:class {:completed completed?
                      :editing editing?}}
         [:div.view
          [:input.toggle {:type "checkbox"
                          :checked completed?
                          :on-change {:e ::m/toggle-completed! :todo-id todo-id}}]
          [:label {:on-dblclick {:e ::m/edit-start! :todo-id todo-id}} todo-text]
          [:button.destroy {:on-click {:e ::m/delete! :todo-id todo-id}}]]

         (when editing?
           (<< [:input#edit.edit {:autofocus true
                                  :on-keydown {:e ::m/edit-update! :todo-id todo-id}
                                  :on-blur {:e ::m/edit-complete! :todo-id todo-id}
                                  :value todo-text}]))]))

  (event ::m/edit-complete! [env {:keys [todo-id]} e]
    (when editing? ;; don't save after escape/cancel
      (sg/run-tx env {:e ::m/edit-save! :todo-id todo-id :text (.. e -target -value)})))

  (event ::m/edit-update! [env {:keys [todo-id]} e]
    (case (.-which e)
      13 ;; enter
      (.. e -target (blur))
      27 ;; escape
      (sg/run-tx env {:e ::m/edit-cancel! :todo-id todo-id})
      ;; default do nothing
      nil)))

(defc ui-filter-select []
  (bind current-filter
    (sg/kv-lookup ::m/ui ::m/current-filter))

  (bind filter-options
    [{:label "All" :value :all}
     {:label "Active" :value :active}
     {:label "Completed" :value :completed}])

  (render
    (<< [:ul.filters
         (sg/keyed-seq filter-options :value
           (fn [{:keys [label value]}]
             (<< [:li [:a
                       {:class {:selected (= current-filter value)}
                        :ui/href (str "/" (name value))}
                       label]])))])))

(defn ?todo-stats [{::m/keys [todo] :as env}]
  (let [current-filter
        (get-in env [::m/ui ::m/current-filter])

        filter-fn
        (case current-filter
          :all
          (fn [x] true)
          :active
          #(not (::m/completed? %))
          :completed
          #(true? (::m/completed? %)))]

    (reduce
      (fn [m {::m/keys [completed?] :as todo}]
        (-> m
            (update (if completed? :num-completed :num-active) inc)
            (cond->
              (filter-fn todo)
              (update :todos conj (::m/todo-id todo)))))
      {:num-total (count todo)
       :num-active 0
       :num-completed 0
       :todos []}
      ;; sorting here to things are processed in order
      ;; and not the order used by the (potential) hash map
      (sort (vals todo)))))

(defc ui-root []
  (bind {:keys [num-total num-active num-completed todos] :as query}
    (sg/query ?todo-stats))

  (render
    (<< [:header.header
         [:h1 "todos"]
         [:input.new-todo {:on-keydown {:e ::m/create-new!}
                           :placeholder "What needs to be done?"
                           :autofocus true}]]

        (when (pos? num-total)
          (<< [:section.main
               [:input#toggle-all.toggle-all
                {:type "checkbox"
                 :on-change {:e ::m/toggle-all!}
                 :checked false}]
               [:label {:for "toggle-all"} "Mark all as complete"]

               [:ul.todo-list
                (sg/keyed-seq todos identity todo-item)]

               [:footer.footer
                [:span.todo-count
                 [:strong num-active] (if (= num-active 1) " item" " items") " left"]

                (ui-filter-select)

                (when (pos? num-completed)
                  (<< [:button.clear-completed {:on-click {:e ::m/clear-completed!}} "Clear completed"]))]]))))

  (event ::m/create-new! [env _ ^js e]
    (when (= 13 (.-keyCode e))
      (let [input (.-target e)
            text (.-value input)]

        (when (seq text)
          (set! input -value "") ;; FIXME: this triggers a paint so should probably be delayed?
          (sg/run-tx env {:e ::m/create-new! ::m/todo-text text})))))

  (event ::m/toggle-all! [env _ e]
    (sg/run-tx env {:e ::m/toggle-all! :completed? (-> e .-target .-checked)})))