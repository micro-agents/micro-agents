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
package org.nzdis.micro.messaging.network.netty;

import java.net.InetSocketAddress;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.events.LocalPlatformDelayedNetworkStartEvent;
import org.nzdis.micro.messaging.MTRuntime;

/**
 * Abstract Network Reader component of Netty implementation for micro-agent platform.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public abstract class AbstractReader {

	protected int port = NettyNetworkConnector.getInstance().getPort();
	
	protected boolean initialized = false;
	private boolean shutdownRequested = false;
	protected NioServerSocketChannelFactory socketChannelFactory = null;
	protected ChannelPipelineFactory pipelineFactory = null;
	private Channel boundChannel = null;
	protected ServerBootstrap bootstrap = null;
	/* counter for failed port binding attempts */
	public int initializationCounter = 0;
	/* maximum number of failed binding attempts */
	public int initializationThreshold = 60;
	/* indicator if delay event has been raised */
	private boolean delayEventRaised = false;
	
	public AbstractReader(){
		if(!initialized){
			initialize();
			//Server configuration
	        bootstrap = new ServerBootstrap(socketChannelFactory);
	        //Pipeline factory
	        bootstrap.setPipelineFactory(pipelineFactory);
	        //set options in implementation
	        bootstrap = setServerBootstrapOptions(bootstrap);
	        //bind port
	        while(boundChannel == null || !boundChannel.isBound()){
	        	//if no shutdown is requested try to bind the socket
	        	if(!shutdownRequested){
		        	try{
			        	boundChannel = bootstrap.bind(new InetSocketAddress(port));
			        } catch(Exception e){
			        	if(e.getClass().equals(ChannelException.class)){
			        		initializationCounter++;
			        		if(!delayEventRaised){
			        			MTConnector.send(new MicroMessage(new LocalPlatformDelayedNetworkStartEvent(MTConnector.platformProcess)));
			        			delayEventRaised = true;
			        		}
			        		if(MTRuntime.dynamicSelectionOfTcpPort()){
			        			int oldPort = port;
			        			//increment port by one
			        			MTRuntime.setTcpPort(oldPort + 1);
			        			//update local port reference
			        			port = NettyNetworkConnector.getInstance().getPort();
			        			
				        		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
				        			.append("Changed local port to ").append(port).append(" to avoid waiting for original port ").append(oldPort).toString());
			        		} else {
			        			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Network reader start delayed until port ").append(port).append(" is available for binding.").toString());
			        			try {
									Thread.sleep(3000);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
			        		}
							if(initializationCounter > initializationThreshold){
								System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Binding of port ").append(port).append(" for network reader failed.\nShutting down network ....").toString());
								NettyNetworkConnector.getInstance().shutdown();
								break;
							}
			        	}
			        }
	        	} else {
	        		//if shutdown is requested stop binding attempts
	        		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Socket binding for network reader aborted as of system shutdown.").toString());
	        		break;
	        	}
	        }
	        //if socket is bound and no shutdown is requested
	        if(boundChannel.isBound() && !shutdownRequested){
	        	initialized = true;
	        }
		}
	}
	
	public void shutdown(){
		shutdownRequested = true;
		
		//releasing port binding
		boundChannel.unbind();
		boundChannel.close();
		
		ChannelFuture closedFuture = boundChannel.getCloseFuture();
		closedFuture.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture arg0) throws Exception {
				// TODO Auto-generated method stub
				initialized = false;
			}
		});
		
		//Awaiting successful unbinding
		while(initialized){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Awaiting Unbinding of reader channel ...").toString());
			//bootstrap.releaseExternalResources();
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Releasing Network Reader socketChannel").toString());
		socketChannelFactory.releaseExternalResources();
		
		//shutdownRequested = false;
		//System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Releasing bootstrap");
		//bootstrap.releaseExternalResources();
	}
	
	protected abstract void initialize();
	protected abstract ServerBootstrap setServerBootstrapOptions(ServerBootstrap bootstrap);
	
}
