; 
;µ² - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
;developed at the Information Science Department, 
;University of Otago, Dunedin, New Zealand.
;
;This file is part of the aforementioned software.
;
;µ² is free software: you can redistribute it and/or modify
;it under the terms of the GNU General Public License as published by
;the Free Software Foundation, either version 3 of the License, or
;(at your option) any later version.
;
;µ² is distributed in the hope that it will be useful,
;but WITHOUT ANY WARRANTY; without even the implied warranty of
;MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;GNU General Public License for more details.
;
;You should have received a copy of the GNU General Public License
;along with the OPAL Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
;
;Author: Christopher Frantz (cfrantz@infoscience.otago.ac.nz)
;Version: 1.0, Date: 14th November 2010
;
(defn print-me []
  (println (str "Agent name is '" (.getAgentName agt) "'.")))

(defn greeting []
  (println (str "Agent " (.getAgentName agt) " is registered (including Clojure) and running.")))

(defstruct intentMessage :recipient :intent) 

(defstruct message :recipient :execEnv :performative :content :intent) 

(defn send-msg 
  ([mess]
  "Sends a message with a passed message struct for message construction (see struct message) or a MicroMessage." 
  (do
  (if (= (.getClass mess) org.nzdis.micro.MicroMessage)
    ;(do
      ;(.print agt (str "Intend to send message " (.toString mess)))
  
      (.send agt mess)
    ;) 
    (let [msg (new org.nzdis.micro.MicroMessage)]
      (if (not (nil? (get mess :recipient)))
        (.setRecipient msg (:recipient mess)))
      (if (not (nil? (get mess :execEnv)))
        (.setExecutionEnvironment msg (:execEnv mess)))  
      (if (not (nil? (get mess :performative)))
        (.setPerformative msg (:performative mess))) 
      (if (not (nil? (get mess :content)))
        (.setContent msg (:content mess))) 
      (if (not (nil? (get mess :intent)))
        (.setIntent msg (:intent mess)))
      (.send agt msg)))))
  ([target payload]
  "Sends a message to a target with passed content string. Is supposed to be executed in CLJ in target."     
     (.sendClj agt target payload)))

(defn send-broadcast [mess] 
  "Broadcasts a given message to all other agents."
  (let [msg (new org.nzdis.micro.MicroMessage)]
    (.setRecipient msg (:recipient mess))
    (.setExecutionEnvironment msg (:execEnv mess))
    (.setPerformative msg (:performative mess)) 
    (.setContent msg (:content mess))    
    (.sendBroadcast agt msg))) 

(defn eval-cmd [#^String cmd]
  "Accepts a string and evaluates it as Clojure statement"
  (load-string cmd) 
)

(defn random [#^Integer dim]
  "returns a random value between 0 and dim-1. Input must be positive." 
  (-> (org.nzdis.micro.MTConnector/getRandomNoGenerator) (.nextInt dim))
)

(defn print-log []
  "prints the individual log file of this agent (if available)"
  (println (.getLog (.log agt))))

 
