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
package org.nzdis.micro.bootloader;

import org.nzdis.micro.constants.OperatingSystems;
import org.nzdis.micro.constants.PlatformConstants;
import org.nzdis.micro.constants.PlatformOutputLevels;
import org.nzdis.micro.constants.SerializationTypes;
import org.nzdis.micro.messaging.MTRuntime;

/**
 * MicroBootProperties allows the override of configuration-file 
 * defined settings in-code if called before platform initialization.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2013/02/28 00:00:00 $
 * 
 */
public class MicroBootProperties extends PlatformConstants {

	public static MicroPropertiesMap bootProperties = new MicroPropertiesMap();
	/** Switch indicating if platform has been initialized */
	private static boolean platformInitialized = false;
	/** Platform configuration filename */
	protected static final String platformFileName = "platform.xml";
	/** Name of XML section in configuration file */
	private static final String configSection = "Micro Platform";
	/** Message prefix used for console output from this class */
	private static final String configPrefix = "Micro Platform Configurator: ";
	
	static{
		try {
			readConfiguration();
		} catch (Exception e) {
			System.err.print("Problems when reading the configuration file '" + platformFileName + "'!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper method to enforce execution of static block (i.e. reading of configuration file) 
	 * without invoking any 'meaningful' method.
	 */
	public static void load(){
		
	}
	
	/**
	 * Checks if platform has been loaded (and configuration initialized).
	 * Generates console output to indicate the effectlessness of configuration in 
	 * this case.
	 */
	private static void platformLoadedCheck(){
		if(platformInitialized){
			System.err.println(configPrefix + "Platform already started, further call to configuration does not have effect!");
		}
	}
	
	/**
	 * Marks the platform as started. Further changes to the configuration 
	 * will not be taken into account for platform.
	 */
	public static void platformInitialized(){
		platformInitialized = true;
	}
	
	/**
	 * Indicates if platform is initialized
	 * @return boolean Value indicating if platform is already initialized
	 */
	public static boolean isPlatformInitialized(){
		return platformInitialized;
	}
	
	/**
	 * sets various capabilities for MICRO_FIBER message passing framework, 
	 * namely number of schedulers for message dispatch and number of 
	 * worker threads for message processing.
	 * @param numberOfCoreSchedulers Number of schedulers to be used
	 * @param numberOfCoreExecutors Number of executors to be used for message delivery
	 */
	public static void setExecutorProperties(Integer numberOfCoreSchedulers, Integer numberOfCoreExecutors){
		platformLoadedCheck();
		bootProperties.put(NUMBER_OF_SCHEDULERS, numberOfCoreSchedulers.toString());
		bootProperties.put(NUMBER_OF_WORKERS, numberOfCoreExecutors.toString());
	}
	
	/**
	 * Activates network support for platform. All network-related functionality
	 * depends on this setting (network transport, network discovery, heartbeat).
	 * @param distributedMode Boolean indicator if platform should be started in distributed mode
	 */
	public static void activateNetworkSupport(boolean distributedMode){
		platformLoadedCheck();
		bootProperties.put(DISTRIBUTED_MODE, String.valueOf(distributedMode));
	}
	
	/**
	 * Sets the TCP port for network communication.
	 * @param port Port to be bound for TCP communication
	 */
	public static void setNetworkTcpPort(Integer port){
		platformLoadedCheck();
		bootProperties.put(NETTY_MICRO_PORT, port.toString());
	}
	
	/**
	 * Indicate if system should dynamically use other ports if the
	 * specified one is occupied */
	public static void setDynamicTcpPortSelection(boolean selectDynamic){
		platformLoadedCheck();
		bootProperties.put(DYNAMIC_PORT_SELECTION, String.valueOf(selectDynamic));
	}
	
	/**
	 * Sets message validator used for application. Please ensure to activate MicroMessage.globalValidation 
	 * or any other validation-related switch in order to use this functionality.
	 * @param messageValidator Fully qualified class name of message validator (implementing the MicroMessageValidator interface)
	 */
	public static void setMicroMessageValidator(String messageValidator){
		platformLoadedCheck();
		bootProperties.put(MICROMESSAGE_VALIDATOR, messageValidator);
	}
	
	/**
	 * Configures heartbeat with according parameter set.
	 * @param activateHeartbeat Boolean indicator for activation
	 * @param frequencyInSeconds Frequency between heartbeat messages
	 * @param heartbeatTimeoutFactor Timeout factor which will be multiplied with frequency to determine timeout
	 */
	public static void startHeartbeat(boolean activateHeartbeat, int frequencyInSeconds, int heartbeatTimeoutFactor){
		platformLoadedCheck();
		bootProperties.put(START_HEARTBEAT, activateHeartbeat);
		bootProperties.put(HEARTBEAT_FREQUENCY, frequencyInSeconds);
		bootProperties.put(HEARTBEAT_TIMEOUT_FACTOR, heartbeatTimeoutFactor);
	}
	
	/**
	 * Configures the discovery service.
	 * @param activateDiscovery Activates service
	 * @param startDiscovery Starts discovery (else only passive)
	 */
	public static void activateNetworkDiscovery(boolean activateDiscovery, boolean startDiscovery){
		platformLoadedCheck();
		bootProperties.put(ACTIVATE_DISCOVERY, String.valueOf(activateDiscovery));
		bootProperties.put(START_DISCOVERY, String.valueOf(startDiscovery));
	}
	
	/**
	 * Configures the Network discovery mode (BROADCAST or MULTICAST) 
	 * (see constants in @DiscoveryModes).
	 * @param discoveryMode Discovery mode to be used
	 */
	public static void setNetworkDiscoveryMode(String discoveryMode){
		platformLoadedCheck();
		bootProperties.put(DISCOVERY_MODE, String.valueOf(discoveryMode));
	}
	
	/**
	 * Configures Multicast group for discovery service
	 * (if Multicast is chosen for network discovery)
	 * @param multicastAddress Multicast group address
	 * @param port Port to bind
	 */
	public static void setMulticastGroup(String multicastAddress, Integer port){
		platformLoadedCheck();
		bootProperties.put(DISCOVERY_MULTICAST_ADDRESS, multicastAddress);
		bootProperties.put(DISCOVERY_MULTICAST_PORT, port);
	}
	
	/**
	 * Sets the broadcast port on which discovery service will be listening 
	 * (if broadcast discovery mode is chosen).
	 * @param port Broadcast port to bind
	 */
	public static void setBroadcastPort(Integer port){
		platformLoadedCheck();
		bootProperties.put(DISCOVERY_BROADCAST_PORT, port);
	}
	
	/**
	 * Sets the network serialization for the network communication
	 * (see @SerializationTypes).
	 * @param serialization Serialization type to use
	 */
	public static void setNetworkSerialization(String serialization){
		platformLoadedCheck();
		serialization = serialization.trim().toUpperCase();
		if(serialization.equals(SerializationTypes.JAVA) || serialization.equals(SerializationTypes.XML) 
				|| serialization.equals(SerializationTypes.JSON) || serialization.equals(SerializationTypes.JAVA_COMPATIBILITY)){
			bootProperties.put(NETWORK_SERIALIZATION, serialization);
		} else {
			System.err.println("Selected network serialization type " + serialization + " is not supported.");
		}
	}
	
	/**
	 * Indicates if platform components should be started lazy.
	 * Default: true
	 * @param lazy true --> lazy, false --> explicit start using startPlatform().
	 */
	public static void startLazy(boolean lazy){
		platformLoadedCheck();
		bootProperties.put(START_LAZY, lazy);
	}
	
	/**
	 * Sets the internal message transport system (JETLANG or MICRO_FIBER)
	 * (see @MessagePassingFrameworks). Defaults to JETLANG. 
	 * Note: Use MICRO_FIBER for large number of agents for better scalability (e.g. 100+ agents).
	 * @param framework Framework to use for message transport
	 */
	public static void setInternalMessageTransport(String framework){
		platformLoadedCheck();
		framework = framework.trim().toUpperCase();
		bootProperties.put(INTERNAL_MESSAGE_TRANSPORT, framework);
	}
	
	/**
	 * Sets synchronous (i.e. blocking) delivery mode for local message passing.
	 * Default: false. 
	 * Note, that this requires the implementation of the handleMessage() method 
	 * for @SocialRole implementations to prevent messages from being stored in a 
	 * message queue (asynchronous mode). The system will do this check at runtime.
	 * @param synchronousMode Specifies if synchronous message delivery is chosen
	 */
	public static void setSynchronousMessageDeliveryMode(boolean synchronousMode){
		platformLoadedCheck();
		bootProperties.put(SYNCHRONOUS_EXECUTION_MODE, synchronousMode);
	}
	
	/**
	 * Activates the Clojure support for the platform. If activated
	 * allows the use of Clojure for role implementations.
	 * @param activated Indicates if Clojure support should be activated
	 */
	public static void activateClojureSupport(boolean activated){
		platformLoadedCheck();
		bootProperties.put(CLOJURE_SUPPORT, activated);
	}
	
	/**
	 * Sets the console output level of platform messages.
	 * Values: 
	 * 0 --> no application-level platform output - only initialization configuration output and errors
	 * 1 --> Additionally to 0, provide output on agent creation/disposal.
	 * 2 --> Additionally to 1, output warnings.
	 * @param outputLevel Level of output as indicated above
	 */
	public static void setPlatformOutputLevel(int outputLevel){
		platformLoadedCheck();
		bootProperties.put(PLATFORM_OUTPUT_LEVEL, outputLevel);
	}
	
	/**
	 * Activates time prefix for platform output, optionally including 
	 * date. Alternatively to conventional time display, nanotime can be displayed.
	 * @param activate Activates time prefix (if false, no prefix will be printed independent of other parameters)
	 * @param dateOutput Prints date along with time
	 * @param nanoTimeOutput Prints nanotime output instead of conventional calendar time format
	 */
	public static void setTimePrefixForPlatformOutput(boolean activate, boolean dateOutput, boolean nanoTimeOutput){
		platformLoadedCheck();
		if(activate && dateOutput && !nanoTimeOutput){
			bootProperties.put(PREFIX_PLATFORM_OUTPUT_WITH_TIME, PlatformOutputLevels.DATE_TIME_PREFIX);
		} else if(activate && nanoTimeOutput){
			bootProperties.put(PREFIX_PLATFORM_OUTPUT_WITH_TIME, PlatformOutputLevels.NANO_TIME_PREFIX);
		} else if(activate && !dateOutput && !nanoTimeOutput){
			bootProperties.put(PREFIX_PLATFORM_OUTPUT_WITH_TIME, PlatformOutputLevels.TIME_PREFIX);
		} else {
			bootProperties.put(PREFIX_PLATFORM_OUTPUT_WITH_TIME, PlatformOutputLevels.NO_TIME_PREFIX);
		}
	}
	
	/**
	 * Sets the console output level for individual agents/roles. This 
	 * allows a fast switch to boost performance.
	 * Values: 
	 * 0 --> no agent output (neither print() nor printError())
	 * 1 --> only error output (printError())
	 * 2 --> all output produced by agent (print() and printError())
	 * @param outputLevel Level of output as indicated above
	 */
	public static void setAgentConsoleOutputLevel(int outputLevel){
		platformLoadedCheck();
		bootProperties.put(AGENT_CONSOLE_OUTPUT_LEVEL, outputLevel);
	}
	
	/**
	 * Indicates if each line of multiline output of agents on console 
	 * should be prefixed with agent name.
	 * Default: false
	 * @param prefixForMultiLineOutput Switch indicating if each line is 
	 * prefixed with agent name or only first one (false)
	 */
	public static void setPrefixForMultiLineAgentConsoleOutput(boolean prefixForMultiLineOutput){
		platformLoadedCheck();
		bootProperties.put(PREFIX_MULTILINE_AGENT_OUTPUT, prefixForMultiLineOutput);
	}
	
	/**
	 * Indicates if message type (e.g. 'INFO' or 'ERROR') should be printed explicitly 
	 * in agent console output to allow easier filtering by message type during analysis.
	 * Default: false
	 * @param printMessageTypePrefix Switch indicating if message type is printed
	 */
	public static void setMessageTypePrefixForAgentConsoleOutput(boolean printMessageTypePrefix){
		platformLoadedCheck();
		bootProperties.put(PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE, printMessageTypePrefix);
	}
	
	/**
	 * Indicates if agent console output should be prefixed with time in nanoseconds.
	 * Default: false
	 * @param setTimePrefix Boolean indicating if nanotime should be printed
	 */
	public static void setNanoTimePrefixForAgentConsoleOutput(boolean setTimePrefix){
		platformLoadedCheck();
		bootProperties.put(PREFIX_AGENT_OUTPUT_WITH_NANOTIME, setTimePrefix);
	}
	
	/**
	 * Sets the seed for the pseudo random number generator (RNG). 
	 * Can be changed at runtime (not advisable!).
	 * @param seed Seed to be used for platform RNG
	 */
	public static void setRandomNumberGeneratorSeed(long seed){
		//no platformLoadedCheck - seed can be changed any time
		bootProperties.put(RANDOM_NUMBER_GENERATOR_SEED, seed);
		//if platform already initialized, change it at runtime
		if(platformInitialized){
			MTRuntime.setRandomNumberGeneratorSeed(seed);
		}
	}
	
	/**
	 * Sets the platform name.
	 * @param name Name of the platform
	 */
	public static void setPlatformName(String name){
		platformLoadedCheck();
		bootProperties.put(PLATFORM_NAME, name);
	}
	
	private static void determineOS(){
		if(System.getProperty("java.vm.name").equals("Dalvik")){
			bootProperties.put(OPERATING_SYSTEM, OperatingSystems.ANDROID);
			return;
		}
		if((System.getProperty("user.dir")).substring(0, 1).equals("/")){
			bootProperties.put(OPERATING_SYSTEM, OperatingSystems.UNIX);
			return;
		}
		if((System.getProperty("user.dir")).substring(1, 2).equals(":")){
			bootProperties.put(OPERATING_SYSTEM, OperatingSystems.WINDOWS);
			return;
		}
		bootProperties.put(OPERATING_SYSTEM, OperatingSystems.UNKNOWN);
	}
	
	/**
	 * Reads the platform configuration from configuration file 'platform.xml' 
	 * in user directory (usually project root path).
	 * @throws Exception Exception thrown if operation system could not be detected
	 */
	public static void readConfiguration() throws Exception {
		
		determineOS();
		
		if(bootProperties.containsKey(OPERATING_SYSTEM)){
			if(bootProperties.getProperty(OPERATING_SYSTEM).equals(OperatingSystems.ANDROID)){
				if(!bootProperties.containsKey(NETWORK_SERIALIZATION)){
					System.out.println("Enforcing " + SerializationTypes.JAVA_COMPATIBILITY + " network serialization on Android.");
					bootProperties.put(NETWORK_SERIALIZATION, SerializationTypes.JAVA_COMPATIBILITY);
				}
			}
		} else {
			throw new RuntimeException("Severe error on reading operating system in boot properties!");
		}
		
		bootProperties.putAll(MicroConfigLoader.getConfiguration(configSection));
	}
	
}
