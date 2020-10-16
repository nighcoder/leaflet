(ns clojupyter-plugin.leaflet
  (:require [camel-snake-kebab.core :as csk]
            [clojupyter-plugin.leaflet.colors :as col]
            [clojupyter-plugin.widgets.control :as inter]
            [clojupyter-plugin.widgets :as widget]
            [clojupyter.kernel.comm-atom :as ca]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))


(def ^:private SPECS (-> "leaflet-schema.min.json"
                         io/resource
                         slurp
                         json/parse-string))

(def BASE-MAPS
  (let [base-maps (json/parse-string (slurp (io/resource "basemaps.min.json")))
        k-maps (for [[outer-key v] base-maps]
                 (if (contains? v "name")
                   {(csk/->kebab-case-keyword outer-key) (walk/keywordize-keys v)}
                   (reduce merge
                     (for [[inner-key vv] v]
                       {(csk/->kebab-case-keyword (str outer-key inner-key)) (walk/keywordize-keys vv)}))))]
    (reduce merge k-maps)))

(defn- def-widget
  [{attributes "attributes"}]
  (reduce merge
    (for [{name "name" default "default" type "type"} attributes]
      {(keyword name) (cond
                        (= type "reference") nil
                        :else default)})))

(declare tile-layer zoom-control attribution-control geo-json map-style)

(defn ->base-layer
  [basemap date]
  (let [bm_ (get BASE-MAPS basemap)
        bm_ (if (clojure.string/includes? (:url bm_) "%s")
              (update bm_ format date)
              bm_)
        bm_ (assoc bm_ :base true)]
    (apply tile-layer (reduce concat (list* bm_)))))

(defn- make-widget
  [spec]
  (fn constructor
    [& {:as args}]
    (let [d-state (def-widget spec)
          base (ca/base-widget d-state)]
      (swap! base merge args)
      (if (= (get spec "name") "map")
        ;; Are we generating a map?
        (let [{:keys [basemap modisdate extra-controls? layout style default_style dragging_style]
               :or {basemap :open-street-map-mapnik
                    modisdate (:modisdate d-state)
                    extra-controls? true}} args]
          (swap! base assoc :basemap basemap :extra-controls? extra-controls?)
          (when-not layout
            (swap! base assoc :layout (widget/layout)))
          (when-not style
            (swap! base assoc :style (map-style :cursor "hand")))
          (when-not default_style
            (swap! base assoc :default_style (map-style :cursor "hand")))
          (when-not dragging_style
            (swap! base assoc :dragging_style (map-style :cursor "grabbing")))
          (when extra-controls?
            (swap! base update :controls (comp vec concat) [(zoom-control) (attribution-control :position "bottomright")]))
          (when basemap
            (let [date (if (= "yesterday" modisdate)
                         (doto (java.time.LocalDate/now)
                               (.minusDays 1)
                               (.toString))
                         modisdate)
                  bm_ (->base-layer basemap date)]
              (swap! base update :layers #(into [bm_] %))))
          (add-watch base :internal-consistency
            (fn [_ _ {old-basemap :basemap old-modisdate :modisdate} {new-basemap :basemap new-modisdate :modisdate}]
              (when (or (not= old-basemap new-basemap)
                        (not= old-modisdate new-modisdate))
                (let [bm_ (->base-layer new-basemap new-modisdate)]
                  (.close (first (:layers @base)))
                  (swap! base update :layers #(into [bm_] (rest %)))))))
          base)
        ;; We are generating anything but a map
        base))))

(doseq [{n "name" :as spec} SPECS]
  (eval `(def ~(symbol n) ~(make-widget spec))))

(defn choropleth
  [& {:keys [_data color-fn key-path data] :or {_data {} key-path [:id] data {}} :as args}]
  (let [base-color "#E96622"
        def-color-fn (fn [data]
                       (if (empty? (vals data))
                         (constantly base-color)
                         (let [v-min (reduce min (vals data))
                               v-max (reduce max (vals data))]
                           (if (= v-min v-max)
                             (constantly base-color)
                             (col/single-hue-linear-fn v-min v-max base-color)))))
        color-fn (or color-fn def-color-fn)
        color-feature (fn [feat data c-fn key-path]
                        (let [k (get-in feat key-path)]
                          (if-let [v (get data k)]
                            (assoc-in feat ["properties" "style"] {:fillColor (c-fn v) :color "black" :weight 0.9})
                            feat)))
        color-geo-data (fn [{type "type" :as geo-data} data c-fn key-path]
                        (case type
                           "Feature" (color-feature geo-data data c-fn key-path)
                           "FeatureCollection" (update geo-data "features"
                                                 ;; Arguably pmap is overkill, especially because its performance is dependent on data size
                                                 ;; Tested on 5MB GEO-JSON on 4-core PC it reduces the loading time from ~1700 to ~800 ms.
                                                 (fn [feat] (reduce concat (pmap (partial clojure.core/map #(color-feature % data c-fn key-path)) (partition 8 8 (repeat {}) feat))))
                                                 #_(fn [feat] (mapv #(color-feature % data c-fn key-path) feat)))
                           geo-data))
        state (assoc args
                :color-fn color-fn
                :data (color-geo-data data _data (color-fn _data) key-path)
                :_data _data
                :key-path key-path)
        d-state (def-widget (first (filter (comp (partial = "choropleth") #(get % "name")) SPECS)))
        state (merge d-state state)
        w (apply geo-json (reduce concat (list* state)))]
    (inter/tie! (fn [data] (let [{:keys [_data key-path color-fn]} @w] (color-geo-data data _data (color-fn _data) key-path))) w w :data :data)
    (inter/tie! (fn [_data] (let [{:keys [data color-fn key-path]} @w] (color-geo-data data _data (color-fn _data) key-path))) w w :_data :data)
    (inter/tie! (fn [key-path] (let [{:keys [data _data color-fn]} @w] (color-geo-data data _data (color-fn _data) key-path))) w w :key-path :data)
    (inter/tie! (fn [color-fn] (let [{:keys [data _data key-path]} @w] (color-geo-data data _data (color-fn _data) key-path))) w w :color-fn :data)
    w))
