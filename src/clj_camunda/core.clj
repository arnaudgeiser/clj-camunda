(ns clj-camunda.core
  (:import [org.camunda.bpm.engine ProcessEngineConfiguration]
           [clojure.contrib.jsr223 ClojureScriptEngineFactory]))

(defn add-script-engine
  "Adds a JSR-223 script engine to a Camunda configuration"
  [configuration script-engine]
  (let [scripting-engines (.getScriptingEngines configuration)]
    (.addScriptEngineFactory scripting-engines script-engine)
    configuration))

(defn create-configuration
  "Creates a H2 in memory configuration"
  []
  (-> (ProcessEngineConfiguration/createStandaloneInMemProcessEngineConfiguration)
      (.setJobExecutorActivate true)))

(def clojure-script-engine
  "Creates a JSR-223 script engine. `proxy` is used
  because Camunda is waiting for a first letter in lowercase"
  (proxy [ClojureScriptEngineFactory] []
    (getEngineName ([] "clojure"))))

(defn create-engine
  "Creates a Camunda engine with the Clojure ScriptEngine"
  []
  (let [configuration (create-configuration)
        engine (.buildProcessEngine configuration)]
    (add-script-engine configuration clojure-script-engine)
    engine))

(defn deploy-process
  "Deploys a BPMN process from the classpath"
  [engine bpmn]
  (-> engine
      (.getRepositoryService)
      (.createDeployment)
      (.addClasspathResource bpmn)
      (.deploy)))

(defn execute-process
  "Executes a new process according to the `process-key` with the
  given `variables`"
  [engine process-key variables]
  (->
   (.getRuntimeService engine)
   (.createProcessInstanceByKey process-key)
   (#(reduce (fn [e [k v]] (.setVariable e (name k) v)) % variables))
   (.execute)))

(defn execution-entity->map
  "Clojurizes the `org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity`"
  [e]
  {:variables (into {} (map (fn [[k v]] [(keyword k) v]) (.getVariables e)))})

(defmacro defprocess
  "Defines a Camunda process"
  [& clauses]
  (cons 'do
        (for [clause clauses
              :let [[n p f] clause]]
          `(defn ~n ~'[e]
             ~(if (seq p)
                `(let [~@p (execution-entity->map ~'e)] ~f)
                f)))))

(comment
  (def engine (create-engine))
  (def deployment (deploy-process engine "simple-diagram.bpmn"))

  ;; Camunda maps directly to Clojure var and
  ;; the `org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity` can
  ;; be used through Java Interop
  (defn send-mail [e]
    (let [email (get (.getVariables e) "email")]
      (prn "I'm sending an email to : " email)
      (re-find #"gmail" email)))

  (defn log-other-email-account [_]
    (prn "Log other email account"))

  (defn log-gmail-account [_]
    (prn "Log gmail account"))

  ;; Or we can use the defprocess macro
  (defprocess
    (send-mail [{{:keys [email]} :variables}]
               (prn (str "I'm sending an email to " email))
               (re-find #"gmail" email))
    (log-other-email-account [] (prn "Log other email account"))
    (log-gmail-account [] (prn "Log gmail account")))


  (execute-process engine "myprocess" {:email "arnaudgeiser@gmail.com"})
  (execute-process engine "myprocess" {:email "arnaudgeiser@exoscale.ch"}))
