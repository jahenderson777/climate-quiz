## scratch 

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

              ;[cljs-bean.core :as bean]