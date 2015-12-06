/*******************************************************************************
 * µ² - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
 * developed at the Information Science Department, 
 * University of Otago, Dunedin, New Zealand.
 * 
 * This file is part of the aforementioned software.
 * 
 * µ² is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * µ² is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.nzdis.micro;

/**
 * This class provides a local execution environment for Java-generated Clojure code.
 * 
 * Created: 25/09/2009
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 */

import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;

import clojure.lang.Associative;
import clojure.lang.Binding;
import clojure.lang.Compiler;
import clojure.lang.Namespace;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class ClojureScriptExecutor {

	
	/**
	 * Runs a given Clojure script in default namespace 
	 * and without any bindings.
	 * @param script Script String to be run
	 * @return
	 */
	public static Object runClojureScript(String script){
		return runClojureScript("", null, script);
	}
	

	/**
	 * This method runs a Clojure script locally.
	 * @param namespace Namespace to run the script in
	 * @param bindings Bindings relevant for processing
	 * @param script Actual script as String
	 * @return Return value as Java Object
	 */
	public static Object runClojureScript(
			final String namespace, 
            final Map<String, ?> bindings,
            String script) {
		
		Namespace ns;
		if(namespace.isEmpty()){
			ns = (Namespace) RT.CURRENT_NS.get();
		} else {
			Symbol nssym = Symbol.intern(namespace);
	    	ns = Namespace.findOrCreate(nssym);
		}
		try {
            new Binding<String>(script);
            
            Associative mappings = PersistentHashMap.EMPTY;
            mappings = mappings.assoc(RT.CURRENT_NS, ns);
            if (bindings != null) {
                Iterator<String> iter = bindings.keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next().toString();
                    Symbol sym = Symbol.intern(key);
                    Var var = Var.intern(ns, sym);
                    //Var var = Var.internPrivate(ns.toString(), key);
                    Object value = bindings.get(key);
                    mappings = mappings.assoc(var, value);
                }
            }
            
            Var.pushThreadBindings(mappings);
            
            Object ret = Compiler.load(new StringReader(script));
            
            return ret;
	    } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	    } finally {
	            Var.popThreadBindings();
	    }
	}
	
}
