(ns sample1.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample1.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(defn nav-link [uri title page]
      [:a.navbar-item
       {:href  uri
        :class (when (= page (:page @session)) "is-active")}
       title])

(defn navbar []
      (r/with-let [expanded? (r/atom false)]
                  [:nav.navbar.is-info>div.container
                   [:div.navbar-brand
                    [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample1"]
                    [:span.navbar-burger.burger
                     {:data-target :nav-menu
                      :on-click    #(swap! expanded? not)
                      :class       (when @expanded? :is-active)}
                     [:span] [:span] [:span]]]
                   [:div#nav-menu.navbar-menu
                    {:class (when @expanded? :is-active)}
                    [:div.navbar-start
                     [nav-link "#/" "Home" :home]
                     [nav-link "#/about" "About" :about]]]]))

(defn about-page []
      [:section.section>div.container>div.content
       [:img {:src "/img/warning_clojure.png"}]])

(def addTotal (r/atom {:x 1 :y 2 :myTotal 3}))

(defn storeTotal [myTotal]
      (swap! addTotal assoc :myTotal myTotal))


(defn plusCall [addTotal]
      (let [x (:x addTotal)
            y (:y addTotal)]
           (POST "/api/math/plus" {:params  {:x x :y y}
                                   :handler #(storeTotal (:total %))})))

(defn plusCall2 [xvalue yvalue]

      (POST "/api/math/plus" {:params  {:x xvalue :y yvalue}
                              :handler #(storeTotal (:total %))}))

(defn input-field [tag id]
      [:div.field
       [tag
        {:type        :number
         :value       (id @addTotal)
         :placeholder (name id)
         :on-change   #(do
                         (swap! addTotal assoc id (js/parseInt (-> % .-target .-value)))
                         (plusCall @addTotal))
         }]])

(defn form []
      (fn []
          [:section.section>div.container>div.content
           [:p "Testing home page $$"]
           [:p (str @addTotal)]
           [:div]
           [input-field :input.input :x ]
           [input-field :input.input :y ]
           [:div]
           [:p "Total = " (:myTotal @addTotal)]
           [:div]
           [:button {:on-click #(plusCall2 2 6)} "compute 2 + 6"]
           [:button {:on-click #(plusCall2 9 4)} "compute 9 + 4"]
           ])
      )

(defn home-page []
      [form])

(comment (shadow/repl :app)                                 ;alt shift l to load ;  control alt shift l
         (def mymap {:key 3 :key2 5})
         mymap
         (:key mymap)
         (plusCall2 3 7)


         @addItem
         (reset! addItem {:mytotal 4})
         (swap! addIte1 conj {:mytotal2 6})

         )


(def pages
  {:home  #'home-page
   :about #'about-page})

(defn page []
      [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]]))

(defn match-route [uri]
      (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
           (reitit/match-by-path router)
           :data
           :name))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
      (doto (History.)
            (events/listen
              HistoryEventType/NAVIGATE
              (fn [^js/Event.token event]
                  (swap! session assoc :page (match-route (.-token event)))))
            (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
      (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn ^:dev/after-load mount-components []
      (rdom/render [#'navbar] (.getElementById js/document "navbar"))
      (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
      (ajax/load-interceptors!)
      (fetch-docs!)
      (hook-browser-navigation!)
      (mount-components))
