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

import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.nzdis.micro.messaging.network.netty.AbstractReader;

/**
 * Network Reader for String serialization.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class StringReader extends AbstractReader {
	

	public StringReader(){
		super();
	}

	protected synchronized void initialize(){
		//SocketChannelFactory initialization
		socketChannelFactory = 
                new NioServerSocketChannelFactory(
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
	protected ServerBootstrap setServerBootstrapOptions(ServerBootstrap bootstrap) {
		/*bootstrap.setOption("child.receiveBufferSize", 1024 * 1024);
		bootstrap.setOption("receiveBufferSize", 1024 * 1024);
		bootstrap.setOption("sendBufferSize", 1024 * 1024);
		bootstrap.setOption("child.sendBufferSize", 1024 * 1024);*/
		return bootstrap;
	}	
}
