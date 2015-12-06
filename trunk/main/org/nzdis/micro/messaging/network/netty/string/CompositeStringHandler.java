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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.nzdis.micro.constants.SerializationTypes;
import org.nzdis.micro.messaging.message.Message;
import org.nzdis.micro.messaging.network.netty.AbstractCompositeHandler;
import org.nzdis.micro.messaging.network.netty.NettyNetworkConnector;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

import flexjson.JSONDeserializer;

/**
 * CompositeHandler for received messages with String serialization.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 *
 */
public class CompositeStringHandler extends AbstractCompositeHandler {

	XStream xstream =  null;
	//ObjectMapper mapper = null;
	JSONDeserializer deserializer = null;

	private ChannelBuffer buffer = new DynamicChannelBuffer(25);
	int msgLength = 0;
	int headerLength = 8;
	
	public CompositeStringHandler(){
		if(NettyNetworkConnector.getInstance().getSerialization().equals(SerializationTypes.JSON)){
			
			deserializer = new JSONDeserializer();
			/*
			//JSON serialization (loosely coupled as library might not be available)
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
	}
	
	@Override
    public synchronized void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		//System.out.println("Message received: " + e.getMessage().toString());
		//System.out.println("Message length received: " + e.getMessage().toString().length());
		
		buffer.writeBytes((ChannelBuffer)e.getMessage());
		
		if(buffer.readableBytes() >= headerLength && msgLength == 0){
			
			byte[] temp = new byte[headerLength];
			
			buffer.readBytes(temp, 0, 8);
			buffer.markReaderIndex();
			buffer.discardReadBytes();
			
			msgLength = Integer.parseInt(new String(temp));
			//System.out.println("Bytes: " + msgLength);
		} 			 
		if(buffer.readableBytes() >= msgLength){
			byte[] temp = new byte[msgLength];
			buffer.readBytes(temp, 0, msgLength);
			msgLength = 0;
			buffer.markReaderIndex();
			//System.out.println("Final message: " + new String(temp));
			buffer.discardReadBytes();
			
			Message message = null;
			if(xstream == null){
				
				/* Jackson approach
				try {
					message = new Message(mapper.readValue(temp, HashMap.class));
				} catch (JsonParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (JsonMappingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				//message = new Message(gson.fromJson(temp.toString(), HashMap.class));
				
				message = new Message((HashMap)deserializer.deserialize(new String(temp)));
				
			} else {
				HashMap hash = new HashMap();
				hash.putAll((HashMap)xstream.fromXML(new String(temp)));
				message = new Message(hash);
			}
			//System.out.println("received single message: " + hash);
			
			processReceivedMessage(message, e);
		}
	}
}
