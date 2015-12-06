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
package org.nzdis.micro.messaging.network.netty.string;

import java.util.HashMap;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.nzdis.micro.constants.SerializationTypes;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.messaging.network.netty.AbstractWriter;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

import flexjson.JSONSerializer;

/**
 * Network Writer for String serialization.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class StringWriter extends AbstractWriter {

	XStream xstream = null;
	//ObjectMapper mapper = null;
	JSONSerializer serializer = null;

	public StringWriter(){
		super();
		if(NettyNetworkConnector.getInstance().getSerialization().equals(SerializationTypes.JSON)){
			
			//JSON serialization (loosely coupled as library might not be available)
			/*
			HierarchicalStreamDriver jsonProvider = null;
			try {
				jsonProvider = (HierarchicalStreamDriver)Class.forName("com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver").newInstance();
			} catch (InstantiationException e) {
				System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
						.append("Could not instantiate serialization driver for JSON.")
						.append("Network functionality will be shut down."));
				e.printStackTrace();
				return;
			} catch (IllegalAccessException e) {
				System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
					.append("Illegal access upon instantiation of serialization driver for JSON.")
					.append("Network functionality will be shut down."));
				e.printStackTrace();
				return;
			} catch (ClassNotFoundException e) {
				System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
					.append("Serialization driver for JSON could not be found.\n")
					.append("Please check if you added the Jettison library to the classpath and restart the platform.\n")
					.append("Network functionality will be shut down."));
				return;
			} catch (NoClassDefFoundError e){
				System.err.println(new StringBuilder(NettyNetworkConnector.getInstance().getPrefix())
					.append("Serialization driver for JSON could not be found.\n")
					.append("Please check if you added the Jettison library to the classpath and restart the platform.\n")
					.append("Network functionality will be shut down."));
				return;
			}
			xstream = new XStream(jsonProvider);
			*/
			serializer = new JSONSerializer();
			//gson = new Gson();
			//mapper = new ObjectMapper();
			//mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
			
		} else {
			//XML serialization
			xstream = new XStream(new PureJavaReflectionProvider());
		}
		if(xstream != null){
			xstream.registerConverter(new MapConverter(xstream.getMapper()));
		}
	
		if(!initialized){
			initialize();
			initialized = true;
		}
	}
	
	protected synchronized void initialize(){
		//SocketChannelFactory initialization
		socketChannelFactory = 
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());
        
        //Pipeline factory initialization
        pipelineFactory = new ChannelPipelineFactory() {
           public ChannelPipeline getPipeline() throws Exception {
              return Channels.pipeline(
                       new StringEncoder(),
                        new CompositeStringHandler());
            }
        };
	}

	@Override
	protected synchronized ChannelFuture serializeToChannel(final Channel channel, Message message) {
		
		//System.out.println("Message to be sent: " + xstream.toXML(hash));
		StringBuffer sendData = null;
		if(xstream == null){
			
			sendData = new StringBuffer(serializer.serialize(message));
			
			/* Jackson approach
			try {
				sendData = new StringBuffer(mapper.writeValueAsString(message));
			} catch (JsonGenerationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			//sendData = new StringBuffer(gson.toJson(message));
		} else {
			HashMap hash = new HashMap();
			hash.putAll(message);
			sendData = new StringBuffer(xstream.toXML(hash));
		}
		
		ChannelFuture future = null;
		String prefix = "";
		int chars = 8;
		int tempIt = ((Integer)sendData.length()).toString().length(); 
		if(tempIt < chars){
			while(tempIt < chars){
				prefix += "0";
				tempIt++;
			}
		} else {
			System.out.println(NettyNetworkConnector.getInstance().getPrefix() + "Message too long!");
		}
		
		future = channel.write(prefix + sendData.length());
		try {
			future.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(future.isSuccess()){
			return channel.write(sendData.toString());
		} else {
			System.err.println(NettyNetworkConnector.getInstance().getPrefix() + "Error when sending prefix message for length indication.\nSending actual message anyway.");
			return channel.write(sendData.toString());
		}
		/*
		final String sendString = sendData.toString();
		ChannelFuture realFuture;
		//System.out.println("Wrote " + prefix + sendData.length());
		future.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture arg0) throws Exception {
				if(arg0.isSuccess()){
					realFuture = channel.write(sendString);
				}
			}
		});
		
		//System.out.println("Sent data: " + sendData.toString());

		return future;*/
	}

	@Override
	protected ClientBootstrap setClientBootstrapOptions(ClientBootstrap bootstrap) {
		/*bootstrap.setOption("child.receiveBufferSize", 1024 * 1024);
		bootstrap.setOption("receiveBufferSize", 1024 * 1024);
		bootstrap.setOption("sendBufferSize", 1024 * 1024);
		bootstrap.setOption("child.sendBufferSize", 1024 * 1024);*/
		return bootstrap;
	}
	
	/*
	private byte[] intToByteArray (final int integer) {
		int byteNum = (40 - Integer.numberOfLeadingZeros (integer < 0 ? ~integer : integer)) / 8;
		byte[] byteArray = new byte[4];
		
		for (int n = 0; n < byteNum; n++)
			byteArray[3 - n] = (byte) (integer >>> (n * 8));
		
		return (byteArray);
	}*/

}
