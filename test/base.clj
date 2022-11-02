(ns base
  (:require [clojure.test :refer :all]
            [java-properties.core :refer :all]
            [clojure.tools.logging :as log]))

(deftest basic
  (testing "embedded sample (simple test)"
    (let [config (load-config "test")]
      (log/info nil config)
      (is (-> config :backend :0 :enabled))
      (is (= (-> config :scheduler :threadPool.threadCount) 12)))
    (let [config (load-config "test" {:with-arrays true})]
      (log/info {:with-arrays true} config)
      (is (-> config :backend first :enabled))
      (is (= (-> config :scheduler :threadPool.threadCount) 12)))))
