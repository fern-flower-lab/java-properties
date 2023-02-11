(ns base
  (:require [clojure.test :refer :all]
            [java-properties.core :refer :all]
            [clojure.tools.logging :as log]))

(deftest basic
  (testing "embedded sample (simple test)"

    (log/infof "parsed without arrays: opts=%s" nil)

    (let [config (load-config "test")]

      (log/info "\n" (pretty config) "\n")

      (is (-> config :backend :0 :enabled))
      (is (= (-> config :scheduler :threadPool.threadCount) 12)))

    (log/infof "parsed without arrays: opts=%s" {:with-arrays true})

    (let [config (load-config "test" {:with-arrays true})]

      (log/info "\n" (pretty config) "\n")

      (is (-> config :backend first :enabled))
      (is (= (-> config :scheduler :threadPool.threadCount) 12))
      (is (and (-> config :service first (= "foo"))
               (-> config :service second :bar (= "bar"))
               (-> config :service last last)))
      (is (and (-> config :tomato first (= "vvv"))
               (-> config :tomato second (= "zzz"))))))

  (testing "overriding file values by manual definitions"
    (System/setProperty "core.journal.port" "11111")
    (System/setProperty "core.journal.host" "1.2.3.4")

    (let [config (load-config "test")]

      (log/info "\n" (pretty config) "\n")
      (is (= (-> config :core :journal :port) 11111))
      (is (= (-> config :core :journal :host) "1.2.3.4")))))
