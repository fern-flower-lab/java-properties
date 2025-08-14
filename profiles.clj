{:dev      {:global-vars  {*warn-on-reflection* true}
            :dependencies [[org.clojure/clojure "1.12.1"]
                           [ch.qos.logback/logback-classic "1.5.18"
                            :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "2.0.17"]
                           [org.slf4j/jcl-over-slf4j "2.0.17"]
                           [org.slf4j/log4j-over-slf4j "2.0.17"]
                           [org.clojure/tools.logging "1.3.0"]]}
 :provided {:jar-exclusions [#"test.properties"]}}
