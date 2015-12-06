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
(def odd false) 

(defn receive-msg [message]
  "receive message from Java"
  (do
    (if (= (.getClass (.getIntent message)) org.nzdis.micro.test.CljExecutionIntent)
      (with-local-vars [newIntent (new org.nzdis.micro.test.CljExecutionIntent)] 
		    
		    (.setResult (var-get newIntent) (load-string (.getSExpression (.getIntent message))))  
		    (if (true? odd)
           (def odd false) (def odd true))  
        
        (if (true? odd)
          (send-msg (doto (new org.nzdis.micro.MicroMessage) (.setIntent (var-get newIntent)) (.setRecipient (.getSender message))))
          (send-msg (struct intentMessage (.getSender message) (var-get newIntent))))
		    ))))