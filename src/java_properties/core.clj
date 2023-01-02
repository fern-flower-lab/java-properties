(ns java-properties.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.edn :refer [read-string]]
            [clojure.pprint :refer [pprint]])
  (:import (java.util Properties Date)
           (java.io Reader StringWriter)
           (java.time.format DateTimeFormatter DateTimeParseException)
           (java.time Instant)))

(defn env-props []
  (reduce (fn [x [y z]] (assoc x y z)) {} (System/getProperties)))

(defn load-props [file]
  (with-open [^Reader reader (io/reader file)]
    (let [props (Properties.)
          overrides (env-props)]
      (.load props reader)
      (into {} (for [[k v] props]
                 [k (read-string (get overrides k v))])))))

(defn split-comma-separated [s]
  (->> (s/split s #",")
       (map s/trim)))

(defn map-vals [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn map-keys [f m]
  (into {} (for [[k v] m] [(f k) v])))

(defn remove-vals [f m]
  (->> m
       (remove #(-> % second f))
       (into {})))

(defn pretty [& args]
  (s/trimr
    (let [out (StringWriter.)]
      (doseq [arg args]
        (pprint arg out))
      (.toString out))))

(defn- kebab-to-camelcase [k]
  (let [parts (-> k
                  name
                  (s/split #"\-"))]
    (-> (first parts)
        (cons (->> parts
                   next
                   (map s/capitalize)))
        s/join
        keyword)))

(defn kebab-conf-to-camelcase [conf]
  (map-keys kebab-to-camelcase conf))

(def ^:private datetime-formatters (mapv #(DateTimeFormatter/ofPattern %)
                                         ["yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
                                          "yyyy-MM-dd HH:mm:ss[.SSS]X"
                                          "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"
                                          "yyyy-MM-dd HH:mm:ss.SSSSSSX"]))

(defn parse-java-util-date [^String s]
  (loop [formatters datetime-formatters]
    (or
      (try
        (->> s (.parse (first formatters)) Instant/from Date/from)
        (catch DateTimeParseException e
          (when-not (second formatters)
            (throw e))))
      (recur (next formatters)))))

(defn merge-common [d keyword]
  (let [c (get d keyword)]
    (->> (dissoc d keyword)
         (map (fn [[k d]] [k (merge c d)]))
         (into {}))))

(defn ordered-configs [d]
  (->> d
       keys
       sort
       (map #(-> (get d %)
                 (assoc :key %)))))
(defn hex [ba]
  (->> (map #(format "%02x" %) ba)
       (apply str)))


(defn unhex [s]
  (->> (partition 2 s)
       (map #(Integer/parseInt (apply str %) 16))
       byte-array))

(def ^:private pairs (atom {}))

(defn extract-strings [line]
  (let [line' (-> line
                  (s/replace #"\[(\d+)\]\." ".$1.")
                  (s/replace #"\[(\d+)\]\s*=" ".$1=")
                  (s/replace #"\[(\d+)\]" ".$1.")
                  (s/replace #"\.+" ".")
                  (s/replace #"\.+$" ""))
        strings (re-seq #"\"[^\"]+\"" line')]
    (if (empty? strings)
      line'
      (loop [li line' st strings]
        (let [term (first st)
              hashed (some-> term .getBytes hex)]
          (swap! pairs assoc hashed (some-> term (s/replace "\"" "") keyword))
          (if (empty? st) li
                          (recur (s/replace li term hashed) (rest st))))))))

(declare group-config)

(defn- deeper [d]
  (if (contains? d nil)
    (get d nil)
    (group-config d)))

(defn- strip-prefix [pairs]
  (->> pairs
       (map-keys #(second (s/split % #"\." 2)))
       deeper))

(defn- compile-dict [d]
  (->> d
       (group-by #(keyword (first (s/split (first %) #"\."))))
       (map-vals strip-prefix)))

(defmacro ^:private idxv? [x]
  `(and (int? ~x)
        (or (pos? ~x) (zero? ~x))))

(defn- idx? [v]
  (try
    (-> v name read-string idxv?)
    (catch NumberFormatException _ false)))

(declare convert-arrays)

(defn- sparsed-array [m]
  (let [idxs (map #(-> % name read-string num) (keys m))
        size (apply max idxs)]
    (vec (for [x (range 0 (inc size))
               :let [val (get m (-> x str keyword))]]
           (convert-arrays val)))))

(defn- convert-arrays [val]
  (if (map? val)
    (if (every? idx? (keys val))
      (sparsed-array val)
      (into {}
            (for [[k v] val]
              [k (convert-arrays v)])))

    val))

(defn- swap-pair* [k]
  (get @pairs (name k) k))

(defn- restore-keys [d]
  (map-keys swap-pair* d))

(defn- group-config [d & [{with-arrays :with-arrays}]]
  (cond->> (map-keys extract-strings d)
           true compile-dict
           with-arrays convert-arrays
           (not-empty @pairs) restore-keys))

(defn load-config [app-name & [{:keys [config] :as options}]]
  (let [conf-dict (group-config
                    (merge
                      (some-> app-name (str ".properties") io/resource load-props)
                      (when config
                        (some-> config io/file load-props)))
                    options)]
    (reset! pairs {})
    conf-dict))
