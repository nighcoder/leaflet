(ns clojupyter-plugin.leaflet.colors)

(defn abs
  [x]
  (if (neg? x) (- x) x))

(defn str->rgb
  "Converts a string colour to a three int rbg tuple"
  [s]
  (->> (rest s)
       (partition 2)
       (map (partial reduce str))
       (map #(Integer/parseInt % 16))))

(defn rgb->hsv
  [clr]
  (let [[r g b] (map #(/ % 255) clr)
        c-max (max r g b)
        c-min (min r g b)
        dif (- c-max c-min)
        v c-max
        s (if (zero? dif) 0 (/ dif c-max))
        h (if (zero? dif)
            0
            (condp = c-max
              r (* 60 (mod (/ (- g b) dif) 6))
              g (* 60 (+ 2 (/ (- b r) dif)))
              b (* 60 (+ 4 (/ (- r g) dif)))))]
    (map float (list h s v))))

(defn hsv->rgb
  [[h s v]]
  {:pre [(<= 0 h 360)
         (<= 0 s 1)
         (<= 0 v 1)]}
  (let [c (* v s)
        q (/ h 60)
        x (* c (- 1 (abs (dec (mod q 2)))))
        [r1 g1 b1] (condp #(<= (first %1) %2 (second %1)) q
                     [0 1] [c x 0]
                     [1 2] [x c 0]
                     [2 3] [0 c x]
                     [3 4] [0 x c]
                     [4 5] [x 0 c]
                     [5 6] [c 0 x]
                     :else [0 0 0])
        m (- v c)]
    (map (comp int (partial * 255) (partial + m)) [r1 g1 b1])))

(defn rgb->str
 [[r g b]]
 {:pre [(every? int? [r g b])
        (every? #(<= 0 % 255) [r g b])]}
 (format "#%02X%02X%02X" r g b))

(defn single-hue-linear-fn
  ([v-min v-max] (single-hue-linear-fn v-min v-max "#BE481F"))
  ([v-min v-max color]
   (fn [x]
     {:pre [(<= v-min x v-max)
            (< v-min v-max)]}
     (let [[h _ v] (-> color str->rgb rgb->hsv)]
       (-> (list h (float (/ (- x v-min) (- v-max v-min))) v)
           hsv->rgb
           rgb->str)))))

(def nothing nil)
