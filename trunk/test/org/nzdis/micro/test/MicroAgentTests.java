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
package org.nzdis.micro.test;

import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nzdis.micro.AbstractAgent;
import org.nzdis.micro.Agent;
import org.nzdis.micro.AgentController;
import org.nzdis.micro.AnonymousAgent;
import org.nzdis.micro.ClojureConnector;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.PlatformController;
import org.nzdis.micro.Role;
import org.nzdis.micro.SystemAgentLoader;
import org.nzdis.micro.SystemOwner;
import org.nzdis.micro.bootloader.MicroBootProperties;
import org.nzdis.micro.constants.MessagePassingFrameworks;
import org.nzdis.micro.exceptions.InvalidDisposalOfRoleOnLivingAgent;
import org.nzdis.micro.exceptions.RegisteredOwnerInOwnGroupException;
import org.nzdis.micro.exceptions.RoleNotInitializedException;
import org.nzdis.micro.test.gip.GenericIntentClientRole;


public class MicroAgentTests {

	@Before
	public void runBeforeEveryTest(){
		MicroBootProperties.activateNetworkSupport(false);
		MicroBootProperties.activateClojureSupport(true);
		MicroBootProperties.setAgentConsoleOutputLevel(2);
		ClojureConnector.setCommonScriptPath("cljTestScripts/");
		//ClojureConnector.setIndividualScriptPath("cljScripts/");
		ClojureConnector.setRoleScriptPath("cljTestScripts/");
		MTConnector.setRandomNumberGeneratorSeed(System.currentTimeMillis());
	}
	
	@After
	public void runAfterEveryTest(){
		MTConnector.printApplicableIntents();
		MTConnector.printEventSubscriptions();
		PlatformController.shutdownPlatform();
		MTConnector.printApplicableIntents();
		MTConnector.printEventSubscriptions();
		assertTrue(MTConnector.shutdownCheck());
	}

	@Test
	public void platformStartAndShutdownMicroFiber(){
		MicroBootProperties.setInternalMessageTransport(MessagePassingFrameworks.MICRO_FIBER);
		PlatformController.startPlatform();
		assertTrue(MTConnector.getInternalMessagePassingFramework().equals("MICRO_FIBER"));
	}
	
	@Test
	public void platformStartAndShutdownJetlang(){
		MicroBootProperties.setInternalMessageTransport(MessagePassingFrameworks.JETLANG);
		PlatformController.startPlatform();
		assertTrue(MTConnector.getInternalMessagePassingFramework().equals("JETLANG"));
	}
	
	@Test(timeout = 8000)
	public void simpleSocialReactiveAdder(){
		System.out.println("TEST ===== mixed social/reactive addition test");
		
		CalcClient client = new CalcClient();
		System.out.println(client.toString());
		SystemAgentLoader.newAgent(client);
		SystemAgentLoader.newAgent(new StorageCalcRole(), "CalcProvider");
		SystemAgentLoader.newAgent(new ReactiveCalcRole(), "Calculator");
		CalcIntent sg = new CalcIntent();
		sg.storeLeftData(38502);
		sg.storeRightData(24200);
		sg.setOperation("ADD");
		client.start(sg);
		while(!client.resultsReceived()){};
		Integer result = Integer.parseInt(client.getResults().toString());
		assertTrue(result == 62702);
	}
	
	@Test(expected=RegisteredOwnerInOwnGroupException.class)
	public void reregisterAgentWithNewGroup(){
		System.out.println("TEST ===== reregistration of agent to other group during runtimes, check that owner agent cannot register itself to its own group.");
		
		CalcClient superAgent1 = new CalcClient();
		CalcClient superAgent2 = new CalcClient();
		SystemAgentLoader.newAgent(superAgent1, "SuperAgent1");
		SystemAgentLoader.newAgent(superAgent2, "SuperAgent2");
		CalcClient client = new CalcClient();
		superAgent1.getAgent().getGroup().getAgentLoader().newAgent(client, "Client");
		assertTrue(superAgent1.getAgent().getGroup().getAgents()[0].equals(client.getAgent()));
		assertTrue(superAgent2.getAgent().getGroup().getAgents().length == 0);
		superAgent2.getAgent().getGroup().reregister(client.getAgent());
		assertTrue(superAgent2.getAgent().getGroup().getAgents()[0].equals(client.getAgent()));
		assertTrue(superAgent1.getAgent().getGroup().getAgents().length == 0);
		
		//ensure that owning agent cannot register to own group - Exception!
		superAgent1.getAgent().getGroup().reregister(superAgent1.getAgent());
	}
	
	@Test(timeout = 1000)
	public void hierarchySocialReactiveAdder(){
		System.out.println("TEST ===== mixed social/reactive cross-hierarchy addition test");
		
		//ensures that no agent is active in Micro-agent hierarchy upon start
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
		CalcClient client = new CalcClient();
		SystemAgentLoader.newAgent(client);
		SystemAgentLoader.newAgent(new StorageCalcRole(), "CalcProvider");
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().getGroup().getAgentLoader().newAgent(new ReactiveCalcRole(), "Calculator");
		CalcIntent sg = new CalcIntent();
		sg.storeLeftData(38502);
		sg.storeRightData(24200);
		sg.setOperation("ADD");
		client.start(sg);
		while(!client.resultsReceived()){};
		Integer result = Integer.parseInt(client.getResults().toString());
		assertTrue(result == 62702);
	}
	
	@Test(expected=InvalidDisposalOfRoleOnLivingAgent.class)
	public void changeApplicableIntentsAndDisposeRolesAtRuntime(){
		System.out.println("TEST ===== Change of applicable intents for roles at runtime, disposal of roles");
		
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
		ReactiveCalcRole role = new ReactiveCalcRole();
		SystemAgentLoader.newAgent(role, "Calculator");
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRoles().length == 1);
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRole(role.getRoleName()).getApplicableIntentTypes().length == 2);
		//remove applicable intent
		role.removeApplicableIntent(AdderIntent.class);
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRole(role.getRoleName()).getApplicableIntentTypes().length == 1);
		//add intent directly to role
		SystemAgentLoader.findAgent("Calculator").getRole(role.getRoleName()).addApplicableIntent(CalcIntent.class);
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRole(role.getRoleName()).getApplicableIntentTypes().length == 2);
		SystemAgentLoader.findAgent("Calculator").addRole(new StorageCalcRole());
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRoles().length == 2);
		SystemAgentLoader.findAgent("Calculator").disposeRole(role);
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRoles().length == 1);
		//will throw exception as it is last role on agent
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].dispose();
		assertTrue(SystemAgentLoader.findAgent("Calculator").getRoles().length == 1);
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().die();
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
	}
	
	@Test(timeout = 1000)
	public void directAccessToReactiveAgent(){
		System.out.println("TEST ===== Direct access to reactive Micro-agent, including sub-agents as well as different lookup mechanisms");
		
		String AGENT_NAME = "Calculator";
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
		SystemAgentLoader.newAgent(new ReactiveCalcRole(), AGENT_NAME);
		int result = ((ReactiveCalcRole)SystemAgentLoader.findRoles(ReactiveCalcRole.class)[0]).add(7582, 8457);
		assertTrue(result == 16039);
		
		//lookup up role by intent
		assertTrue("Looking up role by intent", ((ReactiveCalcRole)SystemAgentLoader.findRolesByIntent(AdderIntent.class)[0]).add(5, 6) == 11);
		
		//lookup of agent by intent
		Agent[] ag = SystemAgentLoader.findAgentsByIntent(AdderIntent.class);
		assertTrue("Looking up agent by intent", ag.length == 1);
		
		//lookup of agent by role
		Agent[] ag3 = SystemAgentLoader.findAgentsByRole(ReactiveCalcRole.class);
		assertTrue("Looking up agent by role", ag3.length == 1);
		
		//lookup non-existing agent by role
		ag3 = SystemAgentLoader.findAgentsByRole(StorageCalcRole.class);
		assertTrue("Looking up agent by non-assigned role", ag3.length == 0);
		
		//lookup of agent names by role
		String[] agName = SystemAgentLoader.findAgentNamesByRole(ReactiveCalcRole.class);
		assertTrue("Looking up agent name by role", agName[0].equals(AGENT_NAME));
		
		Agent[] ag2 = ag[0].getGroup().findAgentsByIntentNonRecursive(AdderIntent.class, false);
		assertTrue("Find agents by intent in non-recursive manner", ag2.length == 0);
		
		ag[0].getGroup().getAgentLoader().newAgent(new ReactiveCalcRole2(), "SubCalcRole");
		//search for agents after loading one sub-agent
		ag2 = ag[0].getGroup().findAgentsByIntentNonRecursive(AdderIntent.class, false);
		assertTrue("Find agents by intent in non-recursive manner", ag2.length == 1);
		
		SystemAgentLoader.findAgent("SubCalcRole").getGroup().getAgentLoader().newAgent(new ReactiveCalcRole2(), "Another sub");
		//search for agents after loading another sub-agent, now three levels of application agents (without SystemOwner)
		ag2 = ag[0].getGroup().findAgentsByIntentNonRecursive(AdderIntent.class, false);
		//non-recursive should still be 1
		assertTrue(ag2.length == 1);
		//recursive should result in 2
		ag2 = ag[0].getGroup().findAgentsByIntent(AdderIntent.class, false);
		assertTrue(ag2.length == 2);
		//now include group owner in search for intents as well --> should result in three
		ag2 = ag[0].getGroup().findAgentsByIntent(AdderIntent.class, true);
		assertTrue(ag2.length == 3);
		
		
		
		
		//find roles by role type
		//load another subagent of super-agents role type
		ag[0].getGroup().getAgentLoader().newAgent(new ReactiveCalcRole(), "3rdSubCalcRole");
		//should only find one agent playing role
		Role[] rl = ag[0].getGroup().findRolesByType(ReactiveCalcRole.class, false);
		assertTrue(rl.length == 1);
		//including group owner should now find two agents
		rl = ag[0].getGroup().findRolesByType(ReactiveCalcRole.class, true);
		assertTrue(rl.length == 2);
		//same result for this (only in owner's group + owner itself)
		rl = ag[0].getGroup().findRolesByTypeNonRecursive(ReactiveCalcRole.class, true);
		assertTrue(rl.length == 2);
		
		//find roles by intent type
		rl = ag[0].getGroup().findRolesByIntent(AdderIntent.class, false);
		assertTrue(rl.length == 3);
		//with owner that should be four
		rl = ag[0].getGroup().findRolesByIntent(AdderIntent.class, true);
		assertTrue(rl.length == 4);
		//without lowest layer and owner that should reduce to two
		rl = ag[0].getGroup().findRolesByIntentNonRecursive(AdderIntent.class, false);
		assertTrue(rl.length == 2);
		
		assertTrue("Checking on group with agent", ag[0].hasGroup());
		
		assertTrue("Checking on initialization of group", ag[0].getGroup().isInitialized());
		//check on number of subagents (should be two)
		assertTrue("Checking for two sub-agents", ag[0].getGroup().getAgents().length == 2);
		
		//search on highest level should reveal three agents
		ag = SystemAgentLoader.findAgentsByIntent(AdderIntent.class);
		assertTrue(ag.length == 3);
	}

	@Test(timeout = 1000)
	public void hierarchicalReassignmentOnDeath(){
		System.out.println("TEST ===== Micro-Agent hierarchy handling");
		
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
		SystemAgentLoader.newAgent(new StorageCalcRole(), "Storage");
		SystemAgentLoader.newAgent(new CalcClient(), "CalcClient");
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().getGroup().getAgentLoader().newAgent(new CalcClient(), "CalcClient2");
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().getGroup().getAgentLoader().newAgent(new CalcClient(), "CalcClient3");
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().killSubHierarchyUponDeath(false);
		int noOfStorageCalcAgentsBefore = SystemAgentLoader.findRoles(StorageCalcRole.class).length;
		int noOfCalcAgentsBefore = SystemAgentLoader.findRoles(CalcClient.class).length;
		SystemAgentLoader.findRoles(StorageCalcRole.class)[0].getAgent().die();
		int noOfStorageCalcAgentsAfter = SystemAgentLoader.findRoles(StorageCalcRole.class).length;
		int noOfCalcAgentsAfter = SystemAgentLoader.findRoles(CalcClient.class).length;
		System.out.println("Before: " + noOfStorageCalcAgentsBefore);
		System.out.println("After: " + noOfStorageCalcAgentsAfter);
		System.out.println("Before: " + noOfCalcAgentsBefore);
		System.out.println("After: " + noOfCalcAgentsAfter);
		assertEquals(1, noOfStorageCalcAgentsBefore);
		assertEquals(0, noOfStorageCalcAgentsAfter);
		assertEquals(3, noOfCalcAgentsBefore);
		assertEquals(3, noOfCalcAgentsAfter);
	}
	
	@Test(timeout = 8000)
	public void simpleCljAgentIntentInteraction(){
		System.out.println("TEST ===== social Micro-Agent Clojure interaction");
		
		assertTrue(SystemOwner.getInstance().getGroup().getAgents().length == 0);
		SystemAgentLoader.newAgent(new CljClient(), "CLJClient");
		SystemAgentLoader.newAgent(new ClojureSocialRole("ClojureSocialAgent.clj"), "CljSocialAgent");
		
		//agent interaction using Intents and Clojure
		CljExecutionIntent intent = new CljExecutionIntent();
		intent.setSExpression("(+ 65638 5464)");
		CljClient client = ((CljClient)SystemAgentLoader.findRoles(CljClient.class)[0]);
		client.start(intent);
		while(!client.resultArrived()){};
		assertEquals(71102, client.getResult());
		client.reset();
		System.out.println("==== 1st round finished =====");
		
		//different send function in Clojure
		intent.setSExpression("(* 128 2)");
		client.start(intent);
		while(!client.resultArrived()){};
		assertEquals(256, client.getResult());
		System.out.println("==== 2nd round finished =====");
		
		//simple test trying the real-time Clojure code execution of agents
		assertEquals(9962, ((AbstractAgent)client.getAgent()).execClj(new StringBuffer("(* 34 293)")));
		System.out.println("==== 3rd round finished =====");	
	}
	
	@Test
	public void eventSubscriptionTest(){
		System.out.println("TEST ===== Event subscription mechanism");
		
		SocialEventClient client1 = new SocialEventClient();
		SystemAgentLoader.newAgent(client1, "Client1");
		SocialEventClient client2 = new SocialEventClient();
		SystemAgentLoader.newAgent(client2, "Client2");
		SocialEventClient client3 = new SocialEventClient();
		SystemAgentLoader.newAgent(client3, "Client3");
		SocialEventClient client4 = new SocialEventClient();
		SystemAgentLoader.newAgent(client4, "Client4");
		SocialEventClient client5 = new SocialEventClient();
		SystemAgentLoader.newAgent(client5, "Client5");
		
		SocialEventSource source = new SocialEventSource();
		SystemAgentLoader.newAgent(source);
		
		client1.subscribe2();
		client3.subscribe2();
		
		assertTrue(client1.getSubscriptionStatus1());
		assertTrue(client2.getSubscriptionStatus1());
		assertTrue(client3.getSubscriptionStatus1());
		assertTrue(client4.getSubscriptionStatus1());
		assertTrue(client5.getSubscriptionStatus1());
		assertTrue(client1.getSubscriptionStatus2());
		assertFalse(client2.getSubscriptionStatus2());
		assertTrue(client3.getSubscriptionStatus2());
		assertFalse(client4.getSubscriptionStatus2());
		assertFalse(client5.getSubscriptionStatus2());
		
		
		//MTLoader.printEventSubscriptions();
		
		source.raiseEvent1();
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		assertTrue(client1.eventCalled1());
		assertTrue(client2.eventCalled1());
		assertTrue(client3.eventCalled1());
		assertTrue(client4.eventCalled1());
		assertTrue(client5.eventCalled1());
		assertFalse(client1.eventCalled2());
		assertFalse(client2.eventCalled2());
		assertFalse(client3.eventCalled2());
		assertFalse(client4.eventCalled2());
		assertFalse(client5.eventCalled2());
		assertEquals(5, source.getCounter1());
		
		client1.resetCalls();
		client2.resetCalls();
		client3.resetCalls();
		client4.resetCalls();
		client5.resetCalls();
		client3.unsubscribe1();
		client4.subscribe2();
		
		//MTLoader.printEventSubscriptions();
		
		source.raiseEvent2();
		start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		assertEquals(3, source.getCounter2());
		assertTrue(client1.eventCalled2());
		assertFalse(client2.eventCalled2());
		assertTrue(client3.eventCalled2());
		assertTrue(client4.eventCalled2());
		assertFalse(client5.eventCalled2());
		assertFalse(client1.eventCalled1());
		assertFalse(client2.eventCalled1());
		assertFalse(client3.eventCalled1());
		assertFalse(client4.eventCalled1());
		assertFalse(client5.eventCalled1());
		
		source.raiseEvent1();
		start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		assertTrue(client1.eventCalled1());
		assertTrue(client2.eventCalled1());
		assertFalse(client3.eventCalled1());
		assertTrue(client4.eventCalled1());
		assertTrue(client5.eventCalled1());

		client1.resetCalls();
		client2.resetCalls();
		client3.resetCalls();
		client4.resetCalls();
		client5.resetCalls();
		
		//subscribe twice - no double delivery but check on data structures
		client4.subscribe2();
		client3.unsubscribe2();
		//unsubscribe without being subscribed
		client5.unsubscribe2();
		
		//MTLoader.printEventSubscriptions();
		
		source.raiseEvent2();
		start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		assertEquals(2, source.getCounter2());
		assertTrue(client1.eventCalled2());
		assertFalse(client2.eventCalled2());
		assertFalse(client3.eventCalled2());
		assertTrue(client4.eventCalled2());
		assertFalse(client5.eventCalled2());
		assertFalse(client1.eventCalled1());
		assertFalse(client2.eventCalled1());
		assertFalse(client3.eventCalled1());
		assertFalse(client4.eventCalled1());
		assertFalse(client5.eventCalled1());
	}
	
	@Test
	public void testRoleRetrieval(){
		System.out.println("TEST ===== Initialization of multiple roles in agent");
		
		SocialEventClient clientx = new SocialEventClient();
		SocialEventClient clienty = new SocialEventClient();
		CalcClient clientz = new CalcClient();
		ArrayList<Role> input = new ArrayList<Role>();
		input.add(clientx);
		input.add(clienty);
		input.add(clientz);
		Role[] roles = (Role[])input.toArray(new Role[input.size()]);
		
		AgentController ac = SystemAgentLoader.newAgent(roles, "MultiRoleAgent");
		
		assertEquals(3, ac.getAgent().getRoles().length);
		assertEquals(2, ac.getAgent().getRoles(SocialEventClient.class).length);
	}
	
	@Test
	public void prohibitedRoleTest(){
		System.out.println("TEST ===== Role prohibition mechanism");
		
		AbstractAgent agent = new AnonymousAgent("TestAgent");
		
		SocialEventClient clientx = new SocialEventClient();
		SocialEventClient clienty = new SocialEventClient();
		CalcClient calcx = new CalcClient();
		CalcClient calcy = new CalcClient();
		
		agent.addRole(calcx);
		agent.addRole(clientx);
		assertEquals(2, agent.getRoles().length);
		assertEquals(1, agent.getRoles(SocialEventClient.class).length);
		assertEquals(1, agent.getRoles(CalcClient.class).length);
		
		AgentController ac = new AgentController(agent);
		ac.prohibit(CalcClient.class);
		
		agent.addRole(clienty);
		agent.addRole(calcy);
		
		assertEquals(3, agent.getRoles().length);
		assertEquals(2, agent.getRoles(SocialEventClient.class).length);
		assertEquals(1, agent.getRoles(CalcClient.class).length);
		
		ac.permit(CalcClient.class);
		agent.addRole(calcy);
		
		assertEquals(4, agent.getRoles().length);
		assertEquals(2, agent.getRoles(SocialEventClient.class).length);
		assertEquals(2, agent.getRoles(CalcClient.class).length);
	}
	
	@Test
	public void goalDistributionTest(){
		System.out.println("TEST ===== Goal listing mechanism");
		
		StorageCalcRole storCalc = new StorageCalcRole();
		ClojureSocialAgent clojure = new ClojureSocialAgent();
		ReactiveCalcRole reacCalc = new ReactiveCalcRole();
		StorageCalcRole storCalc2 = new StorageCalcRole();
		
		ArrayList<Role> input = new ArrayList<Role>();
		input.add(storCalc);
		input.add(reacCalc);
		Role[] roles = (Role[])input.toArray(new Role[input.size()]);
		
		SystemAgentLoader.newAgent(roles, "CombinedAgent");
		
		Agent agent = SystemAgentLoader.findAgent("CombinedAgent");
		
		assertEquals(3, agent.getOwnerGroup().getApplicableIntentTypes().length);
		assertEquals(0, agent.getGroup().getApplicableIntentTypes().length);

		//adding new role with new goals
		agent.addRole(clojure);
		//is another instance but with same goals - should not increase applicable goal list
		agent.addRole(storCalc2);

		assertEquals(4, agent.getOwnerGroup().getApplicableIntentTypes().length);
		assertEquals(0, agent.getGroup().getApplicableIntentTypes().length);
	}
	
	@Test
	public void roleCastTest(){
		System.out.println("TEST ===== Role cast mechanism");
		
		RoleTestAgent role1 = new RoleTestAgent();
		RoleTestAgent role2 = new RoleTestAgent();
		RoleTestAgent role3 = new RoleTestAgent();
		RoleTest2Agent rolea = new RoleTest2Agent();
		RoleTest2Agent roleb = new RoleTest2Agent();
		RoleTestClient client = new RoleTestClient();
		
		SystemAgentLoader.newAgent(role1);
		SystemAgentLoader.newAgent(role2);
		SystemAgentLoader.newAgent(role3);
		SystemAgentLoader.newAgent(rolea);
		SystemAgentLoader.newAgent(roleb);
		SystemAgentLoader.newAgent(client);
		
		client.initRoleCast();
		
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		
		assertTrue(client.test1.equals(role1.result));
		assertTrue(client.test1.equals(role2.result));
		assertTrue(client.test1.equals(role3.result));
		assertFalse(client.test1.equals(rolea.result));
		assertFalse(client.test1.equals(roleb.result));	
	}

	@Test
	public void randomCastTest(){
		System.out.println("TEST ===== random cast mechanism");

		RoleTestAgent role1 = new RoleTestAgent();
		RoleTestAgent role2 = new RoleTestAgent();
		RoleTestAgent role3 = new RoleTestAgent();
		RoleTest2Agent rolea = new RoleTest2Agent();
		RoleTest2Agent roleb = new RoleTest2Agent();
		RoleTestClient client = new RoleTestClient();
		
		SystemAgentLoader.newAgent(role1);
		SystemAgentLoader.newAgent(role2);
		SystemAgentLoader.newAgent(role3);
		SystemAgentLoader.newAgent(rolea);
		SystemAgentLoader.newAgent(roleb);
		SystemAgentLoader.newAgent(client, "Client");
		
		client.initRandomCast();
		
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		
		int count = 0;
		if(client.test2.equals(role1.result)){
			count++;
		}
		if(client.test2.equals(role2.result)){
			count++;
		}
		if(client.test2.equals(role3.result)){
			count++;
		}
		if(client.test2.equals(rolea.result)){
			count++;
		}
		if(client.test2.equals(roleb.result)){
			count++;
		}
		
		assertTrue(count == 4);
	}
	
	@Test
	public void fuzzyCastTest(){
		System.out.println("TEST ===== fuzzy cast mechanism");

		String CLIENT_NAME = "Client";
		
		RoleTestAgent role1 = new RoleTestAgent();
		RoleTestAgent role2 = new RoleTestAgent();
		RoleTestAgent role3 = new RoleTestAgent();
		RoleTest2Agent rolea = new RoleTest2Agent();
		RoleTest2Agent roleb = new RoleTest2Agent();
		RoleTestClient client = new RoleTestClient();
		
		SystemAgentLoader.newAgent(role1);
		SystemAgentLoader.newAgent(role2);
		SystemAgentLoader.newAgent(role3);
		SystemAgentLoader.newAgent(rolea);
		SystemAgentLoader.newAgent(roleb);
		SystemAgentLoader.newAgent(client, CLIENT_NAME);
		
		ArrayList<String> excludedAgents = new ArrayList<String>();
		excludedAgents.add(CLIENT_NAME);
		
		client.initFuzzyCast(0.4f, excludedAgents);
		
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		
		int count = 0;
		if(client.test2.equals(role1.result)){
			count++;
		}
		if(client.test2.equals(role2.result)){
			count++;
		}
		if(client.test2.equals(role3.result)){
			count++;
		}
		if(client.test2.equals(rolea.result)){
			count++;
		}
		if(client.test2.equals(roleb.result)){
			count++;
		}
		
		assertTrue(count == 2);
		
		//Client must not have received data
		assertFalse(client.receivedData);
	}

	@Test
	public void groupCastTest(){
		System.out.println("TEST ===== group cast mechanism");
		
		RoleTestAgent role1 = new RoleTestAgent();
		RoleTestAgent role2 = new RoleTestAgent();
		RoleTestAgent role3 = new RoleTestAgent();
		RoleTest2Agent rolea = new RoleTest2Agent();
		RoleTest2Agent roleb = new RoleTest2Agent();
		RoleTest2Agent rolec = new RoleTest2Agent();
		RoleTestClient client = new RoleTestClient();
		
		SystemAgentLoader.newAgent(role1, "MotherAgent");
		SystemAgentLoader.newAgent(role2);
		SystemAgentLoader.newAgent(rolec);
		SystemAgentLoader.newAgent(client, "Client");
		
		role1.getAgent().getGroup().getAgentLoader().newAgent(role3);
		role1.getAgent().getGroup().getAgentLoader().newAgent(rolea);
		role1.getAgent().getGroup().getAgentLoader().newAgent(roleb);
		
		client.initGroupCast();
		
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()-start < 200){};
		
		//System.out.println("YXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" + client.test1);
		//System.out.println(role1.result);
		
		assertTrue(client.test1.equals(role3.result));
		assertTrue(client.test1.equals(rolea.result));
		assertTrue(client.test1.equals(roleb.result));
		assertFalse(client.test1.equals(role1.result));
		assertFalse(client.test1.equals(role2.result));
		assertFalse(client.test1.equals(rolec.result));	
	}

	@Test
	public void MessageFilterNotProperlyInitializedTest(){
		System.out.println("TEST ===== non-properly initialized message filter");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		boolean error = false;
		
		try{
			role1.addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertTrue(error);
	}

	@Test
	public void MessageFilterProperlyInitializedTest(){
		System.out.println("TEST ===== message filter properly initialized");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		assertTrue(role1.getMessageFilters().length == 1); 
		
		assertFalse(error);
	}
	
	@Test
	public void MessageFilterNoPassthroughTest(){
		System.out.println("TEST ===== message does not pass after being processed by message filter");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result == "");
	}
	
	@Test
	public void MessageFilterPassthroughTest(){
		System.out.println("TEST ===== message passes after being processed by message filter");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		
		((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).allowProcessingByNonMessageFilters(true);
		
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result.equals("TestResult"));
	}
	
	@Test
	public void MessageFilterNotMatchedTest(){
		System.out.println("TEST ===== message does not match message filter");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.startNonMatching();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result == "");
	}
	
	@Test
	public void MessageFilterLoadUnloadTest(){
		System.out.println("TEST ===== message filter load/unloading test");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result == "");
		
		assertTrue(role1.getAgent().hasMessageFilter());
		role1.getAgent().clearMessageFilters();
		assertFalse(role1.getAgent().hasMessageFilter());
	}
	
	@Test
	public void MultipleMessageFilterLoadUnloadTest(){
		System.out.println("TEST ===== multiple message filter load/unloading test");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MicroMessage pattern2 = new MicroMessage();
		pattern2.set(RoleTestAgent.resName, "NECESSARY");
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern2), "ResultMessageFilter2");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).matched);
		
		//allow pass through of messages by one filter (will not work - both need to commit)
		((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).allowProcessingByNonMessageFilters(true);
		
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result == "");
		
		assertTrue(role1.getAgent().getMessageFilters().length == 2);
		role1.getAgent().removeMessageFilter("ResultMessageFilter");
		assertTrue(role1.getAgent().getMessageFilters().length == 1);
		assertTrue(role1.getAgent().hasMessageFilter());
		role1.getAgent().clearMessageFilters();
		assertFalse(role1.getAgent().hasMessageFilter());
	}
	
	@Test
	public void MultipleMessageFilterLoadUnloadPassthroughTest(){
		System.out.println("TEST ===== multiple message filter load/unloading with message pass through test");
		
		RoleTestAgent role1 = new RoleTestAgent();
		
		MicroMessage pattern = new MicroMessage();
		pattern.set(RoleTestAgent.resName, null);
		
		SystemAgentLoader.newAgent(role1, "SuperAgent");
		boolean error = false;
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern), "ResultMessageFilter");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MicroMessage pattern2 = new MicroMessage();
		pattern2.set(RoleTestAgent.resName, "NECESSARY");
		
		try{
			role1.getAgent().addMessageFilter(new ResultMessageFilter(pattern2), "ResultMessageFilter2");
		} catch (RoleNotInitializedException e){
			error = true;
		}
		
		assertFalse(error);
		
		MessageFilterTestClient client = new MessageFilterTestClient();
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).matched);
		
		//make messages pass through to target after filtering
		((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).allowProcessingByNonMessageFilters(true);
		((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).allowProcessingByNonMessageFilters(true);
		
		SystemAgentLoader.newAgent(client, "MessageFilterTestClient");
		
		client.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter").getRoles(ResultMessageFilter.class)[0]).matched);
		assertFalse(((ResultMessageFilter)SystemAgentLoader.findAgent("ResultMessageFilter2").getRoles(ResultMessageFilter.class)[0]).matched);
		assertTrue(role1.result == "TestResult");
		
		assertTrue(role1.getAgent().getMessageFilters().length == 2);
		role1.getAgent().removeMessageFilter("ResultMessageFilter");
		assertTrue(role1.getAgent().getMessageFilters().length == 1);
		assertTrue(role1.getAgent().hasMessageFilter());
		role1.getAgent().clearMessageFilters();
		assertFalse(role1.getAgent().hasMessageFilter());
	}
	
	@Test
	public void GenericIntentProcessorTestPrimitiveIntent(){
		System.out.println("TEST ===== Generic Intent Processor with primitive intent execution");
		
		GenericIntentClientRole client = new GenericIntentClientRole();
		SystemAgentLoader.newAgent(client, "GenericIntentClient");
		assertTrue(client.simpleResult == 0);
		client.startSimpleIntent();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Simple result: " + client.simpleResult);
		assertTrue(client.simpleResult == 11);
	}
	
	@Test
	public void GenericIntentProcessorTestComplexIntent(){
		System.out.println("TEST ===== Generic Intent Processor with complex intent execution");
		
		GenericIntentClientRole client = new GenericIntentClientRole();
		SystemAgentLoader.newAgent(client, "GenericIntentClient");
		assertTrue(client.complexResult == 0.0);
		client.startComplexIntent();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Complex result: " + client.complexResult);
		assertTrue(client.complexResult == 187.9889834959149);
	}
	
	@Test
	public void GenericIntentProcessorSimpleInputAndResultIntent(){
		System.out.println("TEST ===== Generic Intent Processor with inputAndResult intent execution");
		
		GenericIntentClientRole client = new GenericIntentClientRole();
		SystemAgentLoader.newAgent(client, "GenericIntentClient");
		assertTrue(client.inputAndResultResult == 0);
		client.startInputAndResultIntent();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Input and result: " + client.inputAndResultResult);
		assertTrue(client.inputAndResultResult == 21);
	}
	
	/**
	 * open tests:
	 * - Network propagation
	 * - network discovery
	 * - network goal execution
	 * - (broadcast)
	 * - Config loader
	 * - message validator
	 * 
	 * done:
	 * - platform start/shutdown using Jetlang and MicroFiber
	 * - MessageFilter
	 * - GenericIntentProcessor
	 *
	 */

}
