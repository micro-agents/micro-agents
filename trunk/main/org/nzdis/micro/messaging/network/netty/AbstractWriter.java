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
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.nzdis.micro.messaging.SocketAddress;
import org.nzdis.micro.messaging.message.Message;

/**
 * Abstract Network Writer component of Netty implementation for micro-agent platform.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public abstract class AbstractWriter extends Thread {

	protected boolean initialized = false;
	/* indicates if writer is running */
	private boolean running = true;
	/* indicator if thread is successfully shutdown */
	private boolean shutdown = true;
	protected NioClientSocketChannelFactory socketChannelFactory = null;
	protected ChannelPipelineFactory pipelineFactory = null;
	protected ClientBootstrap bootstrap = null;
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
	protected HashSet<Message> messageTrack = new HashSet<Message>();
	protected int sendFutureCounter = 0;
	protected int closeFutureCounter = 0;
	
	public AbstractWriter(){
		this.setName("NetworkWriter");
	}
	
	public void shutdown(){
		running = false;
		int maxWaitIterations = 20; 
		int waitIterations = 0;
		while(!shutdown || !messageQueue.isEmpty() || sendFutureCounter > 0){
			/*System.out.println("Message queue size " + messageQueue.size());
			System.out.println("Shutdown " + shutdown);
			System.out.println("Running " + running);
			System.out.println("SendFutureCounter " + sendFutureCounter);*/
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Awaiting processing of outstanding outbound messages before shutdown.").toString());
			//don't wait forever - connections might just be broken anyway already.
			if(waitIterations >= maxWaitIterations){
				System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Aborted processing of outstanding outbound messages.").toString());
				break;
			}
			waitIterations++;
		}
		
		//Destroy open connections
		Iterator<String> it = NettyNetworkConnector.getInstance().connections.keySet().iterator();
		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Number of open connections to be closed: ").append(NettyNetworkConnector.getInstance().connections.size()).toString());
		ChannelFuture channelFuture = null;
		while(it.hasNext()){
			channelFuture = NettyNetworkConnector.getInstance().connections.remove(ConnectionID.inflate(it.next())).getChannel().getCloseFuture();
			channelFuture.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture arg0) throws Exception {
					closeFutureCounter--;
				}
			});
			channelFuture.getChannel().close();
			closeFutureCounter++;
			//connections.get(it.next()).getChannel().close();
			it = NettyNetworkConnector.getInstance().connections.keySet().iterator();
		}
		while(closeFutureCounter != 0){
			System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Awaiting close of connections.").toString());
		}
		
		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Releasing Network Writer socketChannel").toString());
		if(socketChannelFactory != null){
			socketChannelFactory.releaseExternalResources();
		}
		initialized = false;
	}
	
	private synchronized void connect(SocketAddress sAddress, final ConnectionID id){
		//Client configuration
        bootstrap = new ClientBootstrap(socketChannelFactory);
        //Pipeline factory
        bootstrap.setPipelineFactory(pipelineFactory);
        //set options in implementation
        setClientBootstrapOptions(bootstrap);
        //finally connect
        ChannelFuture future = null;
        try{
        	future = bootstrap.connect(new InetSocketAddress(sAddress.getHostAddress(), sAddress.getPort())).await();
        } catch (Exception e){
        	e.printStackTrace();
        }
       
        future.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture arg0) throws Exception {
				if(arg0.getChannel().isConnected()){
					System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Channel " + arg0.getChannel() + " connected and saved to registry.").toString());
					//System.out.println("Local address: " + arg0.getChannel().getLocalAddress().toString());
					NettyNetworkConnector.getInstance().connections.put(id.getId(), arg0);
				}
			}
		});
        int count = 0;
        int threshold = 3;
        boolean success = false;
        while(count < threshold){
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(NettyNetworkConnector.getInstance().connections.containsKey(id.getId())){
				if(NettyNetworkConnector.getInstance().connections.get(id.getId()).getChannel().isConnected()){
					success = true;
					break;
				}
			}
        }
        
        if(!success){
        	System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Connection to ").append(sAddress.toString()).append(" failed.").toString());
        } else {
        	//will be used when channel initiated by me and closed remotely
        	future.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture arg0) throws Exception {
					System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Channel ").append(arg0.getChannel()).append(" closed and removed from registry.").toString());
					NettyNetworkConnector.getInstance().connections.remove(id);
				}
			});
        }
	}
	
	
	public void sendMessage(Message message, String host, Integer port){
		message.put(NettyNetworkConnector.TARGET_NODE_KEYWORD, host);
		message.put(NettyNetworkConnector.TARGET_PORT_KEYWORD, port);
		
		//System.err.println("Added message to queue: " + message);
		//as long as queue is running, put message
		if(running){
			//asynchronous message queue handling (especially helpful in case of interrupted connections.
			try {
				messageQueue.put(message);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println(NettyNetworkConnector.getInstance().getPrefix() + "Network connector rejected outgoing message as it is shutting down. Message: " + message.toString());
		}
	}

	private synchronized void processMessageQueue() {
		
		Message message = null;
		
		try {
			message = messageQueue.poll(2, TimeUnit.SECONDS);
			//message = messageQueue.poll();
		} catch (InterruptedException e) {
			message = null;
		}

		if(message != null){
			//System.err.println("Processing message " + message.toString());
			
			String host = message.get(NettyNetworkConnector.TARGET_NODE_KEYWORD).toString();
			Integer port = Integer.parseInt(message.get(NettyNetworkConnector.TARGET_PORT_KEYWORD).toString());
			
			SocketAddress sAddress = new SocketAddress(host, port);
	        
			/*
	        String id = new StringBuffer(host)
	        	.append(port.toString())
	        	.append("localhost")
	        	.append(message.get(MicroMessage.MSG_PARAM_SENDER_PORT).toString()).toString();*/
			ConnectionID id = new ConnectionID(host, port.toString());
	        
			if(!NettyNetworkConnector.getInstance().connections.containsKey(id.getId())){
				//System.out.println("Doing connect to " + sAddress.toString());
				//System.out.println("Registry: " + NettyNetworkConnector.getInstance().connections.toString());
				connect(sAddress, id);
				
				//if connection was not successful, set message to null - will not be processed.
				if(!NettyNetworkConnector.getInstance().connections.containsKey(id.getId())){
					
					System.err.println(new StringBuffer(NettyNetworkConnector.getInstance().getPrefix()).append("Message ")
							.append(message.toString()).append(" has been aborted (and saved in UnsentMessagePool) as target platform could not be connected.").toString());
					UnsentMessagePool.putUnsentMessage(message, sAddress.toString());
					message = null;
				}
			} 
			//if message has not been reset as of connection problems
			if(message != null){
				
			//System.out.println("Registry: " + NettyNetworkConnector.getInstance().connections.toString());
			String localAddress = NettyNetworkConnector.getInstance().connections.get(id.getId()).getChannel().getLocalAddress().toString();
			String cutLocalAddress = localAddress.substring(localAddress.lastIndexOf("/")+1, localAddress.lastIndexOf(":"));
			
			//System.out.println("Cut local address: " + cutLocalAddress);
			
			message.put(NettyNetworkConnector.SENDER_LOCAL_ADDRESS, cutLocalAddress);
					
			//System.out.println("AbstractWriter: Sent message " + message.toString());
			
			writeToChannel(NettyNetworkConnector.getInstance().connections.get(id.getId()).getChannel(), message, host);
			//System.err.println("AbstractWriter: Sent message " + message.toString() + " to " + host);
			}
		}
		
	}
	
	public void run(){
		shutdown = false;
		while(running || !messageQueue.isEmpty() || !shutdown || sendFutureCounter > 0){
			/*System.out.println("Still running run() queue of writer");
			System.out.println("Message queue size " + messageQueue.size());
			System.out.println("Shutdown " + shutdown);
			System.out.println("Running " + running);
			System.out.println("SendFutureCounter " + sendFutureCounter);*/
			
			
			processMessageQueue();
			//check if writer is to be closed
			if(!running){
				//System.out.println("Shutdown of writer requested.");
				//System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Writer thread ended!");
				shutdown = true;
			}
		}
		System.out.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Writer thread ended!").toString());
	}
	
	private void writeToChannel(Channel channel, final Message message, String host){
		messageTrack.add(message);
		if(channel != null){
			ChannelFuture future = serializeToChannel(channel, message);
			sendFutureCounter++;
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture arg0) throws Exception {
					if(arg0.isSuccess()){
						messageTrack.remove(message);
					} else {
						NettyNetworkConnector.getInstance().setSendError(message);
					}
					sendFutureCounter--;
				}
			});
		} else {
			System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix()).append("Connection lost during message sending attempt (Message: ").append(message.toString()).toString());
		}
	}
	
	public void printUnsentMessages(){
		System.out.println(messageTrack.toString());
	}
	
	protected abstract void initialize();
	protected abstract ClientBootstrap setClientBootstrapOptions(ClientBootstrap bootstrap);
	protected abstract ChannelFuture serializeToChannel(Channel channel, Message message);
}
