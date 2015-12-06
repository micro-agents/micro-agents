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

import java.io.Serializable;

/**
 * Abstract specification of a location for a platform. It can contain
 * a narrative description or geo coordinates.
 * 
 * @author cfrantz
 *
 */
public class Location implements Serializable{
	
	private String locationName;
	private Double longitude;
	private Double latitude;
	private String operatingSystem;
	
	public Location(){
		this.operatingSystem = MTRuntime.getOperatingSystem();
	}
	
	public Location(String name){
		this.locationName = name;
		this.operatingSystem = MTRuntime.getOperatingSystem();
	}
	
	public Location(String name, Double latitude, Double longitude){
		this.locationName = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.operatingSystem = MTRuntime.getOperatingSystem();
	}
	
	public void setName(String name){
		this.locationName = name;
	}
	
	public String getName(){
		return this.locationName;
	}
	
	public void setLongitude(Double longitude){
		this.longitude = longitude;
	}
	
	public Double getLongitude(){
		return longitude;
	}
	
	public void setLatitude(Double latitude){
		this.latitude = latitude;
	}
	
	public Double getLatitude(){
		return latitude;
	}
	
	public void setOperatingSystem(String operatingSystem){
		this.operatingSystem = operatingSystem;
	}
	
	public String getOperatingSystem(){
		return operatingSystem;
	}
	
	public String toString(){
		return new StringBuffer("Location name: ").append(this.locationName)
			.append(", Latitude: ").append(this.latitude).append(", Longitude: ").append(this.longitude)
			.append(", Operating system: ").append(this.operatingSystem).toString();
	}

}
