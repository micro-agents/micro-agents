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

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.nzdis.micro.MicroMessage;
import org.nzdis.micro.messaging.SocketAddress;
import org.nzdis.micro.messaging.message.Message;

/**
 * Composite Handler for received messages.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public abstract class AbstractCompositeHandler extends SimpleChannelUpstreamHandler {

	protected synchronized void processReceivedMessage(Message message, MessageEvent e){
		
		if(!NettyNetworkConnector.getInstance().networkStarted()){
			System.err.println(NettyNetworkConnector.getInstance().getPrefix() + "Remote node " + e.getChannel().getRemoteAddress() + " tried to connect - connection refused as network not started.");
			e.getChannel().close();
		}
		/*
		if(e.getRemoteAddress().toString().equals("0.0.0.0/0.0.0.0:0")){
			System.err.println(NettyNetworkConnector.getInstance().getPrefix() + "Remote node " + e.getChannel().getRemoteAddress() + " tried to connect - refused as of invalid address!");
			e.getChannel().close();
		}*/
		//System.out.println("Received other local address: " + message.get(NettyNetworkConnector.SENDER_LOCAL_ADDRESS));
		//System.out.println("DDDD: " + e.getRemoteAddress().toString());
		//SocketAddress sAddress = new SocketAddress((InetSocketAddress)e.getRemoteAddress());
		//SocketAddress sAddress = new SocketAddress(message.get(NettyNetworkConnector.SENDER_LOCAL_ADDRESS));
		
		//Format: <IpAddress of sending node><TcpPort of sending node><target ip (my ip)><target port (my inbound port)>
		/*final String id = new StringBuffer(sAddress.getHostAddress())
		.append(message.get(MicroMessage.MSG_PARAM_SENDER_PORT).toString())
		.append("localhost")
		.append(NettyNetworkConnector.getInstance().getPort().toString()).toString();*/
		String senderHost = message.get(NettyNetworkConnector.SENDER_LOCAL_ADDRESS).toString();
		
		final ConnectionID id = new ConnectionID(senderHost, message.get(MicroMessage.MSG_PARAM_SENDER_PORT).toString());
		
		
		//System.out.println("Channel id: " + id);
		if(!NettyNetworkConnector.getInstance().connections.containsKey(id.getId())){
			NettyNetworkConnector.getInstance().connections.put(id.getId(), e.getFuture());
			e.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture arg0) throws Exception {
					System.err.println(NettyNetworkConnector.getInstance().getPrefix() + "Channel " + arg0.getChannel() + " closed and removed from registry.");
					NettyNetworkConnector.getInstance().connections.remove(id);
				}
			});
		}
		try {
			NettyNetworkConnector.getInstance().handleMessage(new SocketAddress(senderHost), message);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		
			StringBuilder sb = new StringBuilder(NettyNetworkConnector.getInstance().getPrefix());
			
			boolean caught = false;
			if(e.getCause().getClass().equals(java.nio.channels.NotYetConnectedException.class)){
				System.err.println(sb.append("Connection has not been established. Channel closed.").toString());
				caught = true;
			}
			if(e.getCause().getClass().equals(java.io.IOException.class)){
				System.err.println(sb.append("Connection " + ctx.getChannel().getRemoteAddress() + " was disconnected remotely. Channel closed."));
				caught = true;
			}
			if(e.getCause().getClass().equals(java.nio.channels.ClosedChannelException.class)){
				System.err.println(sb.append("Connection " + ctx.getChannel().getRemoteAddress() + " was closed. Channel closed.").toString());
				caught = true;
			}
			if(e.getCause().getClass().equals(java.net.ConnectException.class)){
				System.err.println(sb.append("Connection to remote address refused."));
				caught = true;
			}
			
			if(!caught){
				System.err.println(sb.append("Exception on channel to ")
						.append(e.getChannel().getRemoteAddress())
						.append(".\nCause: ")
						.append(e.getCause()).toString());
			} else {
				e.getChannel().close();
			}
	}
}
