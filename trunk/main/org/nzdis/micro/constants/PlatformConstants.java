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
package org.nzdis.micro.constants;

/**
 * Describes constants used throughout the micro-agent platform,
 * including the configuration mechanisms.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2012/03/14 00:00:00 $
 * 
 */
public abstract class PlatformConstants {
	
	/** Configuration filename */
	public static final String PLATFORM_CONFIGFILE_PROPERTY = "opal.config";
	
	/** Default property file */
	public static final String DEFAULT_CONFIGFILE_PROPERTY = "platform.xml";
	
	/** Default micro-agent property file */
	public static final String DEFAULT_MICRO_CONFIG = "micro.properties";
	
	/** Configuration key for platform configuration */
	public static final String MICRO_CONFIGURATION = "Micro Platform";
	
	/** Platform name */
	public static final String PLATFORM_NAME = "PLATFORM_NAME";
	
	/** Operating system */
	public static final String OPERATING_SYSTEM = "OPERATING_SYSTEM";
	
	/** Activation of network support */
	public static final String DISTRIBUTED_MODE = "DISTRIBUTED_MODE";
	
	/** Number of core schedulers for (internal) message passing when not using JETLANG */
	public static final String NUMBER_OF_SCHEDULERS = "NUMBER_OF_SCHEDULERS";
	
	/** Number of core executors for (internal) message passing when not using JETLANG */ 
	public static final String NUMBER_OF_WORKERS = "NUMBER_OF_WORKERS";
	
	/** Selection of Netty-based serialization (see @SerializationType) */
	public static final String NETWORK_SERIALIZATION = "NETWORK_SERIALIZATION";
	
	/** Selection of internal message transport */
	public static final String INTERNAL_MESSAGE_TRANSPORT = "INTERNAL_MESSAGE_TRANSPORT";
	
	/** Selection of synchronous vs. asynchronous execution mode */
	public static final String SYNCHRONOUS_EXECUTION_MODE = "SYNCHRONOUS_EXECUTION_MODE";
	
	/** Clojure activation */
	public static final String CLOJURE_SUPPORT = "CLOJURE_SUPPORT";
	
	/** Console output level (see @OutputLevels) */
	public static final String PLATFORM_OUTPUT_LEVEL = "PLATFORM_OUTPUT_LEVEL";
	
	/** Indicator if platform output should be prefixed with time */
	public static final String PREFIX_PLATFORM_OUTPUT_WITH_TIME = "PREFIX_PLATFORM_OUTPUT_WITH_TIME";
	
	/** Agent console output level (see @AgentConsoleOutputLevels) */
	public static final String AGENT_CONSOLE_OUTPUT_LEVEL = "AGENT_CONSOLE_OUTPUT_LEVEL";
	
	/** 
	 * Constant indicating if each line in multi line output of agents should be prefixed
	 * with agent name.
	 */
	public static final String PREFIX_MULTILINE_AGENT_OUTPUT = "PREFIX_MULTILINE_AGENT_OUTPUT";
	
	/** 
	 * Constant indicating if agent console output should be prefixed (in the first line if multiline output)
	 * with message type (e.g. INFO, ERROR) for easier filtering with text-based filtering tools.
	 */
	public static final String PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE = "PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE";
	
	/** 
	 * Constant indicating to prefix agent output with nanotime information to trace exact timing from 
	 * output.
	 */
	public static final String PREFIX_AGENT_OUTPUT_WITH_NANOTIME = "PREFIX_AGENT_OUTPUT_WITH_NANOTIME";
	
	/**
	 * Constant for specification of random number generator seed in configuration. 
	 */
	public static final String RANDOM_NUMBER_GENERATOR_SEED = "RANDOM_NUMBER_GENERATOR_SEED";
	
	/** Port for network support */
	public static final String NETTY_MICRO_PORT = "NETTY_MICRO_PORT";
	
	/** Switch to allow automatic selection of free port during initialization */
	public static final String DYNAMIC_PORT_SELECTION = "DYNAMIC_PORT_SELECTION";
	
	/** MicroMessageValidator class */
	public static final String MICROMESSAGE_VALIDATOR = "MICROMESSAGE_VALIDATOR";
	
	/** start platform lazy */
	public static final String START_LAZY = "START_LAZY";
	
	/** set up discovery properties */
	public static final String DISCOVERY_MODE = "DISCOVERY_MODE";
	public static final String DISCOVERY_FREQUENCY = "DISCOVERY_FREQUENCY";
	public static final String DISCOVERY_BROADCAST_PORT = "DISCOVERY_BROADCAST_PORT";
	public static final String DISCOVERY_MULTICAST_ADDRESS = "DISCOVERY_MULTICAST_ADDRESS";
	public static final String DISCOVERY_MULTICAST_PORT = "DISCOVERY_MULTICAST_PORT";
	public static final String ACTIVATE_DISCOVERY = "ACTIVATE_DISCOVERY";
	public static final String START_DISCOVERY = "START_DISCOVERY";
	
	/** set up heartbeat settings */
	public static final String START_HEARTBEAT = "START_HEARTBEAT";
	public static final String HEARTBEAT_FREQUENCY = "HEARTBEAT_FREQUENCY";
	public static final String HEARTBEAT_TIMEOUT_FACTOR = "HEARTBEAT_TIMEOUT_FACTOR";
	
	/** method name for message handling (in SocialRole) - necessary for reflection in AbstractRole */
	public static final String SOCIAL_ROLE_MSG_METHOD_NAME = "handleMessage";
}
