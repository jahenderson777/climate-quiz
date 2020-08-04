(ns climate-quiz.core
  (:require [uix.dom.alpha :as uix.dom]
            [uix.core.alpha :as uix]
            [xframe.core.alpha :as xf :refer [<sub]]
            [ajax.core :as ajax]
            ;[cljs-bean.core :as bean]
            ["react" :as react]
            ["framer-motion" :refer [motion AnimatePresence useAnimation]]
            ))

(set! *warn-on-infer* true)

(xf/reg-fx :http-get
           (fn [_ [_ {:keys [url on-ok on-failed]}]]
             (ajax/GET url {:handler #(xf/dispatch [on-ok %])
                            :error-handler (if on-failed
                                             #(xf/dispatch [on-failed %])
                                             println)
                            :keywords? true
                            :response-format :json})
              #_(-> (js/fetch url)
                  (.then #(if (.-ok %)
                            (.json %)
                            (if on-failed
                              (xf/dispatch [on-failed %])
                              (println %))))
                  (.then #(js->clj % :keywordize-keys true))
                  (.then #(xf/dispatch [on-ok %])))))

(xf/reg-fx :http-post
           (fn [_ [_ {:keys [url params]}]]
             (ajax/POST url {:handler println
                             :error-handler println
                             :params params
                             :format :raw})))

(xf/reg-event-fx
 :db/init
 (fn [_ _]
   {:http-get {:url "/quiz001.json"
           :on-ok :merge}
    :db {:id (random-uuid)
         :scroll 0
         :current-question 1
         :window-size [js/window.innerWidth js/window.innerHeight]}}))

(xf/reg-event-db
 :set
 (fn [db [_ ks v]]
   (assoc-in db ks v)))

(xf/reg-event-db
 :merge
 (fn [db [_ data]]
   (merge db data)))

(xf/reg-event-fx
 :select-answer
 (fn [db [_ q-id a-id]]
   {:db (-> db
            (update :current-question (fn [old-current]
                                        (if (<= old-current (inc q-id))
                                          (inc old-current)
                                          old-current)))
            (assoc-in [:quiz q-id :selected] a-id))
    :http-post {:url "https://www.appezium.co.uk/quiz.php"
                :params {:id (:id db)
                         :q-id q-id
                         :a-id a-id}}}))

(xf/reg-sub
 :get
 (fn [& ks]
   (get-in (xf/<- [::xf/db]) ks)))

(xf/reg-sub
 :questions
 (fn [_]
   (if-let [quiz (xf/<- [:get :quiz])]
     (subvec quiz
             0
             (xf/<- [:get :current-question]))
     [])))

(xf/reg-sub
 :complete?
 (fn [_]
   (when-let [quiz (xf/<- [:get :quiz])]
     (every? :selected quiz))))

(xf/reg-sub
 :num-correct
 (fn [_]
   (reduce (fn [cnt q]
             (if (= (:selected q)
                    (:correct-index q))
               (inc cnt)
               cnt))
           0
           (xf/<- [:get :quiz]))))

(xf/reg-sub
 :num-questions
 (fn [_]
   (count (xf/<- [:get :quiz]))))

#_(defn animated []
  [:> (.-div motion)
   {:initial #js {:opacity 1}
    :animate #js {:opacity 0.2}
    :transition #js {:duration 2}
    }
   "Animated element2"])

#_(defn use-animation-component []
  (let [controls (useAnimation)]
    (react/useEffect (fn []
                       (.start controls (fn [i]
                                          #js {;; :opacity 0
                                               :x 100
                                               :transition #js {:delay (* 0.3 i)}}))
                       (fn []))
                     #js [])
    [:ul
     (for [i (range 3)]
       [:> (.-li motion)
        {:key i
         :custom i
         :animate controls}
        "Item " i])]))

(defn question [q-id]
  (let [ref (uix/ref)
        {:keys [question answers correct-index selected]} (<sub [:get :quiz q-id])]
    (uix/effect! (fn []
                   (js/window.scrollTo #js {:left 0
                                            :top (.. @ref -offsetTop)
                                            :behavior "smooth"}))
                 [])
    [:div {:ref ref
           :style {:min-height (second (<sub [:get :window-size]))}}
     [:h2 question]
     (map-indexed
      (fn [idx answer]
        ^{:key idx}
        [:> (.-div motion) {:class "answer"
                            :initial #js {:opacity 0}
                            :animate #js {:opacity 1}
                            :transition #js {:duration 0.20
                                             :delay (+ 0.35 (* idx 0.2))}
                            :style {:background-color (when (= idx selected) "#0f0")}
                            :on-click #(xf/dispatch [:select-answer q-id idx])}
         answer])
      answers)]))

(comment
  [:h1 "AnimatePresence223"]
  [:> (.-div motion) {:style {:background-color "green"
                              :display "block"
                              :width 200}
                      :transition #js {:duration 2}

                      :animate #js {:scale 0.3}}
   "FOOO"]
  [:> (.-div motion) {:style {:color "red"
                              :background-color "green"}

                      :whileHover #js {:scale 2}}
   "a link"]

  (when (<sub [:get :foo])
    [animated])
  [:button
   {:on-click #(xf/dispatch [:set :foo true])}
   "Toggle"]

  [:h1 "useAnimation"]
  [use-animation-component])

(defn complete-block []
  (let [ref (uix/ref)
        _ (uix/effect! #(js/window.scrollTo #js {:left 0
                                                 :top (.. @ref -offsetTop)
                                                 :behavior "smooth"})
                       [])]
    [:div {:ref ref
           :style {:background-color "#8a8"
                   :min-height (second (<sub [:get :window-size]))}}
     [:div "quiz complete"]
     [:div (str "you scored " (<sub [:num-correct]) " out of " (<sub [:num-questions]))]]))

(defn main []
  [:div#content
   (for [q-id (range (<sub [:get :current-question]))]
     ^{:key q-id}
     [question q-id])
   
   (when (<sub [:complete?])
     [complete-block])])

(defn init-fn []
  (xf/dispatch [:db/init]))

(defn start []
  (defonce init (init-fn))
  (uix.dom/render [main] (.getElementById js/document "app")))

(start)
