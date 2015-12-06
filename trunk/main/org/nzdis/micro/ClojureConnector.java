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


import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import clojure.lang.Associative;
import clojure.lang.Compiler;
import clojure.lang.Namespace;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import clojure.lang.Var;
import clojure.lang.Symbol;

/**
 * ClojureConnector provides all means to access Clojure code from Java respectively
 * manage agent registration in Clojure.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 2.0 $ $Date: 2010/11/14 00:00:00 $
 *
 */
public class ClojureConnector {

	/** version of script */ 
	private static String version = "0.2";
	/** Owning Java agent */
	private AbstractAgent owningAgent = null;
	
	/** platform-related script variables */
	/** platform script (looked up in the commonScriptPath (if set)) */
	private static String platformScript = "platform.clj";
	private static boolean platformScriptLoaded = false;
	/** common base script for all agents (looked up in the commonScriptPath (if set)) */
	private String baseScript = "commonAgentBase.clj";
	/** definition of individual setup script (lookup in indivScriptPath (if set)) */
	private String initScript = "";
	/** base namespace for Clojure (all agents) */
	private static String cljNamespace = "user";
	
	/** application-related script variables */
	/** default application/environment script - is application-related and run upon start of first agent */
	private static String appScript = "";
	private static boolean appScriptLoaded = false;
	/** post initialization script - is run at user-defined time (runPostInitScript()), typically after all initializations */
	private static String postInitScript = "";
	private static boolean postInitScriptLoaded = false;
	
	
	/** script paths */
	/** path for all common scripts (like platform.clj) */
	private static String commonScriptPath = "cljScripts/commonCljScripts/";
	/** path for all individual scripts (used by agents) */
	private static String indivScriptPath = "cljScripts/indivCljScripts/";
	/** path for application scripts */
	private static String appScriptPath = "cljScripts/appCljScripts/";
	/** path for role-related scripts */
	private static String roleScriptPath = "cljScripts/roleCljScripts/";

	private Symbol user;
	private Namespace userNS;
	
	/** individual agent namespace (defined on initialization using base namespace and agent name) */
	private String agentNamespace;
	
	/** Clojure Vars to intern upon Clojure initialization */
	private Symbol CLOJURE_MAIN = Symbol.intern("clojure.main");
	private Var REQUIRE = RT.var("clojure.core", "require");
	private static Var MAIN = RT.var("clojure.main", "main");

	/** interned agent var for (agent) java object **/
	private Var agt;
	/** Stringbuffer for command execution (used in initialization) */
	private StringBuffer execCmd = new StringBuffer();
	/** Port for Clojure network REPL */
	private static String tcpPort = "10000";
	/** indicates if console REPL is running (needs to be started last!) */
	private static boolean consoleReplRunning = false;
	/** indicates if network REPL is running */
	private static boolean networkReplRunning = false;

	private static String platformInfoPrefix = "Platform info: ";
	
	/** some strings printing a nice platform greeting to indicate that Clojure has been started. */
	private static String bar = "\n|==========================================|";
	private static String platformHeader = new StringBuilder(bar)
				.append("\n| Clojure OPAL Micro-Agent Platform Loader |") 
				.append("\n| Version: ").append(version)
				.append("                             |") 
				.append(bar).toString();
	private static String succBar = "\n|====== Clojure Platform initialized ======|";

	/** empty HashMap to save Thread bindings for initializations */
	private Map<String, Object> globals = new HashMap<String, Object>();
	/** Saved ThreadMappings */
	private Associative mappings = PersistentHashMap.EMPTY;
	
	/**
	 * ClojureConnector is used to manage the Clojure agent associated to an according Java micro-agent
	 * instance.
	 * @param microAgent - micro-agent associated with Clojure environment
	 * @param clojureBase - filename for clj base script for individual Clojure environment 
	 * (to be saved in user directory (respectively Eclipse project root folder))
	 */
	ClojureConnector(AbstractAgent microAgent, String clojureBase){
		
		if(clojureBase.isEmpty()){
			this.initScript = "";
		} else {
			this.initScript = clojureBase;
		}
		
		/*
		 * create symbols and namespace for owner agent
		 */
		this.owningAgent = microAgent;
		this.user = Symbol.create(cljNamespace);
		this.userNS = Namespace.findOrCreate(user);
		this.agentNamespace = new StringBuilder(cljNamespace).append(".").append(this.owningAgent.getAgentName()).append("ns").toString();

		/**
		 * setup basic platform script (upon instantiation of first agent)
		 */
		if(platformScriptLoaded == false){
			loadPlatform();
		}
		
		/**
		 * application scripts are run upon instantiation of first Clojure-enabled agent
		 */
		if(appScriptLoaded == false && !appScript.equals("")){
			loadApplication();
		}
		
		try {
			registerAgent();
		} catch (Exception e) {
			System.err.println(platformInfoPrefix+"Clojure Agent initialization failed!");
			e.printStackTrace();
		}
	}

	/**
	 * Starts the Clojure base platform upon call to avoid lazy initialization during runtime
	 */
	public static void startPlatform(){
		loadPlatform();
		loadApplication();
	}
	
	/**
	 * Loads the actual platform
	 */
	private static void loadPlatform(){
		
		if(!platformScriptLoaded){
			System.out.println(platformHeader);
			StringBuffer buffer = new StringBuffer("Clojure parameters: ")
				.append("\nPlatform script: ")
				.append(commonScriptPath).append("/").append(platformScript)
				.append("\nCommon script path: ").append(commonScriptPath)
				.append("\nIndividual script path: ").append(indivScriptPath)
				.append("\nApplication script path: ").append(appScriptPath)
				.append("\nRole script path: ").append(roleScriptPath);
			System.out.println(buffer.append(succBar).append("\n").toString());
			StringBuilder platformLoad = new StringBuilder(100);
			platformLoad.append("(ns ");
			platformLoad.append(cljNamespace);
			platformLoad.append(")(load-file \"");
			if(!commonScriptPath.equals("")){
				platformLoad.append(commonScriptPath);
				platformLoad.append("/");
			}
			platformLoad.append(platformScript);
			platformLoad.append("\")");
			//String internalCmd = "(ns "+ CljNamespace +")(load-file \""+ platformScript +"\")";
			execInClojure(platformLoad.toString());
			platformScriptLoaded = true;
		} else {
			System.out.println(platformInfoPrefix + "Platform already loaded.");
		}
	}
	
	/**
	 * Loads the application-dependent scripts
	 */
	private static void loadApplication(){
		
		if(!platformScriptLoaded){
			System.out.print(platformInfoPrefix + "Ensure to load platform prior to application script!");
		} else {
			if(!appScriptLoaded && !appScript.equals("")){
				StringBuilder sb = new StringBuilder();
				sb.append("(load-file \"");
				if(!appScriptPath.equals("")){
					sb.append(appScriptPath).append("/");
				}
				sb.append(appScript).append("\")");
				execInClojure(sb.toString());
				appScriptLoaded = true;
			} else {
				if(!appScript.equals("")){
					System.out.println(platformInfoPrefix + "Application script already loaded.");
				}
			}
		}
	}
	
	/**
	 * returns the owner agent's Clojure namespace.
	 * @return
	 */
	public String getAgentNamespace() {
		return agentNamespace;
	}

	/**
	 * This function allows execution of String CLJ statements in Clojure.
	 * Its functionality is similar to ExecInClojure(String) but does not prefix
	 * the agent namespace. Caution: Inefficient, and should not be used for
	 * application code in agent.
	 * @param internalCmd S-expression string
	 * @param internal Boolean indicating that code is called internally (else no execution)
	 * @return
	 */
	private Object execInClojure(String internalCmd, boolean internal){
		
		if(internal == true){
			Object ret;
			//this.mappings.assoc(RT.CLOJURE_NS, Namespace.findOrCreate(Symbol.intern(this.agentNamespace)));
			try {
				Var.pushThreadBindings(mappings);
				ret = Compiler.load(new StringReader(internalCmd));
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
				ret = null;
			} finally {
				this.mappings = Var.getThreadBindings();
		        Var.popThreadBindings();
		    }
			return ret;
		}
		return null;
	}
	
	/**
	 * TODO
	 * Deletes the contents of the namespace for the according CljConnector instance
	 */
	protected void shutdownConnectorForNamespace(){
		
	}
	
	/**
	 * Executes a given command in supplied namespace. For use from main method of
	 * application.
	 * @param namespace Namespace as String (e.g. "user"). Defaults to "user" if not supplied.
	 * @param clojureCmd Command to be executed
	 * @return Object as return
	 */
	public static Object execInClojureNamespace(String namespace, String clojureCmd){
		if(namespace == null || namespace.equals("")){
			namespace = "user";
		}
		return execInClojure(new StringBuffer("(ns ").append(namespace).append(")").append(clojureCmd).toString());
	}
	
	/**
	 * Static version of code executor without mappings
	 * @param internalCmd
	 * @return
	 */
	private static Object execInClojure(String internalCmd){
		Object ret;
		//this.mappings.assoc(RT.CLOJURE_NS, Namespace.findOrCreate(Symbol.intern(this.agentNamespace)));
		try {
			//Var.pushThreadBindings(mappings);
			ret = Compiler.load(new StringReader(internalCmd));
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			ret = null;
		} finally {
			//this.mappings = Var.getThreadBindings();
	        //Var.popThreadBindings();
	    }
		return ret;
	}
	
	/**
	 * Executes a given s-expression in Clojure. This method should be used
	 * as limited as possible as it initializes the Clojure compiler (which is
	 * very inefficient). All statements are executed in current agent's namespace.
	 * @param clojureCmd S-expression String
	 * @return
	 */
	public Object execInClojure(StringBuffer clojureCmd){
		return execInClojure(new StringBuffer("(ns ").append(this.getAgentNamespace()).append(")").append(clojureCmd).toString(), true);
	}
	
	/**
	 * This function returns a var for a function defined in the Clojure namespace of
	 * the agent and returns a reference allowing invocation from Java.
	 * @param cljFunctionName
	 * @return Clojure Var
	 */
	public Var registerFunction(String cljFunctionName){
		return RT.var(this.agentNamespace, cljFunctionName);
	}
	
	/**
	 * Runs a specified role script (must reside in roleScriptPath) 
	 * in agent's namespace.
	 * @param roleScript Role script to be run
	 */
	public void runRoleScript(String roleScript){
		StringBuffer buffer = new StringBuffer();
		buffer.append("(load-file \"");
		if(!roleScriptPath.equals("")){
			buffer.append(roleScriptPath).append("/");
		} else {
			if(!appScriptPath.equals("")){
				buffer.append(appScriptPath).append("/");
			}
		}
		buffer.append(roleScript).append("\")");
		execInClojure(buffer);
	}
	
	/**
	 * This function registers the agent owning this ClojureConnector instance with
	 * Clojure, creates a new namespace for it (Scheme: "user.<agentName>ns") and 
	 * binds the variable agt in this namespace with the Java agent instance allowing
	 * access to all public methods and fields.
	 */
	private void registerAgent(){
		
		try {
			REQUIRE.invoke(CLOJURE_MAIN);
		} catch (Exception e) {
			e.printStackTrace();
		}

		globals.put(this.owningAgent.getAgentName(), this.owningAgent);
		
		execCmd.append("(ns ").append(this.agentNamespace).append(")");
		execCmd.append("(def agt ").append(cljNamespace).append("/").append(this.owningAgent.getAgentName()).append(")");
		
		if(!commonScriptPath.equals("")){
			execCmd.append("(load-file \"").append(commonScriptPath).append("/").append(this.baseScript).append("\")");
		} else {
			execCmd.append("(load-file \"").append(this.baseScript).append("\")");
		}
		
		if(!this.initScript.equals("")){
			if(!indivScriptPath.equals("")){
				execCmd.append("(load-file \"").append(indivScriptPath).append("/").append(this.initScript).append("\")");
			} else {
				execCmd.append("(load-file \"").append(this.initScript).append("\")");
			}
		}

		for (Map.Entry<String, Object> global : globals.entrySet()) {
		    String key = global.getKey();
		    Object value = global.getValue();
		    //System.out.println("Key: " + key + ", Value: " + value.toString());
		    agt = Var.intern(userNS, Symbol.create(key), value);
		    //System.out.println("agt: " + agt.toString());
		    mappings.assoc(agt, value);
		    //System.out.println(mappings.containsKey(agt) + " - " + mappings.entryAt(agt));
		}
		//System.err.println("Execution script: "+ execCmd);
		execInClojure(execCmd.toString(), true);
	}
	
	/**
	 * Starts a console REPL. Should be started after all initialization procedures,
	 * especially network REPL (network REPL does not start after console REPL has 
	 * been started).
	 */
	public static void startConsoleREPL(){
		consoleReplRunning = true;
		if(networkReplRunning == false){
			System.out.println(platformInfoPrefix+"If network REPL should be used, please ensure to initialize before console REPL!");
		}
		try {
			MAIN.applyTo(RT.seq(""));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts the network REPL on the default TCP port (10000).
	 */
	public static void startNetworkREPL(){
		networkReplRunning = true;
		StringBuilder sb = new StringBuilder();
		sb.append("(require 'clojure.contrib.server-socket)(clojure.contrib.server-socket/create-repl-server ");
		sb.append(tcpPort).append(")");
		//String networkCmd = "(require 'clojure.contrib.server-socket)(clojure.contrib.server-socket/create-repl-server "+ tcpPort +")";
		try {
			Compiler.load(new StringReader(sb.toString()));
		} catch (Exception e) {
			System.out.println(platformInfoPrefix+"Remote Clojure REPL already running on Port "+ tcpPort + ". No further port opened.");
		}	
	}
	
	/**
	 * Starts the network REPL with a user-defined TCP port.
	 * @param tcpPort String definition of TCP port (e.g. 10001)
	 */
	public static void startNetworkREPL(String tcpPort){
		setTcpPort(tcpPort);
		startNetworkREPL();
	}
	
	/**
	 * Returns the TCP port used for the Clojure network REPL.
	 * @return
	 */
	public static String getTcpPort() {
		return tcpPort;
	}

	/**
	 * Sets the TCP port for Clojure network REPL.
	 * Change will only have effect before starting network REPL.
	 * @param tcpPort
	 */
	public static void setTcpPort(String tcpPort) {
		ClojureConnector.tcpPort = tcpPort;
	}
	
	/**
	 * Sets the name for the script executed by all agents upon initialization.
	 * If set this file is used, else the default "commonAgentScript.clj". 
	 * Ensure to set it before initializing agents.
	 * It should reside in the commonScriptPath.
	 * @param scriptName Script to be executed as default script for all agents
	 */
	public static void setCommonAgentScript(String scriptName){
		if(platformScriptLoaded){
			System.err.println(platformInfoPrefix + "Cannot define common agent " +
					"CLJ script after platform initialization!");
		} else {
			ClojureConnector.platformScript = scriptName;
		}
	}
	
	
	/**
	 * Sets the platform script name. It is expected to be found in
	 * the commonScriptPath (see setCommonScriptPath()).
	 * @param scriptName Platform script
	 */
	public static void setPlatformScript(String scriptName){
		ClojureConnector.platformScript = scriptName;
	}
	
	/**
	 * Indicates if the platform CLJ script has been executed using.
	 * @return boolean
	 */
	public static boolean platformScriptStarted(){
		if(ClojureConnector.platformScriptLoaded == true){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Sets the application script name. It is expected to be in the user directory subfolder
	 * appScriptPath ( --> System.getProperty("user.dir")/appScriptPath).
	 * @param scriptName Application-dependent script
	 */
	public static void setApplicationScript(String scriptName){
		ClojureConnector.appScript = scriptName;
	}
	
	/**
	 * Indicates if the application-specific CLJ script has been executed using.
	 * @return boolean
	 */
	public static boolean applicationScriptStarted(){
		if(ClojureConnector.appScriptLoaded == true){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Sets the post initialization script name (e.g. "postInit.clj").
	 * It is expected to be in the found in the appScriptPath.
	 * @param scriptName String name of the script file name
	 */
	public static void setPostInitScript(String scriptName){
		ClojureConnector.postInitScript = scriptName;
	}
	
	/**
	 * Indicates if the post initialization script has been executed using
	 * runPostInitScript().
	 * @return boolean
	 */
	public static boolean postInitScriptStarted(){
		if(ClojureConnector.postInitScriptLoaded == true){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Runs the post initialization script as a time defined by the implementer.
	 * The scripts needs to be defined before using setPostInitScript() and
	 * is expected to be in the appScriptPath (application script path).
	 */
	public static void runPostInitScript(){
		if(!ClojureConnector.postInitScript.equals("")){
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("(load-file \"");
				if(!postInitScript.equals("")){
					sb.append(appScriptPath).append("/");
				}
				sb.append(postInitScript).append("\")");
				Compiler.load(new StringReader(sb.toString()));
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
			}
			postInitScriptLoaded = true;
		} else {
			System.out.println(platformInfoPrefix+"Run of post-init script aborted as post-init script not defined!");
		}
	}
	
	/**
	 * Runs a user-defined script when called from application code. Not recommended
	 * for general use but for special needs. Preferably use runPostInitScript() as
	 * no special checks are done and the script is directly executed in the JVM.
	 * Path is relative to java userdir (e.g. "customfolder/customscript.clj"). 
	 * Returned values are not passed through. Use ExecInClojure for that purpose.
	 */
	public static void runCLJScript(String script){
		
		if(platformScriptLoaded){
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("(load-file \"").append(script).append("\")");
				Compiler.load(new StringReader(sb.toString()));
			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
			}
		} else {
			System.out.println(platformInfoPrefix + "Load platform before executing scripts.");
		}
	}
	
	
	/**
	 * defines the relative path (from Java userpath) for CLJ scripts which are
	 * of platform-wide use (e.g. common script for each agent (commonAgentScript.clj), platform script (platform.clj)).
	 * Defaults to userpath.
	 * @param path
	 */
	public static void setCommonScriptPath(String path){
		commonScriptPath = path;
	}
	
	/**
	 * Sets the relative path (from userpath) where all individual agent scripts are stored.
	 * Please consider to include the final slash (e.g. "scriptPath/").
	 * Defaults to userpath.
	 * @param path
	 */
	public static void setIndividualScriptPath(String path){
		indivScriptPath = path;
	}
	
	/**
	 * sets the relative path (from userpath) where all application-related scripts 
	 * (appScript, postInitScript) are stored.
	 * Please consider to include the final slash (e.g. "scriptPath/").
	 * Defaults to userpath.
	 * @param path
	 */
	public static void setApplicationScriptPath(String path){
		appScriptPath = path;
	}
	
	/**
	 * sets the role script path (relative from userpath) where role-related Clojure
	 * scripts are to be found. If not set, appScriptPath will be assumed.
	 * Please consider to include the final slash (e.g. "scriptPath/").
	 * @param path
	 */
	public static void setRoleScriptPath(String path){
		roleScriptPath = path;
	}
	
}
