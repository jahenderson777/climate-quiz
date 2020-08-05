(ns climate-quiz.core
  (:require [uix.dom.alpha :as uix.dom]
            [uix.core.alpha :as uix]
            [xframe.core.alpha :as xf :refer [<sub]]
            [ajax.core :as ajax]
            ["react" :as react]
            ["framer-motion" :refer [motion AnimatePresence useAnimation]]
            ["smoothscroll-polyfill" :as smooth]
            ))

(set! *warn-on-infer* true)

(xf/reg-fx :http-get
           (fn [_ [_ {:keys [url on-ok on-failed]}]]
             (ajax/GET url {:handler #(xf/dispatch [on-ok %])
                            :error-handler (if on-failed
                                             #(xf/dispatch [on-failed %])
                                             println)
                            :keywords? true
                            :response-format :json})))

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
   (merge db data)
   ;(assoc db :quiz (subvec (:quiz data) 0 2))
   ))

(xf/reg-event-fx
 :select-answer
 (fn [db [_ q-id a-id]]
   {:db (assoc-in db [:quiz q-id :selected] a-id)
    :http-post {:url "https://www.appezium.co.uk/quiz.php"
                :params {:id (:id db)
                         :q-id q-id
                         :a-id a-id}}}))

(xf/reg-sub
 :get
 (fn [& ks]
   (get-in (xf/<- [::xf/db]) ks)))

(xf/reg-sub
 :current-question
 (fn [_]
   (inc (count (filter :selected (xf/<- [:get :quiz]))))))

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

(defn hsl
  ([hue sat lightness]
   (hsl hue sat lightness 1))
  ([hue sat lightness alpha]
   (str "hsla(" hue ", " sat "%, " lightness "%, " alpha ")")))

(defn gradient [direction & stops]
  (str "linear-gradient(" direction "deg,"
       (apply str (interpose ", "
                             (for [[h s l a pct-pos] stops]
                               (str (hsl h s l a) " " pct-pos "% "))))
       ")"))

(def colors 
  ["rgb(247, 238, 106)"
   "rgb(117, 208, 241)"
   "rgb(237, 155, 196)"
   ;"rgb(152, 98, 151)"
   "rgb(190, 210, 118)"
   "rgb(255, 193, 80)"
   "rgb(207, 98, 151)"
   "rgb(230, 89, 80)"
   "rgb(86, 196, 220)"
   "rgb(220, 80, 170)"])

(defn answer-component [idx answer q-id selected]
  [:> (.-div motion) {:class "answer"
                      :initial #js {:opacity 0}
                      :animate #js {:opacity 1}
                      :transition #js {:duration 0.20
                                       :delay (+ 0.35 (* idx 0.2))}
                      :style {:background-color (when (= idx selected) "#98f")}
                      :on-click #(xf/dispatch [:select-answer q-id idx])}
   [:> (.-div motion)
    {:whileHover #js {:opacity 0.5}
     :whileTap #js {:opacity 0.5}}
    answer]])

(defn slide [{:keys [hue dir bg]} & content]
  (let [ref (uix/ref)]
    (uix/effect! (fn []
                   (js/window.scrollTo #js {:left 0
                                            :top (.. @ref -offsetTop)
                                            :behavior "smooth"}))
                 [])
    [:div.slide {:ref ref
                 :style {:min-height (second (<sub [:get :window-size]))
                         :background-color bg
                         #_#_:background (gradient dir
                                               [hue 10 90 1 0]
                                               [hue 10 90 1 60]
                                               [0 70 88 1 100])}}
     [:div.slide-inner
      content]]))

(defn question [q-id]
  (let [{:keys [question answers correct-index selected]} (<sub [:get :quiz q-id])]
    [slide {:bg (nth colors (mod q-id (count colors)))
            :hue (* 90 q-id)
            :dir (if (even? q-id) 90 270)}
     [:div.progress
      (str "Question " (inc q-id) " of " (<sub [:num-questions]))]
     [:h2 question]
     (map-indexed
      (fn [idx answer]
        ^{:key idx}
        [answer-component idx answer q-id selected])
      answers)]))

(defn complete-block []
  [slide {:hue 300 :dir 180 :bg "rgb(152, 68, 151)"}
   [:div {:style {:color "#fff"}}
    [:h1 "Great job!"]
    [:h2 (str "You scored " (<sub [:num-correct]) " out of " (<sub [:num-questions]))]
    [:h2 "Share this quiz with your friends to see if they can beat your score..."]
    [:div {:style {:text-align "center"}}
     [:button.btn-facebook {:on-click #(js/window.open 
                                        (str "https://facebook.com/sharer.php?u="
                                             (js/encodeURIComponent "https://quiz.earthics.org")))}
      [:img {:src "icon-facebook.png"}]
      "Share on Facebook"]]]])

(defn ask-for-help-block []
  [slide {:hue 300 :dir 180 :bg "rgb(68, 151, 152)"}
   [:div {:style {:color "#fff"}}
    [:h1 "Quiz complete!"]
    [:h2 "While you wait for your results will you sign a petition demanding the UK governemnt take more urgent action on the climate emergency?"]
    [:div 
     [:button.btn-sign {:on-click #(xf/dispatch [:set [:user-will-help] true])}
           "OF COURSE - ADD MY NAME"]]
    [:div 
     [:button.btn-no {:on-click #(xf/dispatch [:set [:user-will-help] false])} 
           "NO, SORRY"]]]])

(defn help-block []
  [slide {:hue 300 :dir 180 :bg "rgb(153, 122, 48)"}
   [:div {:style {:color "#fff"}}
    [:h2 "Enter your email"]
    [:div
     [:input {:type "text" :name "email" :placeholder "Email"}]]
    
    [:div 
     [:button.btn-sign {:on-click #(xf/dispatch [:set [:user-helped] true])}
           "SIGN"]]]])

(defn main []
  (let [user-will-help (<sub [:get :user-will-help])
        user-helped (<sub [:get :user-helped])]
    [:<>
     [:header
      "CLIMATE QUIZ"]
     [:div#content
      (for [q-id (range (min (<sub [:current-question])
                             (<sub [:num-questions])))]
        ^{:key q-id}
        [question q-id])
      
      (when (<sub [:complete?])
        [:<>
         ^{:key -1}
         [ask-for-help-block]
         (when user-will-help
           ^{:key -2}
           [help-block])
         (when (or user-helped
                   (= false user-will-help))
           ^{:key -3}
           [complete-block])])]]))

(defn init-fn []
  (.polyfill smooth)
  (xf/dispatch [:db/init]))

(defn start []
  (defonce init (init-fn))
  (uix.dom/render [main] (.getElementById js/document "app")))

(start)
