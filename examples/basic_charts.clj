(ns hanami.basic-charts

  (:require [clojure.string :as cljstr]
            #_[clojure.data.csv :as csv]
            [clojure.data.json :as json]

            #_[aerial.utils.math.probs-stats :as p]
            #_[aerial.utils.math.infoth :as it]

            #_[aerial.utils.string :as str]
            #_[aerial.utils.coll :as coll]

            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.core :as hmi]))



(defn log2 [x]
  (let [ln2 (Math/log 2)]
    (/ (Math/log x) ln2)))



(hmi/start-server 3000 :idfn (constantly "Basics"))
#_(hmi/stop-server)


;;; Simple Barchart with template
(->>
 (let [data [{:a "A", :b 28 },
             {:a "B", :b 55 },
             {:a "C", :b 43 },
             {:a "D", :b 91 },
             {:a "E", :b 81 },
             {:a "F", :b 53 },
             {:a "G", :b 19 },
             {:a "H", :b 87 },
             {:a "I", :b 52 }]]
   (hc/xform ht/simple-bar-chart
             :TITLE "A Simple Bar Chart"
             :HEIGHT 300, :WIDTH 350
             :X "a" :XTYPE "ordinal" :XTITLE "foo" :Y "b" :YTITLE "bar"
             :DATA data))
 (hmi/svgl! "Basics"))


;;; Geo Example
(->>
 {:width 500,
  :height 300,
  :data {:url "data/airports.csv"},
  :projection {:type "albersUsa"},
  :mark "circle",
  :encoding {:longitude {:field "longitude", :type "quantitative"},
             :latitude {:field "latitude", :type "quantitative"},
             :tooltip [{:field "name", :type "nominal"}
                       {:field "longitude", :type "quantitative"}
                       {:field "latitude", :type "quantitative"}],
             :size {:value 10}},
  :config {:view {:stroke "transparent"}}}
 (hmi/svgl! "Basics" :geo))



;;; Self Info - unexpectedness
(->>
 (let [data (->> (range 0.005 0.999 0.001)
                 (mapv (fn[p] {:x p, :y (- (log2 p)) :col "SI"})))]
   (hc/xform ht/simple-layer-chart
             :TITLE "Self Information (unexpectedness)"
             :LAYER [(hc/xform ht/line-layer
                               :XTITLE "Probability of event"
                               :YTITLE "-log(p)")
                     (hc/xform ht/xrule-layer :AGG "mean")]
             :DATA data))
 (hmi/svgl! "Basics"))

;;; Entropy - unpredictability
(->>
 (let [data (->> (range 0.00005 0.9999 0.001)
                         (mapv (fn[p] {:x p,
                                      :y (- (- (* p (log2 p)))
                                            (* (- 1 p) (log2 (- 1 p))))
                                      })))]
   (hc/xform ht/simple-layer-chart
             :TITLE "Entropy (Unpredictability)"
             :LAYER [(hc/xform ht/gen-layer
                               :MARK "line"
                               :XTITLE "Probability of event" :YTITLE "H(p)")
                     (hc/xform ht/xrule-layer :AGG "mean")]
             :DATA data))
 (hmi/svgl! "Basics"))



[hc/_defaults hc/default-opts]
;;; Multi Chart - cols and rows
(->>
 [(let [data (->> (range 0.005 0.999 0.001)
                 (mapv (fn[p] {:x p, :y (- (log2 p)) :col "SI"})))]
   (hc/xform ht/simple-layer-chart
             :TITLE "Self Information (unexpectedness)"
             :HEIGHT 300, :WIDTH 350
             :LAYER [(hc/xform ht/line-layer
                               :XTITLE "Probability of event"
                               :YTITLE "-log(p)")
                     (hc/xform ht/xrule-layer :AGG "mean")]
             :DATA data))
  (let [data (->> (range 0.00005 0.9999 0.001)
                         (mapv (fn[p] {:x p,
                                      :y (- (- (* p (log2 p)))
                                            (* (- 1 p) (log2 (- 1 p))))
                                      })))]
   (hc/xform ht/simple-layer-chart
             :TITLE "Entropy (Unpredictability)"
             :HEIGHT 300, :WIDTH 350
             :LAYER [(hc/xform ht/gen-layer
                               :MARK "line"
                               :XTITLE "Probability of event" :YTITLE "H(p)")
                     (hc/xform ht/xrule-layer :AGG "mean")]
             :DATA data))]
 (hmi/svgl! "Basics" :row))

(hmi/stabs! "Basics"
            [{:id :col, :label "Col"}
             {:id :row, :label "Row",
              :opts
              {:vgl {:export false}
               :layout {:order :row, :size "auto"}}}])

(hmi/sopts! "Basics" :row
            (merge hc/default-opts
                   {:vgl {:export {:png true :svg false}}
                    :layout {:order :row, :size "auto"}}))





(def obsdist
  (let [obs [[0 9] [1 78] [2 305] [3 752] [4 1150] [5 1166]
             [6 899] [7 460] [8 644] [9 533] [10 504]]
        totcnt (->> obs (mapv second) (apply +))
        pdist (map (fn[[k cnt]] [k (double (/ cnt totcnt))]) obs)]
    pdist))

;;(p/mean obsdist) => 5.7
(hc/xform ht/xrule-layer {:AGG 5.7})



(->>
 (hc/xform ht/simple-layer-chart
           :TITLE "A Real (obvserved) distribution"
           :LAYER
           [(hc/xform ht/bar-layer
                      :XTYPE "ordinal" :XTITLE "Count"
                      :YTITLE "Probability")
            (hc/xform ht/xrule-layer :X "m" :RTYPE "ordinal" :AGG "mean")]
           :DATA (mapv (fn[[x y]] {:x x :y y :m 5.7}) obsdist))
 (hmi/svgl! "Basics" :col))




(->
 (let [data (concat (->> obsdist
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Real"
                                  :tt (str y)})))
                    (->> (p/binomial-dist 10 0.57)
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Binomial"
                                  :tt (str y)}))))]
   (hc/xform ht/grouped-sq-cnt-chart
             {:WIDTH (-> 550 (/ 11) double Math/round (- 15))
              :TITLE "Real vs Binomial 0.57"
              :DATA data
              :X "dist" :XTYPE "nominal" :XTITLE ""
              :Y "y" :YTITLE "Probability"
              :COLUMN "cnt" :COLTYPE "ordinal"
              }))
 hmi/svgl!)



(->
 (let [data (->> "~/Bio/Rachel-Abhishek/lib-sq-counts.clj"
                 fs/fullpath slurp read-string (coll/takev 3)
                 (mapcat #(->> % (sort-by :cnt >) (coll/takev 200))) vec)]
   (hc/xform ht/grouped-sq-cnt-chart
             {:WIDTH (-> 550 (/ 9) double Math/round (- 15))
              :TITLE (format "Counts for %s"
                             (->> data first :nm (str/split #"-") first))
              :DATA data
              :X "nm" :XTYPE "nominal" :XTITLE ""
              :Y "cnt" :YTITLE "Count"
              :COLUMN "sq" :COLTYPE "nominal"
              }))
 hmi/svgl!)







(-> {:title  {:text "Real distribution vs Binomials"
              :offset 10}
     :height 80
     :width  450
     :background "floralwhite"
     :mark "bar"
     :encoding {:x {:field "cnt"
                    :type "ordinal"
                    ;;:scale {:rangeStep 1}
                    :axis {:title ""}}
                :y {:field "y"
                    :axis {:title "Probability"}
                    :type "quantitative"}
                :row {:field "dist" :type "nominal"}
                :color {:field "dist" :type "nominal"
                        :scale {:scheme {:name "greenblue" #_"category20c"
                                         :extent [0.4 1]}}
                        }
                :tooltip {:field "tt" :type "nominal"}
                }

     :data {:values
            (concat (->> obsdist
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Real"
                                  :tt (str y)})))
                    (mapcat #(let [l (hc/roundit %)]
                               (->> (p/binomial-dist 10 %)
                                    (mapv (fn[[x y]]
                                            {:cnt x :y y
                                             :dist (str l)
                                             :tt (str (hc/roundit y))}))))
                            (range 0.1 0.9 0.2)))}

     :config {:bar {:binSpacing 0
                    :discreteBandSize 1
                    :continuousBandSize 1}
              :view {:stroke "transparent"},
              :axis {:domainWidth 1}}}
    hmi/svgl!)


;;; :background "beige"
;;; :background "aliceblue"
;;; :background "floralwhite" ; <-- These are
;;; :background "ivory"       ; <-- the better
;;; :background "mintcream"
;;; :background "oldlace"

(->> {:title  {:text "KLD minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  550
      :background "floralwhite"
      :layer
      [#_{:mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       ;:axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       ;:axis {:title "KLD(P||Q)"}
                       }
                   }}
       {:mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "KLD(P||Q)"}}
                   :tooltip {:field "tt" :type "nominal"}
                   }}]

      :data {:values (mapv #(let [RE (it/KLD (->> obsdist (into {}))
                                            (->> (p/binomial-dist 10 %)
                                                 (into {})))
                                  REtt (hc/roundit RE)
                                  ptt (hc/roundit % :places 2)]
                              {:x % :y RE :tt (str ptt ", " REtt)})
                           (range 0.06 0.98 0.01))}
      }

     hmi/svgl!)

(->> {:title  {:text "JSD minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  550
      :background "floralwhite"

      :layer
      [#_{:mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)"}}
                   }}
       {:mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)"}}
                   :tooltip {:field "tt" :type "nominal"}
                   }}]

      :data {:values (mapv #(let [RE (it/jensen-shannon
                                      (->> obsdist (into {}))
                                      (->> (p/binomial-dist 10 %)
                                           (into {})))
                                  REtt (hc/roundit RE)
                                  ptt (hc/roundit % :places 2)]
                              {:x % :y RE :tt (str ptt ", " REtt)})
                           (range 0.06 0.98 0.01))}
      }

     hmi/svgl!)


(->> {:title  {:text "Minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  500
      :background "floralwhite"

      :layer
      [{:transform [{:filter {:field "RE" :equal "KLD"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "KLD(P||Q)" :grid false}
                       }
                   :color {:field "RE" :type "nominal"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "RE"}}
                   }}
       {:transform [{:filter {:field "RE" :equal "JSD"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)" :grid false}
                       }
                   :color {:field "RE" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "RE"}}
                   }}]
      :resolve {:y {:scale "independent"}}

      :data {:values (concat
                      (mapv #(let [RE (it/KLD (->> obsdist (into {}))
                                              (->> (p/binomial-dist 10 %)
                                                   (into {})))]
                               {:x % :y RE :RE "KLD"})
                            (range 0.06 0.98 0.01))
                      (mapv #(let [RE (it/jensen-shannon
                                       (->> obsdist (into {}))
                                       (->> (p/binomial-dist 10 %)
                                            (into {})))]
                               {:x % :y RE :RE "JSD"})
                            (range 0.06 0.98 0.01))) }
      }

     hmi/svgl!)


(count panclus.clustering/ntsq)
(def credata
  (let [mx 1.0
        %same (range mx 0.05 -0.05)
        sqs (hc/mutate panclus.clustering/ntsq %same)]
    (->> (hc/opt-wz (concat [["s1" panclus.clustering/ntsq]]
                            (map #(vector (str "s" (+ 2 %1)) %2)
                                 (range) (->> sqs shuffle (take 5))))
                    :alpha "AUGC" :limit 14)
         second (apply concat)
         (reduce (fn[M v] (assoc M (first v) v)) {})
         vals
         (map (fn[[x y _ sq]] {:x x :y y :m 9.0})))))



(-> {:title  {:text "CRE / Optimal Word Size"}
     :height 500
     :width 550
     :background "floralwhite"
     :layer
     [{:mark "line"
       :encoding {:x {:field "x"
                      :axis {:title "Word Size"}
                      :type "quantitative"}
                  :y {:field "y"
                      :axis {:title "CRE"}
                      :type "quantitative"}
                  }}
      {:mark "rule"
       :encoding {:x {:field "m"
                      :type "quantitative"}
                  :size {:value 1}
                  :color {:value "red"}}}]
     :data {:values (conj credata
                          {:x 1 :y 1.1 :m 9.0} {:x 2 :y 1.63 :m 9.0})}}
    hmi/svgl!)









;;; Lowess charting....
;;;
(def base-xy
  (->> "/home/jsa/Bio/no-lowess.clj" slurp read-string))
(def lowess-1
  (->> "/home/jsa/Bio/1-lowess.clj" slurp read-string))
(def lowess-2
  (->> "/home/jsa/Bio/2-lowess.clj" slurp read-string))
(def lowess-3
  (->> "/home/jsa/Bio/3-lowess.clj" slurp read-string))
(def lowess-4
  (->> "/home/jsa/Bio/4-lowess.clj" slurp read-string))


(->> {:title  {:text "Raw vs 1-4 lowess smoothing"
               :offset 5}
      :height 500
      :width  700
      :background "floralwhite"

      :data {:values (concat base-xy lowess-1 lowess-2 lowess-3 lowess-4) }

      :layer
      [{:transform [{:filter {:field "NM" :equal "NoL"}}]
        :mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L1"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L2"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L3"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L4"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}]
      ;;:resolve {:y {:scale "independent"}}

      }

     hmi/svgl!)