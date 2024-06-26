package org.nzdis.micro.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class DataStructurePrettyPrinter {

	private static int decompositionLevel = 1;
	private static int mapDecompositionThreshold = 6;
	
	/**
	 * Sets threshold before decomposing map into line-based entries.
	 * @param mapDecompositionThreshold
	 */
	public static void setMapDecompositionThreshold(int mapDecompositionThreshold) {
		DataStructurePrettyPrinter.mapDecompositionThreshold = mapDecompositionThreshold;
	}
	
	/**
	 * Produces human-readable output for Arrays, ArrayLists and Maps. Any other type is simply printed 
	 * based on its toString() method.
	 * @param value Object to be printed in decomposed form.
	 * @param buffer StringBuffer the output is appended to. New buffer is created if value is null.
	 * @return StringBuffer containing input object's string representation
	 */
	public static synchronized StringBuffer decomposeRecursively(Object value, StringBuffer buffer){
		return decomposeRecursively(value, buffer, mapDecompositionThreshold);
	}
	
	/**
	 * Produces human-readable output for Arrays, ArrayLists and Maps. Any other type is simply printed 
	 * based on its toString() method.
	 * @param value Object to be printed in decomposed form.
	 * @param buffer StringBuffer the output is appended to. New buffer is created if value is null.
	 * @param decompositionThreshold Threshold at which map elements are decomposed in individual lines.
	 * @return StringBuffer containing input object's string representation
	 */
	public static synchronized StringBuffer decomposeRecursively(Object value, StringBuffer buffer, int decompositionThreshold){
		StringBuffer intBuffer = buffer;
		if(intBuffer == null){
			intBuffer = new StringBuffer();
		}
		if(containsMap(value)){
			boolean containsMap = false;
			int mapSize = 0;
			for(Object key: ((Map)value).keySet()){
				if(containsMap(((Map)value).get(key))){
					containsMap = true;
					break;
				}
			}
			mapSize = ((Map)value).size();
			if(!containsMap && mapSize < decompositionThreshold){
				//if map on highest level (but no embedded map) --> just print
				if(mapSize > 0){
					intBuffer.append("(").append(mapSize).append(") ");
				}
				intBuffer.append(value);
			} else {
				//else (if embedded map or threshold exceeded) decompose into elements to make it more readable
				if(mapSize > 0){
					intBuffer.append(" (").append(mapSize).append(") ");
				}
				for(Object key: ((Map)value).keySet()){
					intBuffer.append(System.getProperty("line.separator")).append(calcStringPrefix()).append(" ").append(key);
					intBuffer.append(": ");
					decompositionLevel++;
					intBuffer = decomposeRecursively(((Map)value).get(key), intBuffer);
					decompositionLevel--;
				}
			}
		} else {
			boolean isArray;
			try {
				isArray = value.getClass().isArray();
				//call can cause NPE on primitive type
			} catch (NullPointerException e) {
				isArray = false;
			}
			if (isArray) {
				//System.out.println("Value is array: " + value);
				//if value is array, decompose
				intBuffer.append("[");
				boolean allNull = true;
				boolean decompose = Array.getLength(value) > decompositionThreshold;
				for (int i=0; i<Array.getLength(value); i++) {
					if (Array.get(value, i) != null) {
						intBuffer = decomposeRecursively(Array.get(value, i), intBuffer);
						intBuffer.append(", ");
						if (decompose) {
							intBuffer.append(System.getProperty("line.separator"));
						}
						allNull = false;
					}
				}
				if (!allNull) {
					//only remove separators if there are any values in array (i.e. any separators have been added in the first place)
					int lastIndex = intBuffer.lastIndexOf(", ");
					intBuffer.delete(lastIndex, lastIndex+2);
				}
				intBuffer.append("]");
			} else if(value.getClass().equals(ArrayList.class)){
				//if is ArrayList, decompose ...
				ArrayList list = (ArrayList)value;
				intBuffer.append("List containing ").append(list.size()).append(" elements: ").append(System.getProperty("line.separator"));
				for(int i = 0; i < list.size(); i++){
					//and print individual items
					intBuffer.append(list.get(i)).append(System.getProperty("line.separator"));
				}
			} else {
				//System.out.println("Value is NOT array: " + value);
				//else simply add to print buffer
				intBuffer.append(value);
			}
		}
		return intBuffer;
	}
	
	/**
	 * Tests whether a given object is a map (or a specialisation of a map).
	 * @param object
	 * @return
	 */
	public static boolean containsMap(Object object){
		boolean containsMap = false;
		Class recursiveClass = null;
		try {
			//call can break when called on primitive type
			recursiveClass = object.getClass();
		} catch (NullPointerException e){
			return false;
		}
		while(recursiveClass != null && !containsMap){
			for(int i=0; i<recursiveClass.getInterfaces().length; i++){
				if(recursiveClass.getInterfaces()[i].equals(Map.class)){
					containsMap = true;
					break;
				}
			}
			if(!containsMap){
				recursiveClass = recursiveClass.getSuperclass();
			}
		}
		return containsMap;
	}
	
	private static int previousLength = 0;
	
	private static StringBuffer calcStringPrefix(){
		StringBuffer buf = new StringBuffer();
		int decomp = decompositionLevel;
		while(decomp > 0){
			buf.append("-");
			decomp--;
		}
		if(previousLength != 0 && previousLength > buf.length()){
			previousLength = buf.length();
			buf = new StringBuffer(System.getProperty("line.separator")).append(buf);
		} else {
			previousLength = buf.length();
		}
		return buf;
	}
}
