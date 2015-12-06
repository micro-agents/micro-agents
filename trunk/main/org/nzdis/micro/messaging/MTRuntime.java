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
package org.nzdis.micro.messaging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.jetlang.channels.MemoryChannel;
import org.nzdis.micro.AbstractAgent;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.bootloader.MicroBootProperties;
import org.nzdis.micro.bootloader.MicroPropertiesMap;
import org.nzdis.micro.bootloader.Version;
import org.nzdis.micro.constants.AgentConsoleOutputLevels;
import org.nzdis.micro.constants.DiscoveryModes;
import org.nzdis.micro.constants.MessagePassingFrameworks;
import org.nzdis.micro.constants.PlatformOutputLevels;
import org.nzdis.micro.constants.PlatformConstants;
import org.nzdis.micro.constants.SerializationTypes;
import org.nzdis.micro.events.RemotePlatformLocationEvent;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.messaging.network.NetworkConnectorInterface;
import org.nzdis.micro.messaging.network.discovery.DiscoveryService;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;
import org.nzdis.micro.messaging.processor.AbstractMicroFiber;
import org.nzdis.micro.messaging.processor.Worker;
import org.nzdis.micro.messaging.processor.MicroFiber;
import org.nzdis.micro.messaging.processor.Scheduler;
import org.nzdis.micro.msgvalidator.DefaultMicroMessageValidator;
import org.nzdis.micro.msgvalidator.MicroMessageValidator;
import org.nzdis.micro.random.MersenneTwister;

/**
 * MTRuntime is the core Runtime facility on the Message Transport Layer
 * of the micro-agent platform. It manages a wide range of functionality 
 * (asynchronous message passing, network (transport and discovery) along 
 * with effectively all thread-related concerns.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.1 $ $Date: 2013/02/08 00:00:00 $
 * 
 */
public class MTRuntime extends PlatformConstants {
	
	private static String platformName = "";
	
	private static String operatingSystem = null;
	
	private static int NETTY_TCP_PORT = 7938;
	
	/** indicates if the network management can select a free port if
	 * the parametrized is bound.
	 */
	private static boolean allowDynamicPortSelection = true;
	
	private static String multicastGroup = "225.0.0.1";
	
	private static int multicastPort = 4444;
	
	private static int broadcastPort = 5555;
	
	protected static boolean isDistributed = false;
	
	private static boolean isPropagating = true;
	
	private static boolean discoveryStart = true;
	
	private static boolean discoveryActivated = true;
	
	private static String discoveryMode = DiscoveryModes.MULTICAST;
	
	/** Frequency of discovery announcement sending in seconds */
	private static int discoveryFrequency = 5;

	/** Indicates if discovery is switched off once platform is connected 
	 * to other platform. Will be automatically be switched on again
	 * if all connections are lost.
	 */
	private static boolean switchOffDiscoveryOnceConnected = false;

	private static int numberOfSchedulers = 1;

	private static int numberOfWorkers = 0;

	private static int numberOfCPUCores = 0;

	/** Directories for both default internal messaging and Jetlang */
	private static ConcurrentHashMap<String, AbstractMicroFiber> registeredMicroFibersMap = new ConcurrentHashMap<String, AbstractMicroFiber>();
	private static ConcurrentHashMap<String, MemoryChannel<MicroMessage>> registeredJetlangChannelsMap = new ConcurrentHashMap<String, MemoryChannel<MicroMessage>>();
	private static final MemoryChannel<MicroMessage> broadCastChannel = new MemoryChannel<MicroMessage>();
	
	/**
	 * Array holding all initialized worker threads
	 */
	private static Worker[] workers = null;

	/**
	 * Array holding all initialized schedulers
	 */
	private static Scheduler[] schedulers = null;

	/**
	 * Index for iterating over worker threads for fair selection
	 */
	private static int workerIndex = 0;
	
	/**
	 * Index for iterating over scheduler threads for fair selection
	 */
	private static int schedulerIndex = 0;

	private static String serialization = SerializationTypes.XML;
	
	private static String internalMessageFramework = MessagePassingFrameworks.JETLANG;
	
	protected static NetworkConnectorInterface networkConnector = null;
	
	private static boolean lazyInitialization = true;
	
	private static boolean platformInitialized = false;
	
	private static boolean microFiberWorkersStarted = false;
	
	/** specifies if local messages are passed in a synchronous (i.e. blocking) manner */
	private static boolean synchronousOperationMode = false;
	
	/** Reserved words and platform message prefixes */
	
	private static String platformPrefix = "MicroMessaging Platform: ";
	
	/** indicates if platform output should be prefixed with current time */
	private static boolean prefixPlatformOutputWithTime = false;
	private static boolean prefixPlatformOutputWithDateTime = false;
	private static boolean prefixPlatformOutputWithNanoTime = false;
	
	/** keyword indicating definite request for propagation (one Platform might have lost data).*/
	public static final String propagationInitializationKeyword = "INIT";
	
	/** keyword indicating platform network disconnection (remote shutdown) */
	public static final String remotePlatformShutdownKeyword = "DISCONNECT";
	
	/** keywords indicating changed values in propagated fibers */
	public static final String processAdditionKeyword = "ADD_AGENT";
	
	public static final String processRemovalKeyword = "DEL_AGENT";
	
	public static final String platformIdKeyword = "PLATFORM_ID";

	//reserved word for identification of node in serialized message
	public static final String nodeKeyword = "NODE_KEYWORD";
	
	public static final String nodePortKeyword = "PORT_KEYWORD";
	
	public static final String roleKeyword = "ROLE_KEYWORD";
	
	//keyword used in messages to other nodes to identify recipient process
	public static final String processSerializationKeyword = "TARGET_AGENT";
	
	//keyword to indicate messages to platform (e.g. for propagation)
	public static final String platformProcess = "PLATFORM";
	
	/** keyword maintaining remote intent resolution interactions */
	public static final String intentResolutionKeyword = "INTENT_RESOLUTION";
	
	/** keyword to indicate that intent lookup has to take place on remote node - although message
	   message encapsulating might be broadcast message --> remove broadcast and deliver to
	   individual intent processor */
	public static final String intentResolutionRequestKeyword = "RESOLVE_INTENT_REQUEST";
	
	/** keyword indicating that remote intent resolution failed */
	public static final String intentResolutionFailedKeyword = "RESOLVE_INTENT_FAILED";
	
	/**
	 * Indicates the number of remote nodes propagated with this node.
	 */
	public static final String numberOfRemoteNodes = "NUMBER_OF_REMOTE_NODES";
	
	/**
	 * Name of GenericIntentProcessor (Identifier for platform to solve with internal GenericIntentProcessor).
	 */
	public static final String GENERIC_INTENT_PROCESSOR_NAME = "GenericIntentProcessor";
	
	/**
	 * Output level of platform
	 */
	protected static Integer platformOutputLevel = PlatformOutputLevels.INITIALIZATION_INFO;
	
	/**
	 * Output level for all agent-/role-produced output (by application developer).
	 */
	protected static Integer agentConsoleOutputLevel = AgentConsoleOutputLevels.ALL;
	
	/**
	 * Indicates if EVERY line of agent output via print() or printError() is prefixed with agent name (eases filtering of console output).
	 */
	protected static boolean prefixOutputLinesWithAgentPrefix = false;
	
	/**
	 * Indicates if every line of output should be prefixed with time in nanoseconds.
	 */
	protected static boolean prefixAgentOutputWithNanoTime = false;
	
	/**
	 * Indicates a message type prefix (for "INFO" and "ERROR") for agent output.
	 */
	protected static boolean prefixMessageTypeIndicationForAgentOutput = false;
	
	/**
	 * Indicates if system should show errors when picking randomly.
	 */
	public static boolean showErrorsWhenPickingRandomly = false;
	
	/**
	 * Indicates if system should show stack trace if errors occur (to ease spotting in code).
	 */
	public static boolean showStackTraceOnErrors = false;
	
	/**
	 * Clojure activation indicator
	 */
	private static boolean ClojureActivated = false;
	
	public static final String LINE_DELIMITER = System.getProperty("line.separator");
	
	/** Network-related stuff */
	
	//maintains all propagated processes from other nodes (format <process, nodeIP>)
	private static ConcurrentHashMap<String, ArrayList<String>> propagatedRemoteProcessMap = new ConcurrentHashMap<String, ArrayList<String>>();
	
	//maintains list of nodes which have been propagated to
	private static ConcurrentHashMap<String, Boolean> propagatedNodes = new ConcurrentHashMap<String, Boolean>();
	
	//IDs of registered and propagated platforms (Key: SocketAddress.toString, Value: Platform ID)
	private static ConcurrentHashMap<String, String> propagatedNodeIDs = new ConcurrentHashMap<String, String>();
	
	//name table of nodes
	private static ConcurrentHashMap<String, String> nodeNameTable = new ConcurrentHashMap<String, String>();

	//reference to message validator
	private static volatile MicroMessageValidator validator = null;
	
	private static ArrayList<String> reservedWords = new ArrayList<String>();
	
	public static final String broadcastPrimitive = "BROADCAST";
	
	public static final String rolecastPrimitive = "ROLECAST";
	
	public static final String shutdownPerformative = "SHUTDOWN_PLATFORM";
	
	public static String anonymousAgentPrefix = "AnonymousAgent";
	
	/** counter for automatic generation of agent name */
	private static Integer anonymousAgentNameCounter = 0;
	private static int suffixLength = 7;
	
	private static long startTime = System.currentTimeMillis();
	
	/** network heartbeat settings */
	private static boolean heartbeatActivated = true;
	/** heartbeat frequency in seconds */
	private static int heartbeatFrequency = 30;
	/** heartbeat timeout factor */
	private static int heartbeatTimeoutFactor = 3;
	
	/** random number generator */
	private static long seed = System.currentTimeMillis();
	protected static volatile MersenneTwister random = null;
	
	/** platform ID for unification */
	protected static String platformID = null;

	/** debug switch */
	private static final boolean debug = false;
	
	/** location of agent platform */
	private static volatile Location location = null;
	
	public static void load(){
		
	}
	
	static{
		
		//assignProperties(MicroBootProperties.bootProperties);
		
		/* calculate platform ID */
		Long idSuffix = new MersenneTwister().nextLong(System.currentTimeMillis());
		platformID = platformName.concat(idSuffix.toString());
		
		/* Reserved words */
		reservedWords.add(broadcastPrimitive);
		reservedWords.add(rolecastPrimitive);
		reservedWords.add(platformProcess);
		reservedWords.add(processSerializationKeyword);
		reservedWords.add(propagationInitializationKeyword);
		reservedWords.add(nodeKeyword);
		reservedWords.add(shutdownPerformative);
		reservedWords.add(remotePlatformShutdownKeyword);
		
		// Determine no. of cores and start coreExecuters
		numberOfCPUCores = Runtime.getRuntime().availableProcessors();

		numberOfWorkers = numberOfCPUCores;
		
		networkConnector = NettyNetworkConnector.getInstance();
		
		//assign default location if not already assigned
		if(location == null){
			location = new Location("DefaultLocation");
		}
		
		if(!lazyInitialization){
			initializePlatform();
		}
	}
	
	public static void initializePlatform(){
		if(!platformInitialized){
			
			//reassignment of properties if those have been changed after shutdown.
			assignProperties(MicroBootProperties.bootProperties);
			
			//System.out.println(getPlatformPrefix() + "Initializing.");
			platformInitialized = true;
			
			if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
				if(!lazyInitialization){
					startMicroFiberWorkers();
				}
			}
			
			// start of core schedulers
			schedulers = new Scheduler[numberOfSchedulers];
			for (int i = 0; i < schedulers.length; i++) {
				schedulers[i] = new Scheduler();
				schedulers[i].setName("MicroFiber_Scheduler_" + i);
				schedulers[i].start();
			}
			
			startNetwork();
			
			printSystemConfig();
		}
	}

	private static void printSystemConfig(){
		String bar = LINE_DELIMITER + "|==========================================|";
		String finalBar = LINE_DELIMITER + "|========== Platform initialized ==========|" + LINE_DELIMITER;
		
		StringBuffer platformHeader = new StringBuffer(bar)
					.append(LINE_DELIMITER + "| Micro-Agent Platform Loader              |") 
					.append(LINE_DELIMITER + "| Version: ").append(Version.VERSION)
					.append("                             |") 
					.append(bar);
		
		platformHeader.append(LINE_DELIMITER + "Micro Messaging Runtime parameters:")
			.append(LINE_DELIMITER + "PLATFORM_NAME: ").append(platformName)
			.append(LINE_DELIMITER + "PLATFORM_ID: ").append(platformID)
			.append(LINE_DELIMITER + "OPERATING_SYSTEM: ").append(operatingSystem)
			.append(LINE_DELIMITER + "OUTPUT_LEVEL: ").append(PlatformOutputLevels.getOutputLevelAsString(platformOutputLevel))
			.append(LINE_DELIMITER + "AGENT_OUTPUT_LEVEL: ").append(AgentConsoleOutputLevels.getOutputLevelAsString(agentConsoleOutputLevel))
			.append(LINE_DELIMITER + PlatformConstants.PREFIX_PLATFORM_OUTPUT_WITH_TIME).append(": ")
				.append((MicroBootProperties.bootProperties.containsKey(PlatformConstants.PREFIX_PLATFORM_OUTPUT_WITH_TIME) ? 
					MicroBootProperties.bootProperties.get(PREFIX_PLATFORM_OUTPUT_WITH_TIME).toString() : PlatformOutputLevels.NO_TIME_PREFIX))
			.append(LINE_DELIMITER + "PREFIX_MULTILINE_AGENT_OUTPUT_WITH_NAME: ").append(prefixOutputLinesWithAgentPrefix)
			.append(LINE_DELIMITER + "PREFIX_AGENT_OUTPUT_WITH_TIME: ").append(prefixAgentOutputWithNanoTime)
			.append(LINE_DELIMITER + "RANDOM_NUMBER_GENERATOR_SEED: ").append(seed)
			.append(LINE_DELIMITER + "DISTRIBUTED_MODE: ").append(isDistributed);
		if(isDistributed){
			platformHeader.append(LINE_DELIMITER).append("NETWORK_SERIALIZATION: ").append(MTRuntime.serialization)
				.append(LINE_DELIMITER).append("NETWORK_TCP_PORT: ").append(MTRuntime.getTcpPort())
				.append(LINE_DELIMITER).append("ACTIVATE_DISCOVERY: ").append(MTRuntime.discoveryActivated);
			if(discoveryActivated){
				platformHeader.append(LINE_DELIMITER).append("START_DISCOVERY: ").append(MTRuntime.discoveryStart)
					.append(LINE_DELIMITER).append("DISCOVERY_MODE: ").append(MTRuntime.discoveryMode)
					.append(LINE_DELIMITER).append("DISCOVERY_FREQUENCY: ").append(MTRuntime.discoveryFrequency);
				if(discoveryMode.equals(DiscoveryModes.BROADCAST)){
					platformHeader.append(LINE_DELIMITER).append("DISCOVERY_BROADCAST_PORT: ").append(MTRuntime.broadcastPort);	
				}
				if(discoveryMode.equals(DiscoveryModes.MULTICAST)){
					platformHeader.append(LINE_DELIMITER).append("DISCOVERY_MULTICAST_GROUP: ").append(MTRuntime.multicastGroup)
						.append(LINE_DELIMITER).append("DISCOVERY_MULTICAST_PORT: ").append(MTRuntime.multicastPort);
				}
			}
			platformHeader.append(LINE_DELIMITER).append("HEARTBEAT_ENABLED: ").append(heartbeatActivated);
			if(heartbeatActivated){
				platformHeader.append(LINE_DELIMITER).append("HEARTBEAT_FREQUENCY: ").append(heartbeatFrequency)
				//heartbeat timeout factor is multiplied with the frequency to determine when a connection is timed out.
					.append(LINE_DELIMITER).append("HEARTBEAT_TIMEOUT_FACTOR: ").append(heartbeatTimeoutFactor);
			}
		}
		if(!synchronousOperationMode){
			platformHeader.append(LINE_DELIMITER).append("INTERNAL_MESSAGING_FRAMEWORK: ") 
				.append(MTRuntime.internalMessageFramework)
				.append(LINE_DELIMITER).append("CLOJURE_ENABLED: ").append(MTRuntime.ClojureActivated)
				.append(LINE_DELIMITER).append("NUMBER_OF_CORE_SCHEDULERS: ").append(numberOfSchedulers)
				.append(LINE_DELIMITER).append("NUMBER_OF_CORE_EXECUTERS: ").append(numberOfWorkers);
		} else {
			platformHeader.append(LINE_DELIMITER).append("SYNCHRONOUS_MESSAGE_PASSING: true");
		}
		if(validator != null){
			platformHeader.append(LINE_DELIMITER).append("MICROMESSAGE_VALIDATOR: ").append(validator.getClass().getCanonicalName());
		}
		platformHeader.append(LINE_DELIMITER).append("LAZY_INITIALIZATION: ").append(lazyInitialization)
				.append(finalBar);
		System.out.println(platformHeader);
		
	}
	
	
	protected static void shutdownPlatform(){
		if(platformInitialized){
			
			platformInitialized = false;
			
			if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
				if(microFiberWorkersStarted && workers != null){
					// Stop the worker threads for MICRO_FIBER framework (if started)
					for (int i = 0; i < workers.length; i++) {
						workers[i].stopWorker();
					}
				}
			}
			
			// stop the schedulers
			for (int i = 0; i < schedulers.length; i++) {
				schedulers[i].stopScheduler();
			}
			System.out.println(getPlatformPrefix() + "Platform threads shut down.");
			
			shutdownNetwork();
		}
	}
	
	private synchronized static void startNetwork(){
		// InterNode Interaction and Cross Language Compatibility
		if (isDistributed) {
			
			//if not already started, start network asynchronously
			if(!networkConnector.networkStarted()){
				networkConnector.setSerialization(serialization);
				
				//start network asynchronously
				Runnable networkStart = new Runnable(){
					@Override
					public void run() {
						
						networkConnector.startNetwork();
						//System.out.println(new StringBuilder(MTRuntime.getPlatformPrefix()).append("Starting network"));
						if(networkConnector.networkStarted()){
							startDiscovery();
						} else {
							System.err.println(new StringBuilder(MTRuntime.getPlatformPrefix()).append("Discovery will not be started as network start failed."));
						}
					}					
				};
				Thread network = new Thread(networkStart);
				network.start();
			}
		}
	}
	
	private static void startDiscovery(){
		DiscoveryService.startDiscovery();
	}
	
	private static void stopDiscovery(){
				DiscoveryService.stopDiscovery();
			}
	private static void checkDiscoveryState(){
		if(platformInitialized && switchOffDiscoveryOnceConnected){
			if(propagatedNodes.isEmpty() || !propagatedNodes.values().contains(Boolean.TRUE)){
				System.out.println(getPlatformPrefix() + "All remote platforms disconnected. Restarted discovery.");
				startDiscovery();
			} else {
				System.out.println(getPlatformPrefix() + "Remote platform connected. Switched off discovery.");
				stopDiscovery();
			}
		}
	}
	
	private static void shutdownNetwork(){
		if(networkConnector != null){
			if(discoveryActivated){
				stopDiscovery();
			}
			Iterator<String> it = propagatedNodes.keySet().iterator();
			while(it.hasNext()){
				SocketAddress tempKey = SocketAddress.inflate(it.next());
				notifyRemotePlatformToDisconnect(tempKey.getHostAddress(), tempKey.getPort());
			}
			networkConnector.shutdown();
		}
	}
	
	private static void assignProperties(MicroPropertiesMap properties){

		boolean propertiesNotFound = false;
		StringBuffer messageStart = new StringBuffer(getPlatformPrefix()).append("INFO: Some properties have not been configured:").append(LINE_DELIMITER).append("(");
		StringBuffer separator = new StringBuffer(", ");
		StringBuffer messageEnd = new StringBuffer(").").append(LINE_DELIMITER).append("Using default values where applicable.");
		
		if(properties.containsKey(OPERATING_SYSTEM)){
			operatingSystem = properties.getProperty(OPERATING_SYSTEM);
		} else {
			System.out.println(new StringBuilder(MTRuntime.getPlatformPrefix()).append("Operating system not properly detected."));
		}
		
		if(properties.containsKey(PLATFORM_NAME)){
			if(platformName.equals("")){
				platformName = properties.getProperty(PLATFORM_NAME).trim();
				if(!platformName.equals("")){
					platformPrefix = platformPrefix + platformName + ": ";
				}
			}
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PLATFORM_NAME);
		}

		if(properties.containsKey(SYNCHRONOUS_EXECUTION_MODE)){
			synchronousOperationMode = properties.getBoolean(SYNCHRONOUS_EXECUTION_MODE);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(SYNCHRONOUS_EXECUTION_MODE);
		}
		
		if(properties.containsKey(INTERNAL_MESSAGE_TRANSPORT)){
			internalMessageFramework = properties.getProperty(INTERNAL_MESSAGE_TRANSPORT).trim().toUpperCase();
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(INTERNAL_MESSAGE_TRANSPORT);
		}
		
		if(properties.containsKey(CLOJURE_SUPPORT)){
			ClojureActivated = properties.getBoolean(CLOJURE_SUPPORT);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(CLOJURE_SUPPORT);
		}
		
		if(properties.containsKey(PLATFORM_OUTPUT_LEVEL)){
			platformOutputLevel = Integer.parseInt(properties.get(PLATFORM_OUTPUT_LEVEL).toString());
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PLATFORM_OUTPUT_LEVEL);
		}
		
		if(properties.containsKey(PREFIX_PLATFORM_OUTPUT_WITH_TIME)){
			String platformTimePrefixType = properties.get(PREFIX_PLATFORM_OUTPUT_WITH_TIME).toString();
			if(platformTimePrefixType.equals(PlatformOutputLevels.NO_TIME_PREFIX)){
				prefixPlatformOutputWithTime = false;
				prefixPlatformOutputWithDateTime = false;
				prefixPlatformOutputWithNanoTime = false;
			} else if(platformTimePrefixType.equals(PlatformOutputLevels.TIME_PREFIX)){
				prefixPlatformOutputWithTime = true;
				prefixPlatformOutputWithDateTime = false;
				prefixPlatformOutputWithNanoTime = false;
			} else if(platformTimePrefixType.equals(PlatformOutputLevels.DATE_TIME_PREFIX)){
				prefixPlatformOutputWithTime = true;
				prefixPlatformOutputWithDateTime = true;
				prefixPlatformOutputWithNanoTime = false;
			} else if(platformTimePrefixType.equals(PlatformOutputLevels.NANO_TIME_PREFIX)){
				prefixPlatformOutputWithTime = true;
				prefixPlatformOutputWithDateTime = false;
				prefixPlatformOutputWithNanoTime = true;
			}
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PREFIX_PLATFORM_OUTPUT_WITH_TIME);
		}
		
		if(properties.containsKey(AGENT_CONSOLE_OUTPUT_LEVEL)){
			agentConsoleOutputLevel = Integer.parseInt(properties.get(AGENT_CONSOLE_OUTPUT_LEVEL).toString());
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(AGENT_CONSOLE_OUTPUT_LEVEL);
		}
		
		if(properties.containsKey(PREFIX_MULTILINE_AGENT_OUTPUT)){
			prefixOutputLinesWithAgentPrefix = properties.getBoolean(PREFIX_MULTILINE_AGENT_OUTPUT);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PREFIX_MULTILINE_AGENT_OUTPUT);
		}
		
		if(properties.containsKey(PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE)){
			prefixMessageTypeIndicationForAgentOutput = properties.getBoolean(PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PREFIX_AGENT_OUTPUT_WITH_MESSAGE_TYPE);
		}
		
		if(properties.containsKey(PREFIX_AGENT_OUTPUT_WITH_NANOTIME)){
			prefixAgentOutputWithNanoTime = properties.getBoolean(PREFIX_AGENT_OUTPUT_WITH_NANOTIME);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(PREFIX_AGENT_OUTPUT_WITH_NANOTIME);
		}
		
		if(properties.containsKey(RANDOM_NUMBER_GENERATOR_SEED)){
			seed = Long.parseLong(properties.get(RANDOM_NUMBER_GENERATOR_SEED).toString().replaceAll("L", ""));
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(RANDOM_NUMBER_GENERATOR_SEED);
		}
		
		if(properties.containsKey(DISTRIBUTED_MODE)){
			isDistributed = properties.getBoolean(DISTRIBUTED_MODE);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(DISTRIBUTED_MODE);
		}

		//check on Serialization type
		if(properties.containsKey(NETWORK_SERIALIZATION)){
			serialization = properties.getProperty(NETWORK_SERIALIZATION).trim().toUpperCase();
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(NETWORK_SERIALIZATION);
			}
		}
		
		
		//default: 1 scheduler
		if (properties.containsKey(NUMBER_OF_SCHEDULERS)){
			numberOfSchedulers = Integer.parseInt(properties
					.getProperty(NUMBER_OF_SCHEDULERS));

			if (numberOfSchedulers <= 0){
				numberOfSchedulers = 1;
				System.out
						.println("The value of parameter NUMBER_OF_CORE_SCHEDULERS in properties file must be greater than 0" + LINE_DELIMITER
								+ "setting it to default value 1");
			}
		} else {
			if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(NUMBER_OF_SCHEDULERS);
			}
		}
			
		//default: number of cpu cores
		if (properties.containsKey(NUMBER_OF_WORKERS)) {
			numberOfWorkers = Integer.parseInt(properties
					.getProperty(NUMBER_OF_WORKERS));
			if (numberOfWorkers <= 0)
			{
				numberOfWorkers = numberOfCPUCores;
				System.out
						.println("The value of parameter NUMBER_OF_CORE_EXECUTERS in properties file must be greater than 0" + LINE_DELIMITER
								+ "setting it to default value equals to the number Of CPUCores");

			}
		} else {
			if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(NUMBER_OF_WORKERS);
			}
		}
			
		if(properties.containsKey(MICROMESSAGE_VALIDATOR)){
			try {
				validator = (MicroMessageValidator) Class.forName(properties.get(MICROMESSAGE_VALIDATOR).toString()).newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(MICROMESSAGE_VALIDATOR);
		}
		
		if(properties.containsKey(START_LAZY)){
			lazyInitialization = properties.getBoolean(START_LAZY);
		} else {
			if(propertiesNotFound){
				messageStart.append(separator);
			}
			propertiesNotFound = true;
			messageStart.append(START_LAZY);
		}

		if(properties.containsKey(NETTY_MICRO_PORT)){
			NETTY_TCP_PORT = Integer.parseInt(properties.getProperty(NETTY_MICRO_PORT));
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(NETTY_MICRO_PORT);
			}
		}
		
		if(properties.containsKey(DYNAMIC_PORT_SELECTION)){
			allowDynamicPortSelection = properties.getBoolean(DYNAMIC_PORT_SELECTION);
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DYNAMIC_PORT_SELECTION);
			}
		}
		
		if(properties.containsKey(ACTIVATE_DISCOVERY)){
			discoveryActivated = properties.getBoolean(ACTIVATE_DISCOVERY);
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(ACTIVATE_DISCOVERY);
			}
		}
		
		if(properties.containsKey(START_DISCOVERY)){
			discoveryStart = properties.getBoolean(START_DISCOVERY);
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(START_DISCOVERY);
			}
		}

		if(properties.containsKey(DISCOVERY_MODE)){
			discoveryMode = properties.getProperty(DISCOVERY_MODE).trim().toUpperCase();
		} else {
			if(discoveryActivated && isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DISCOVERY_MODE);
			}
		}
		
		if(properties.containsKey(DISCOVERY_FREQUENCY)){
			discoveryFrequency = Integer.parseInt(properties.getProperty(DISCOVERY_FREQUENCY));
		} else {
			if(discoveryActivated && isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DISCOVERY_FREQUENCY);
			}
		}
		
		if(properties.containsKey(DISCOVERY_MULTICAST_ADDRESS)){
			multicastGroup = properties.getProperty(DISCOVERY_MULTICAST_ADDRESS).trim();
		} else {
			if(discoveryActivated && isDistributed && discoveryMode.equals(DiscoveryModes.MULTICAST)){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DISCOVERY_MULTICAST_ADDRESS);
			}
		}
			
		if(properties.containsKey(DISCOVERY_MULTICAST_PORT)){
			multicastPort = Integer.parseInt(properties.getProperty(DISCOVERY_MULTICAST_PORT));
		} else {
			if(discoveryActivated && isDistributed && discoveryMode.equals(DiscoveryModes.MULTICAST)){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DISCOVERY_MULTICAST_PORT);
			}
		}
		
		if(properties.containsKey(DISCOVERY_BROADCAST_PORT)){
			broadcastPort = Integer.parseInt(properties.getProperty(DISCOVERY_BROADCAST_PORT));
		} else {
			if(discoveryActivated && isDistributed && discoveryMode.equals(DiscoveryModes.BROADCAST)){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(DISCOVERY_BROADCAST_PORT);
			}
		}
		
		if(properties.containsKey(START_HEARTBEAT)){
			heartbeatActivated = properties.getBoolean(START_HEARTBEAT);
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(START_HEARTBEAT);
			}
		}
		
		if(properties.containsKey(HEARTBEAT_FREQUENCY)){
			heartbeatFrequency = Integer.parseInt(properties.getString(HEARTBEAT_FREQUENCY));
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(HEARTBEAT_FREQUENCY);
			}
		}
		
		if(properties.containsKey(HEARTBEAT_TIMEOUT_FACTOR)){
			heartbeatTimeoutFactor = Integer.parseInt(properties.getString(HEARTBEAT_TIMEOUT_FACTOR));
		} else {
			if(isDistributed){
				if(propertiesNotFound){
					messageStart.append(separator);
				}
				propertiesNotFound = true;
				messageStart.append(HEARTBEAT_TIMEOUT_FACTOR);
			}
		}
		
		if(propertiesNotFound && platformOutputLevel > 0){
			System.out.println(messageStart.append(messageEnd).toString());
		}
		
		//indicate that configuration values are assigned (new assignments won't have effect)
		MicroBootProperties.platformInitialized();
		
	}

	/**
	 * Starts the MICRO_FIBER worker threads.
	 */
	private static void startMicroFiberWorkers(){
		if(!microFiberWorkersStarted){
			// Start the Core Executers for MICRO_FIBER framework
			workers = new Worker[numberOfWorkers];
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new Worker();
				workers[i].setName("MicroFiber_Worker_" + i);
				workers[i].start();
			}
			microFiberWorkersStarted = true;
			//System.out.println(getPlatformPrefix() + "MICRO_FIBER worker threads started.");
		} else {
			System.err.println(getPlatformPrefix() + "Start of MICRO_FIBER worker threads requested - although started before.");
		}
	}
	
	/**
	 * Returns the next available worker thread.
	 * @return the next worker thread reference.
	 */
	public static Worker getNextWorker(){
		if(!microFiberWorkersStarted){
			startMicroFiberWorkers();
		}
		Worker executer = workers[workerIndex % numberOfWorkers];
		workerIndex++;
		if(workerIndex == numberOfWorkers){
			workerIndex = 0;
		}
		return executer;
	}

	/**
	 * Returns the next available scheduler.
	 * @return the next scheduler reference (fair selection)
	 */
	public static Scheduler getNextScheduler(){
		Scheduler scheduler = schedulers[schedulerIndex % numberOfSchedulers];
		schedulerIndex++;
		if(schedulerIndex == numberOfSchedulers){
			schedulerIndex = 0;
		}
		return scheduler;
	}
	
	public static void send(MicroMessage message){
		
		if(synchronousOperationMode){
			sendViaMicroFiber(message);
		} else {
			if(internalMessageFramework.equals(MessagePassingFrameworks.JETLANG)){
				sendViaJetlang(message);
			}
			if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
				sendViaMicroFiber(message);
			}
		}
	}
	
	private static void sendViaMicroFiber(MicroMessage message){
		MicroMessage msg = (MicroMessage)message.clone();
		if(msg.getRecipient().equals(broadcastPrimitive)){
			sendMicroFiberBroadcast(msg);
		} else {
			sendLocal(msg.getRecipient(), msg);
		}
	}
	
	private static void sendViaJetlang(MicroMessage msg){
		if(msg.getRecipient().equals(broadcastPrimitive)){
			sendJetlangBroadcast(msg);
		} else {
			//System.out.println(getPlatformPrefix() + "Sent via Jetlang");
			registeredJetlangChannelsMap.get(msg.getRecipient()).publish(msg);
		}
	}
	
	
	public synchronized static void sendLocalBroadcast(MicroMessage message){
		message.setRecipient(broadcastPrimitive);
		if(internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
			sendMicroFiberBroadcast(message);
		}
		if(internalMessageFramework.equals(MessagePassingFrameworks.JETLANG)){
			sendJetlangBroadcast(message);
		}
	}
	
	private synchronized static void sendMicroFiberBroadcast(MicroMessage message){
		Iterator<String> it = registeredMicroFibersMap.keySet().iterator();
		String sender = message.getSender();
		String tempTarget;
		MicroMessage newMessage = null;
		while(it.hasNext()){
			tempTarget = it.next();
			if(!tempTarget.equals(sender)){
				newMessage = (MicroMessage) message.clone();
				newMessage.setRecipient(tempTarget);
				sendLocal(tempTarget, newMessage);
			}
		}
	}
	
	private static void sendJetlangBroadcast(MicroMessage message) {
		broadCastChannel.publish(message);
	}
	
	/**
	 * Sends message to all remote hosts. Key primitive is recipient keyword 
	 * (e.g. for broadcast (broadCastPrimitive), rolecast (roleCastPrimitive)).
	 * @param message
	 * @param keyPrimitive
	 */
	protected static void sendRemotecast(MicroMessage message, String keyPrimitive){
		//network broadcast
		message.setRecipient(keyPrimitive);
		Iterator<String> it = propagatedNodes.keySet().iterator();
		while(it.hasNext()){
			String targetIP = it.next();
			System.out.println(new StringBuffer(getPlatformPrefix()).append("Propagating broadcast to ").append(SocketAddress.inflate(targetIP).getHostAddress()).toString());
			sendRemote(SocketAddress.inflate(targetIP).getHostAddress(), message.getRecipient(), SocketAddress.inflate(targetIP).getPort(), message);
		}
	}
	
	/**
	 * Sends global broadcast. If localBroadcast = true, will send to local hosts as well.
	 * @param message
	 * @param localBroadcast
	 */
	public static void sendGlobalBroadcast(MicroMessage message, boolean localBroadcast) {
		if(localBroadcast){
			//local broadcast
			sendLocalBroadcast(message);
		}
		sendRemotecast(message, broadcastPrimitive);
	}
	
	/**
	 * Sends a message to a registered agent.
	 * @param agentName Name of the target agent.
	 * @param message Message sent to target (format map of key-value pairs)
	 */
	private synchronized static void sendLocal(String agentName, Message message){

		//pick target agent
		AbstractMicroFiber agent = getRegisteredAgent(agentName);
		if (agent == null) {
			System.out.println(new StringBuilder(getPlatformPrefix()).append("Message sent to an unregistered Process: ")
					.append(agentName));
			System.out.println(new StringBuilder(getPlatformPrefix()).append("Message: ").append(message.toString()));
		} else {
			//deliver it synchronously
			if(synchronousOperationMode){
				agent.service(message);
			} else {
				//deliver it asynchronously
				agent.putMessage(message);
				//schedule processing of message delivery
				Scheduler.setAgent(agent);
			}
		}
	}
	
	/**
	 * Indicates if all messages to agents have been delivered 
	 * (if MICRO_FIBER message passing is configured, else returns null)
	 * @return boolean indicating that message queues for all agents are empty
	 */
	public static Boolean allMicroFiberMessagesDelivered(){
		if(!internalMessageFramework.equals(MessagePassingFrameworks.MICRO_FIBER)){
			System.err.println(new StringBuffer(getPlatformPrefix())
				.append(" Check empty message queues only works if using MICRO_FIBER message passing.").append(LINE_DELIMITER).append("Call to method ignored."));
			return null;
		} else {
			for(AbstractMicroFiber fiber: registeredMicroFibersMap.values()){
				if(!fiber.hasEmptyMessageQueue()){
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Sends message to remote node
	 * @param nodeName - remote node or ip address
	 * @param processName - target agent to receive message
	 * @param rawMessage - raw message
	 */
	public static synchronized void sendRemote(String nodeName, String processName, int port, Message rawMessage) {
		if(isDistributed) {
			
			// mark this message as sent by local host (to avoid processing of 'loop' messages)
			rawMessage.addLocalNodeToSenderLog();
			// serialize remote agent name
			rawMessage.put(processSerializationKeyword, processName);
			// set local incoming port (in case of return)
			rawMessage.put(MicroMessage.MSG_PARAM_SENDER_PORT, networkConnector.getPort());
			
			if(debug){
				System.out.println(getPlatformPrefix() + "Sending message " + rawMessage + " to " + nodeName + ":" + port);
			}
			networkConnector.sendMessage(rawMessage, nodeName, port);
		} else {
			System.out.println(new StringBuilder(getPlatformPrefix()).append("Platform is not initalized for DISTRIBUTED_MODE."));
		}
	}
	
	/**
	 * Get the Process from registeredProcessMap if registered.
	 * 
	 * @param processName
	 *            Name of the process by which it is registered in the
	 *            registeredProcessMap.
	 * @return the Process from registeredProcessMap if registered else returns
	 *         null.
	 */
	public static AbstractMicroFiber getRegisteredAgent(String agentName) {
		return registeredMicroFibersMap.get(agentName);
	}

	
	public static void register(String agentName, MessageCommunicator agent)
		throws RuntimeException {
		//lazy initialization
		if(!platformInitialized){
			initializePlatform();
		}
		if (registeredMicroFibersMap.get(agentName) == null && !reservedWords.contains(agentName)) {
			registeredMicroFibersMap.put(agentName, new MicroFiber(agent));
			if(internalMessageFramework.equals(MessagePassingFrameworks.JETLANG)){
				registeredJetlangChannelsMap.put(agentName, new MemoryChannel<MicroMessage>());
			}
			if(isPropagating && isDistributed){
				Iterator<String> it = propagatedNodes.keySet().iterator();
				while(it.hasNext()){
					propagateProcessAdditionToNode(agentName, it.next());
				}
			}
			if(selectivePrintingOfCollectedAgentLogsActivated){
				if(agentsCollectedLogsToBePrinted.contains(agentName)){
					activateSelectivePrintingForAgent(agentName, true);
				} else {
					activateSelectivePrintingForAgent(agentName, false);
				}
			}
		} else {
			throw new RuntimeException("Agent "+ agentName +" already exists or agent name is reserved word.");
		}
	}

	public synchronized static void unregister(String agentName){
		if (registeredMicroFibersMap.containsKey(agentName)){
			registeredMicroFibersMap.remove(agentName);
			registeredJetlangChannelsMap.remove(agentName);
			if(isPropagating && isDistributed){
				Iterator<String> it = propagatedNodes.keySet().iterator();
				while(it.hasNext()){
					propagateProcessRemovalToNode(agentName, it.next());
				}
			}
		} else {
			System.err.println(new StringBuilder(getPlatformPrefix()).append("Agent ")
					.append(agentName).append(" is not registered."));
		}
	}

	/**
	 * Initiates propagation with specified host. Time out is preset to 20 seconds.
	 * @param hostAddress - host address
	 */
	public static void initiatePropagationWithNode(String hostAddress){
		initiatePropagationWithNode(hostAddress, 20000);
	}
	
	public static void initiatePropagationWithNode(String hostAddress, int port){
		initiatePropagationWithNode(hostAddress, port, 20000);
	}

	/**
	 * Initiates a propagation with specified node to exchange registered agents.
	 * @param hostAddress - Host address as string (IPv4)
	 * @param timeOutMs - time out in ms after which the propagation will be considered as failed.
	 */
	public static void initiatePropagationWithNode(String hostAddress, int port, int timeOutMs){
		boolean debug = false;
		
		if(isDistributed && isPropagating){
			
			if(!platformInitialized){
				initializePlatform();
			}
			
			System.out.println(getPlatformPrefix() + "Initializing propagation with " + hostAddress + ":" + port + ".");
			
			if(hostAddress.equals("localhost")){
				hostAddress = "127.0.0.1";
				if(port == getTcpPort()){
					throw new RuntimeException(getPlatformPrefix() + "Propagation to own platform not allowed. Ensure to choose different target port.");
				}
			}
			Message msg = new Message();
			msg.put(propagationInitializationKeyword, platformName);
			//System.out.println("Target: " + hostAddress + ": " + port);
			sendRemote(hostAddress, platformProcess, port, msg);
			
			/** value for checking on successful propagation */
			int testValue = 100;
			/** wait ms before proceeding when having received propagation 
			 * (to ensure that own propagation has reached the other host) */
			int waitTime = 500;
			
			int count = 0;
			boolean success = false;
			
			SocketAddress sAddress = new SocketAddress(hostAddress, port);
			while(((count*testValue) < timeOutMs) && !success){
				try {
					Thread.sleep(testValue);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				//System.out.println("Looked up address: " + sAddress.toString());
				//System.out.println(propagatedNodes.get(sAddress.toString()));
				
				/* Await sending of request */
				if(propagatedNodes.containsKey(sAddress.toString())){
					/* wait for propagation from another node (mutual propagation) */
					if(MTRuntime.getPropagatedNodes().get(sAddress.toString())){
						success = true;
						return;
					}
					
					try {
						Thread.sleep(waitTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println(new StringBuilder(getPlatformPrefix()).append("Propagation initialization with ").append(hostAddress).append(" successful."));
				} else {
					if(debug){
						System.out.println("ms expired: " + (count*testValue) + ", timeout: " + timeOutMs);
					}
				}
				count++;
			}
			if(!success){
				System.err.println(new StringBuilder(getPlatformPrefix()).append("Propagation initialization with ").append(hostAddress).append(" failed."));
				//in case of failed propagation delete platform ID entries
				propagatedNodeIDs.remove(sAddress.toString());
				checkDiscoveryState();
			}
		} else {
			if(!isDistributed){
				System.err.println(new StringBuilder(getPlatformPrefix()).append("Network propagation was requested although network support is not activated!"));
			}
			if(!isPropagating){
				System.err.println(new StringBuilder(getPlatformPrefix()).append("Network propagation was requested although network propagation of is not activated!"));
			}
		}
	}

	/**
	 * Awaits one remote connection before returning. Use awaitRemoteConnections()
	 * a distinct number of connections should be awaited.
	 */
	public static void awaitRemoteConnection(){
		awaitRemoteConnections(1);
	}
	
	/**
	 * Awaits a defined number of network connections before proceedings.
	 * This is especially useful in the context of distributed applications
	 * to await start until all nodes are connected.
	 * @param numberOfPlatforms - Integer number of platforms to wait for
	 */
	public static void awaitRemoteConnections(int numberOfPlatforms){
		
		if(isDistributed){
			
			if(!platformInitialized){
				initializePlatform();
			}
			
			int testValue = 100;
			boolean success = false;
			System.out.println(getPlatformPrefix() + "Awaiting connection and synchronization with " + numberOfPlatforms + " remote platform(s).");
			while(!success){
				try {
					Thread.sleep(testValue);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(propagatedNodes.size() >= numberOfPlatforms){
					int targetNo = 0;
					Iterator<String> it = propagatedNodes.keySet().iterator();
					//check that propagation has been mutual
					if(it.hasNext()){
						if(MTRuntime.getPropagatedNodes().get(it.next())){
							targetNo++;				
						}
					}
					if(targetNo >= numberOfPlatforms){
						success = true;
						System.out.println(getPlatformPrefix() + "Number of connected platforms: " + numberOfPlatforms + ".");
					}
				}
			}
		} else {
			System.out.println(getPlatformPrefix() + "Distributed mode needs to be activated in order to used network-related functionality.");
		}
	}
	
	/**
	 * Notifies a platform on remote address to remove all entries for this platform
	 * in order to provide clean disconnection.
	 * @param nodeAddress - address of remote platform
	 */
	public static void notifyRemotePlatformToDisconnect(String nodeAddress, int port){
		System.out.println(getPlatformPrefix() + "Notifying remote platform " + nodeAddress + " to disconnect.");
		Message msg = new Message();
		msg.put(remotePlatformShutdownKeyword, platformName);
		sendRemote(nodeAddress, platformProcess, port, msg);
	}
	
	public static void setTcpPort(int port){
		if(allowDynamicPortSelection){
			NETTY_TCP_PORT = port;
		} else {
			System.out.println(getPlatformPrefix() + "Change of network only allowed if dynamic port selection is activated.");
		}
	}
	
	
	/** inbound propagation management (agents on other nodes) */

	public synchronized static void addPropagatedRemoteProcess(String process, String node) {
		//System.out.println("Added remote process " + process + " on node " + node + " to local database.");
		if(propagatedRemoteProcessMap.containsKey(process)){
			propagatedRemoteProcessMap.get(process).add(node);
		} else {
			ArrayList<String> tempList = new ArrayList<String>();
			tempList.add(node);
			propagatedRemoteProcessMap.put(process, tempList);
		}
	}
	
	public synchronized static void removePropagatedRemoteProcess(String process, String node){
		//System.out.println("Deleted remote process " + process + " on node " + node + " from local database.");
		if(propagatedRemoteProcessMap.containsKey(process)){
			if(propagatedRemoteProcessMap.get(process).size() == 1){
				propagatedRemoteProcessMap.remove(process);
			} else {
				ArrayList<String> tempList = propagatedRemoteProcessMap.get(process);
				tempList.remove(node);
				//propagatedRemoteProcessMap.put(process, tempList);
			}
		}
	}
	
	public synchronized static void removeAllPropagatedRemoteProcessesOfNode(String node){
		Iterator<String> it = propagatedRemoteProcessMap.keySet().iterator();
		//System.out.println("Node to be removed from " + node);
		String key;
		int count = 0;
		while(it.hasNext()){
			key = it.next();
			if(propagatedRemoteProcessMap.get(key).contains(node)){
				ArrayList<String> tempList = propagatedRemoteProcessMap.get(key);
				if(tempList.size() == 1){
					propagatedRemoteProcessMap.remove(key);
					it = propagatedRemoteProcessMap.keySet().iterator();
				} else {
					tempList.remove(node);
					//propagatedRemoteProcessMap.put(key, tempList);
				}
				count++;
			}
		}
		System.out.println(getPlatformPrefix() + count + " number of remote processes removed from database.");
	}

	public static ConcurrentHashMap<String, Boolean> getPropagatedNodes() {
		return propagatedNodes;
	}
	
	public static ConcurrentHashMap<String, String> getPropagatedNodeIDs(){
		return propagatedNodeIDs;
	}
	
	/**
	 * adds the platform ID for a propagating remote node before initial
	 * exchange of agent directories. Uses SocketAddress.toString() 
	 * @param node
	 */
	public static void addPropagatedNodeID(String node, String platformId){
		propagatedNodeIDs.put(node, platformId);
	}

	public static void addPropagatedNode(String node, boolean receivedPropagation) {
		propagatedNodes.put(node, receivedPropagation);
		checkDiscoveryState();
	}

	/**
	 * Adds a node to the node name resolution list.
	 * @param nodeName - node name
	 * @param hostAddress - node's ip address
	 */
	public static void addToNodeNameTable(String nodeName, String hostAddress){
		nodeNameTable.put(nodeName, hostAddress);
	}
	
	public static void removeFromNodeNameTable(String hostAddress){
		if(nodeNameTable.containsValue(hostAddress)){
			Iterator<String> it = nodeNameTable.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				if(nodeNameTable.get(key).equals(hostAddress)){
					nodeNameTable.remove(key);
					it = nodeNameTable.keySet().iterator();
				}
			}
		}
	}
	
	public static ConcurrentHashMap<String, String> getNodeNameTable(){
		return nodeNameTable;
	}
	
	public static void purgeRemoteNodeEntries(String nodeAddress) {
		if(debug){
			System.out.println("Agent register cache entries BEFORE removal of entries for platform " + nodeAddress);
			System.out.println("Propagated nodes: " + propagatedNodes);
			System.out.println("Propagated processes: " + propagatedRemoteProcessMap);
		}
		propagatedNodes.remove(nodeAddress);
		removeAllPropagatedRemoteProcessesOfNode(nodeAddress);
		removeFromNodeNameTable(SocketAddress.inflate(nodeAddress).getHostname());
		if(debug){
			System.out.println("Agent register cache entries AFTER removal of entries for platform " + nodeAddress);
			System.out.println("Propagated nodes: " + propagatedNodes);
			System.out.println("Propagated processes: " + propagatedRemoteProcessMap);
		}
		//remove from ID register finally to allow rediscovery
		propagatedNodeIDs.remove(nodeAddress);
		//automatically switch on discovery if it has been shut down after platform sync.
		checkDiscoveryState();
	}
	
	
	/** outbound propagation management (my agents propagated on other nodes) */
	
	public static void propagateProcessAdditionToNode(String processName, String node){
		Message processToPropagate = new Message();
		processToPropagate.put(processName, processAdditionKeyword);
		sendRemote(SocketAddress.inflate(node).getHostAddress(), platformProcess, SocketAddress.inflate(node).getPort(), processToPropagate);
	}
	
	public static void propagateProcessRemovalToNode(String processName, String node){
		Message processToPropagate = new Message();
		processToPropagate.put(processName, processRemovalKeyword);
		sendRemote(SocketAddress.inflate(node).getHostAddress(), platformProcess, SocketAddress.inflate(node).getPort(), processToPropagate);
	}
	
	public static void propagateProcessesToNode(SocketAddress address) {
		Iterator<String> it = registeredMicroFibersMap.keySet().iterator();
		Message localProcessesToPropagate = new Message();
		while(it.hasNext()){
			localProcessesToPropagate.put(it.next(), processAdditionKeyword);
		}
		//send platform ID in case the remote platform connection is not done via network discovery
		localProcessesToPropagate.put(platformID, platformIdKeyword);
		//System.out.println("Message to be sent: " + localProcessesToPropagate.toString());
		sendRemote(address.getHostAddress(), platformProcess, address.getPort(), localProcessesToPropagate);
		addPropagatedNode(address.toString(), false);
	}
	
	public static ConcurrentHashMap<String, ArrayList<String>> getRemoteProcessMap(){
		return propagatedRemoteProcessMap;
	}

	public static String getPlatformPrefix(){
		//check for prefixing with nanotime
		return (prefixPlatformOutputWithTime && prefixPlatformOutputWithNanoTime ? new StringBuilder(String.valueOf(System.nanoTime())).append(" - ")
			//else check on normal time format
			: prefixPlatformOutputWithTime ? new StringBuilder(MTConnector.getCurrentTimeString(prefixPlatformOutputWithDateTime)).append(" - ") : "") + platformPrefix;
	}
	
	public static String getInternalMessagePassingFramework(){
		return internalMessageFramework;
	}
	
	public static String getSerializationType(){
		return serialization.toString();
	}
	
	public static boolean isClojureActivated(){
		return ClojureActivated;
	}
	
	public static ConcurrentHashMap<String, AbstractMicroFiber> getRegisteredAgents(){
		return registeredMicroFibersMap;
	}
	
	/**
	 * Sets prefix for agents instantiated without explicitly specified name.
	 * @param namePrefix
	 */
	public static void setAnonymousAgentNamePrefix(String namePrefix){
		anonymousAgentPrefix = namePrefix;
	}

	/**
	 * Returns the next anonymous name from the platform
	 * @return - String as agent name
	 */
	public synchronized static String getNextAnonymousAgentName(){
		return anonymousAgentPrefix + getNextSuffixForAgentName();
	}
	
	/**
	 * Returns the next available integer ID which can be used
	 * as a suffix for agent names to ensure unique ids.
	 * @return - String value of the id
	 */
	public synchronized static String getNextSuffixForAgentName(){
		anonymousAgentNameCounter++;
		if(anonymousAgentNameCounter.toString().length() < suffixLength){
			StringBuffer input = new StringBuffer();
			int repetition = suffixLength-anonymousAgentNameCounter.toString().length();
			for(int i=0; i<repetition; i++){
				input.append("0");
			}
			return input.append(anonymousAgentNameCounter).toString();
		}
		return anonymousAgentNameCounter.toString();
	}
	
	/**
	 * Returns platform output level (see {@link PlatformOutputLevels}).
	 * @return
	 */
	public static Integer getPlatformOutputLevels(){
		return platformOutputLevel;
	}
	
	/**
	 * Returns agents' console output level (see {@link AgentConsoleOutputLevels}).
	 * @return
	 */
	public static Integer getAgentConsoleOutputLevel(){
		return agentConsoleOutputLevel;
	}
	
	/**
	 * returns the number of agents registered to Message Transport
	 */
	public static int getNumberOfAgents(){
		return registeredMicroFibersMap.size();
	}
	
	public static MemoryChannel<MicroMessage> getAgentChannel(String agentName){
		return registeredJetlangChannelsMap.get(agentName);
	}
	
	public static MemoryChannel<MicroMessage> getCommonChannel(){
		return broadCastChannel;
	}
	
	public static MicroMessageValidator getMicroMessageValidator(){
		if(validator == null){
			validator = new DefaultMicroMessageValidator();
		}
		return validator;
	}
	
	public static boolean isPropagating(){
		return isPropagating;
	}
	
	public static boolean isDiscoveryActivated(){
		return discoveryActivated;
	}
	
	public static boolean discoveryAutostart(){
		return discoveryStart;
	}
	
	public static String getMulticastAddress(){
		return multicastGroup;	
	}
	
	public static int getMulticastPort(){
		return multicastPort;
	}
	
	public static int getDiscoveryFrequency(){
		return discoveryFrequency;
	}
	
	public static boolean isDistributed(){
		return isDistributed;
	}
	
	public static Integer getTcpPort(){
		return NETTY_TCP_PORT;
	}
	
	public static boolean dynamicSelectionOfTcpPort(){
		return allowDynamicPortSelection;
	}
	
	public static String getDiscoveryMode(){
		return discoveryMode;
	}
	
	public static int getBroadcastPort(){
		return broadcastPort;
	}
	
	public static boolean isHeartbeatActivated(){
		return heartbeatActivated;
	}
	
	public static int getHeartbeatFrequency(){
		return heartbeatFrequency;
	}
	
	public static int getHeartbeatTimeoutFactor(){
		return heartbeatTimeoutFactor;
	}
	
	/**
	 * Returns the uptime of the platform in ms.
	 * @return
	 */
	public static long getUptime(){
		return (System.currentTimeMillis()-startTime);
	}
	
	/**
	 * Starts random number generator.
	 */
	protected static void startRandomNoGenerator(){
		if(random == null){
			random = new MersenneTwister(seed);
		}
	}
	
	/**
	 * Sets user-defined seed for the random number generator.
	 * Should be called before any call randomNoGenerator-related method 
	 * (such as sendRandomcast() or getRandomNoGenerator()) to ensure 
	 * reproducible behaviour. However, the seed can be updated at any time 
	 * and will take effect immediately.
	 * @param seed Seed for random number generator
	 */
	public static void setRandomNumberGeneratorSeed(long seed){
		MTRuntime.seed = seed;
		System.out.println(new StringBuffer(getPlatformPrefix())
			.append("Set random number generator seed to ").append(seed));
		if(random != null){
			System.out.println(new StringBuffer(getPlatformPrefix())
				.append("Reinitializing running random number generator with updated seed."));
			random.setSeed(seed);
		}
	}
	
	/**
	 * Returns the random number generator seed set for this platform.
	 * Only to be called once platform is initialized.
	 * @return
	 */
	public static Long getRandomNumberGeneratorSeed(){
		if(!platformInitialized){
			throw new RuntimeException(platformPrefix + "Random number generator seed should only be requested once platform is started.");
		}
		return MTRuntime.seed;
	}
	
	/**
	 * Returns a reference to random number generator instance for arbitrary use.
	 * @return
	 */
	public static MersenneTwister getRandomNoGenerator(){
		startRandomNoGenerator();
		return random;
	}
	
	/**
	 * Returns a unique runtime ID for the platform. Unique for every instance
	 * and changing upon every platform (re)initialization.
	 * @return
	 */
	@Inspect
	public static String getPlatformID(){
		return platformID;
	}
	
	/**
	 * Indicates if platform is initialized (i.e. all configuration settings applied).
	 * @return
	 */
	public static boolean platformInitialized(){
		return platformInitialized;
	}
	
	/**
	 * Indicates if message passing occurs in synchronous (i.e. blocking) or 
	 * asynchronous fashion.
	 * @return
	 */
	public static boolean isSynchronousOperationMode(){
		return synchronousOperationMode;
	}
	
	/**
	 * Sets location of local platform.
	 * @param location
	 */
	public static void setLocation(Location location){
		MTRuntime.location = location;
		MTRuntime.send(new MicroMessage(new RemotePlatformLocationEvent(MTRuntime.platformProcess, MTRuntime.platformID, MTRuntime.location)));
	}
	
	/**
	 * Returns user-specified location of local platform.
	 * @return
	 */
	public static Location getLocation(){
		return MTRuntime.location;
	}
	
	/**
	 * Returns the operating system the platform is running on.
	 * @return
	 */
	@Inspect
	public static String getOperatingSystem(){
		return operatingSystem;
	}
	
	/**
	 * returns the agent for a given name.
	 * @param agentName
	 * @return AbstractAgent is returned
	 */
	public static AbstractAgent getAgentForName(String agentName){
		if(MTRuntime.getRegisteredAgent(agentName) != null){
			return (AbstractAgent) (MTRuntime.getRegisteredAgent(agentName)).getAgent();
		} else return null;
	}
	
	/** indicates if selective printing of agent logs is activated */
	protected static boolean selectivePrintingOfCollectedAgentLogsActivated = false;
	/** contains agents for whom printing of logs is activated */
	protected static ArrayList<String> agentsCollectedLogsToBePrinted = new ArrayList<String>();
	
	/**
	 * Adds a single agent for selective printing of log data.
	 * @param agentname
	 */
	public static void addAgentForSelectivePrintingOfCollectedLogs(String agentname){
		if(!agentsCollectedLogsToBePrinted.contains(agentname)){
			agentsCollectedLogsToBePrinted.add(agentname);
			if(selectivePrintingOfCollectedAgentLogsActivated){
				getAgentForName(agentname).collectAndPrint = true;
			}
		}
	}
	
	/**
	 * Adds agents for selective printing of log data.
	 * @param agentLogsToBePrinted - ArrayList containing agent names
	 */
	public static void addAgentsForSelectivePrintingOfCollectedLogs(ArrayList<String> agentLogsToBePrinted){
		for(int i=0; i<agentLogsToBePrinted.size(); i++){
			if(!agentsCollectedLogsToBePrinted.contains(agentLogsToBePrinted.get(i))){
				agentsCollectedLogsToBePrinted.add(agentLogsToBePrinted.get(i));
				if(selectivePrintingOfCollectedAgentLogsActivated){
					getAgentForName(agentLogsToBePrinted.get(i)).collectAndPrint = true;
				}
			}
		}
	}
	
	/** returns ArrayList of agents which do printing of collected logs */
	public static ArrayList<String> getCollectedLogPrintingAgents(){
		return agentsCollectedLogsToBePrinted;
	}
	
	/** 
	 * Removes agent from selective printing of logs.
	 * @param agentName
	 */
	public static void removeAgentFromCollectedLogPrinting(String agentName){
		agentsCollectedLogsToBePrinted.remove(agentName);
	}
	
	/**
	 * Clears the list of agents for selective printing of collected log data.
	 */
	public static void clearCollectedLogPrintingAgents(){
		System.out.println(new StringBuffer(getPlatformPrefix()).append("Register for selective printing of collected agent logs cleared."));
		agentsCollectedLogsToBePrinted.clear();
	}
	
	/**
	 * Indicates if selective printing of logs is activated.
	 * @return
	 */
	public static boolean selectivePrintingOfCollectedLogsActivated(){
		return selectivePrintingOfCollectedAgentLogsActivated;
	}
	
	/**
	 * Activates selective printing of logs.
	 */
	public static void activateSelectivePrintingOfCollectedAgentLogs(){
		for(int i=0; i<agentsCollectedLogsToBePrinted.size(); i++){
			activateSelectivePrintingForAgent(agentsCollectedLogsToBePrinted.get(i), true);
		}
		selectivePrintingOfCollectedAgentLogsActivated = true;
		System.out.println(new StringBuffer(getPlatformPrefix()).append("Selective printing of collected agent logs activated."));
	}
	
	/**
	 * De/Activates selected printing of logs for given agent
	 * @param agentname Agent whose setting are to be modified
	 * @param activate Activation or deactivation of collecting and printing of logs
	 */
	public static void activateSelectivePrintingForAgent(String agentname, boolean activate){
		AbstractAgent agent = getAgentForName(agentname);
		if(agent != null){
			if(activate){
				agent.collectAndPrint = true;
			} else {
				agent.collectAndPrint = false;
			}
		}
	}
	
	/**
	 * Deactivates selective printing of logs.
	 */
	public static void deactivateSelectivePrintingOfCollectedAgentLogs(){
		if(selectivePrintingOfCollectedAgentLogsActivated){
			for(int i=0; i<agentsCollectedLogsToBePrinted.size(); i++){
				getAgentForName(agentsCollectedLogsToBePrinted.get(i)).collectAndPrint = false;
			}
		}
		selectivePrintingOfCollectedAgentLogsActivated = false;
		System.out.println(new StringBuffer(getPlatformPrefix()).append("Selective printing of collected agent logs deactivated."));
	}
	
	/**
	 * Returns the console output level of platform.
	 * See @link OutputLevels for output levels. 
	 * @return
	 */
	public static int getOutputLevel(){
		return platformOutputLevel;
	}
	
}
