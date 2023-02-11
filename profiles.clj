{:dev      {:global-vars  {*warn-on-reflection* true}
            :dependencies [[org.clojure/clojure "1.11.1"]
                           [ch.qos.logback/logback-classic "1.4.5"
                            :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "2.0.6"]
                           [org.slf4j/jcl-over-slf4j "2.0.6"]
                           [org.slf4j/log4j-over-slf4j "2.0.6"]
                           [org.clojure/tools.logging "1.2.4"]]}
 :provided {:jar-exclusions [#"test.properties"]}}
