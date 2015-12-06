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
package org.nzdis.micro.inspector;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nzdis.micro.Role;
import org.nzdis.micro.inspector.annotations.CollectiveView;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.sofosim.environment.stats.DataStructurePrettyPrinter;

/**
 * This class provides the actual inspection services of the PlatformInspector.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2011/09/07 00:00:00 $
 *
 */
public class InspectorAnnotationReflector {

	public static final String INSPECTION = "INSPECTION";
	public static final String COLLECTIVE_INSPECTION = "COLLECTIVE_INSPECTION";
	public static final Class inspectionAnnotation = Inspect.class;
	public static final Class collectiveViewAnnotation = CollectiveView.class;
	protected static boolean printClassName = true;
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static final String roleErrorPrefix = "Class member ";
	private static final String roleErrorSuffix = " to be inspected is a role (and ignored as of potential recursion).";
		
	/**
	 * This method iterates through the fieldList and methodList, puts the results 
	 * into the provided treeMap and returns it.
	 * @param map
	 * @param fieldList
	 * @param methodList
	 * @param object object to inspect, or null if class to be inspected.
	 * @param inspectionType type of inspection to be done on element
	 * @return
	 * @throws InvocationTargetException will be thrown if the inspected objected does not exist (anymore)
	 */
	private static LinkedHashMap<String, Object> inspectSpecifiedFieldsAndMethods(LinkedHashMap<String, Object> map, ArrayList<Field[]> fieldList, ArrayList<Method[]> methodList, Object object, String inspectionType) throws InvocationTargetException{
		
		Class annotation = null;
		if(inspectionType.equals(INSPECTION)){
			annotation = inspectionAnnotation;
		}
		if(inspectionType.equals(COLLECTIVE_INSPECTION)){
			annotation = collectiveViewAnnotation;
		}
		if(annotation == null){
			annotation = inspectionAnnotation;
			System.out.println("Inspect set to detailed inspection as inspection type parameter is set to null.");
		}
		
		for(int i=0; i<fieldList.size(); i++){
			for (Field field : fieldList.get(i)) {
				try {
					field.setAccessible(true);
					if(field.isAnnotationPresent(annotation)){
						processElement(map, field.getName(), field.get(object), object, inspectionType);
					}
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		for(int l=0; l<methodList.size(); l++){
			for (Method method : methodList.get(l)) {
		    	try {
		    		method.setAccessible(true);		
		    		if(method.isAnnotationPresent(annotation)){
			    		processElement(map, method.getName() + "()", method.invoke(object), object, inspectionType);
		    		}
				} catch (IllegalArgumentException e) {
					System.out.println(e.getClass().getSimpleName() + " when inspecting " + method.getName() + " on object " + object);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		    	}
			}
		}
		return map;
	}
	
	private static void processElement(LinkedHashMap<String, Object> map,
			String fieldOrMethodName, Object invokedMethodOrField, Object object, String inspectionType) throws InvocationTargetException {
		
		if(invokedMethodOrField != null){
			//check if field is of type role --> Ignore
			if(implementsInterface(Role.class, invokedMethodOrField)){
				addToMap(map, object, fieldOrMethodName, new StringBuffer(roleErrorPrefix).append(fieldOrMethodName).append(roleErrorSuffix).toString());
			} else {
				//System.out.println("Annotation present, object ...." + field.get(object) + field.get(object).getClass().getName());
				LinkedHashMap<String, Object> intMap = inspect(invokedMethodOrField, new LinkedHashMap<String, Object>(), inspectionType);
				StringBuffer arrayBuffer = null;
				//Array check
				if(invokedMethodOrField.getClass().isArray()){
					//System.out.println("Array");
					arrayBuffer = decomposeArray(invokedMethodOrField, intMap, true);
				} else {
					if(implementsInterface(Map.class, invokedMethodOrField)){
						try {
							for(Entry<Object,Object> obj: ((Map<Object,Object>)invokedMethodOrField).entrySet()){

								if(obj.getValue().getClass().isArray()){
									//field value in map is array
									arrayBuffer = decomposeArray(obj.getValue(), intMap, true);

									//fill decomposed array as value for decomposed map values
									intMap.put(obj.getKey().toString(), arrayBuffer);
									//reset buffer to avoid overwriting of full map with buffer of last line
									arrayBuffer = null;
								} else if(implementsInterface(Map.class, obj.getValue())){
									//non-array map values are recursively decomposed
									LinkedHashMap<String,Object> int2Map = inspect(obj.getValue(), inspectionType);
									if(!int2Map.isEmpty()){
										intMap.put(obj.getKey().toString(), int2Map);
									}
								} else {
									//other value content - such as collections (whose formatting is performed by the inspector UI itself)
									intMap.put(obj.getKey().toString(), obj.getValue());
								}
							}
						} catch(ConcurrentModificationException e){
							//ignore as it will be repeated anyway
						}
					}
				}
				
				
				if(intMap.isEmpty() && arrayBuffer == null){
					//if internal map (used for decomposition of embedded maps/arrays) is empty, simply print field value
					addToMap(map, object, fieldOrMethodName, invokedMethodOrField);
				} else {
					if(!intMap.isEmpty()){
						//else check for internal map (decomposed map entries)
						addToMap(map, object, fieldOrMethodName, intMap);
					}
					if(arrayBuffer != null){
						//or buffer --> complex data structure in field value decomposed as StringBuffer
						addToMap(map, object, fieldOrMethodName, buffer);
					}					
				}
			}
		}
	}

	/**
	 * Checks if a given object (or its recursive superclasses) implement a 
	 * given interface. Particularly useful to check for collections and maps 
	 * but works with any object
	 * @param iface interface to check for
	 * @param object object to check
	 * @return boolean value indicating if the object's type (or supertypes) implements the interface
	 */
	public static boolean implementsInterface(Class iface, Object object){
		boolean implementsIface = false;
		Class recursiveClass = object.getClass();
		while(recursiveClass != null && !implementsIface){
			for(int i=0; i<recursiveClass.getInterfaces().length; i++){
				if(recursiveClass.getInterfaces()[i].equals(iface)){
					implementsIface = true;
					break;
				}
			}
			if(!implementsIface){
				recursiveClass = recursiveClass.getSuperclass();
			}
		}
		return implementsIface;
	}
	
	private static void addToMap(LinkedHashMap<String, Object> map, Object object, String fieldOrMethodName, Object value){
		if(printClassName && object != null){
			map.put(new StringBuffer(object.getClass().getSimpleName()).append(" - ").append(fieldOrMethodName).toString(), value); 
		} else {
			map.put(fieldOrMethodName, value);
		}
	}
	
	
	private static StringBuffer buffer = new StringBuffer();
	private static final String spaces = "  ";
	private static int number = 0;
	
	private static void addPrefix(){
		for(int i=0; i<number*2; i++){
			buffer.append(spaces);
		}
	}
	
	private synchronized static StringBuffer decomposeArray(Object object,
			LinkedHashMap<String, Object> intMap, boolean resetBuffer) {
		if(resetBuffer){
			buffer = new StringBuffer();
		}
		buffer.append(LINE_SEPARATOR);
		addPrefix();
		buffer.append("[");
		boolean addPrefix = true;
		for(int i=0; i<Array.getLength(object); i++){
			if(Array.get(object, i) != null && Array.get(object, i).getClass().isArray()){
				if(i != 0){
					addPrefix();
					buffer.append(" ");
				}
				buffer.append(" Index ").append(i).append(" (Level ").append(number).append(")");
				number++;
				decomposeArray(Array.get(object, i), intMap, false);
				number--;
			} else {
				buffer.append(" ");
				if(Array.get(object, i) != null){
					addPrefix = false;
					buffer.append(Array.get(object, i));
				}
				if(i != Array.getLength(object)-1){
					buffer.append(", ");
				} else {
					buffer.append(" ");
				}
			}
		}
		if(addPrefix){
			addPrefix();
		}
		buffer.append("]");
		buffer.append(LINE_SEPARATOR);
		
		if(resetBuffer){
			return buffer;
		}
		return null;
	}
	
	/**
	 * This method inspects a given object on Inspect annotations 
	 * and saves the field values and method return values into a 
	 * newly created LinkedHashMap.
	 * @param objectToInspect
	 * @param inspectionType
	 * @return LinkedHashMap containing introspection results for all annotated fields and methods.
	 * @throws InvocationTargetException will be thrown if the inspected objected does not exist (anymore)
	 */
	public static LinkedHashMap<String, Object> inspect(Object objectToInspect, String inspectionType) throws InvocationTargetException{
		return inspect(objectToInspect, null, inspectionType);
	}
	
	/**
	 * This method inspects a given object on Inspect annotations 
	 * and saves the field values and method return values into a 
	 * (given) LinkedHashMap.
	 * @param objectToInspect object to be inspected
	 * @param map map to add results to, if null a new map is created
	 * @param annotationType indicating introspection type 
	 * (e.g. INSPECTION (individual inspection) or COLLECTIVE_INSPECTION (collective inspection))
	 * @return LinkedHashMap containing introspection results for all annotated fields and methods.
	 * @throws InvocationTargetException 
	 */
	public static LinkedHashMap<String, Object> inspect(Object objectToInspect, LinkedHashMap<String, Object> map, String annotationType) throws InvocationTargetException{
		
		LinkedHashMap<String, Object> linkedMap;
		if(map == null){
			linkedMap = new LinkedHashMap<String, Object>(); 
		} else {
			linkedMap = map;
		}
		
		if(objectToInspect != null){
			ArrayList<Field[]> fieldList = new ArrayList<Field[]>();
			fieldList.add(objectToInspect.getClass().getDeclaredFields());
			fieldList.add(objectToInspect.getClass().getFields());
			
			ArrayList<Method[]> methodList = new ArrayList<Method[]>();
			methodList.add(objectToInspect.getClass().getDeclaredMethods());
			methodList.add(objectToInspect.getClass().getMethods());
			
			Class recursiveClass = objectToInspect.getClass().getSuperclass();
			while(recursiveClass != null){
				fieldList.add(recursiveClass.getDeclaredFields());
				fieldList.add(recursiveClass.getFields());
				methodList.add(recursiveClass.getDeclaredMethods());
				methodList.add(recursiveClass.getMethods());
				recursiveClass = recursiveClass.getSuperclass();
			}
			
			return inspectSpecifiedFieldsAndMethods(linkedMap, fieldList, methodList, objectToInspect, annotationType);
		} else {
			System.err.println("Object to be inspected is null.");
		}
		return linkedMap;
	}
	
	/**
	 * This method inspects a given class on Inspect annotations 
	 * and saves the field values and method return values into a 
	 * newly created LinkedHashMap.
	 * @param objectToInspect
	 * @param inspectionType
	 * @return LinkedHashMap containing introspection results for all annotated fields and methods.
	 * @throws InvocationTargetException will be thrown if the inspected objected does not exist (anymore)
	 */
	public static LinkedHashMap<String, Object> inspect(Class classToInspect, String inspectionType) throws InvocationTargetException{
		return inspect(classToInspect, null, inspectionType);
	}
	
	/**
	 * This method inspects a given class on Inspect annotations 
	 * and saves the field values and method results into a LinkedHashMap.
	 * @param map map to add results to, if null a new map is created
	 * @param inspectionType indicating inspection type 
	 * (e.g. INSPECTION (individual inspection) or COLLECTIVE_INSPECTION (collective inspection))
	 * @return LinkedHashMap containing introspection results for all annotated fields and methods.
	 * @throws InvocationTargetException will be thrown if the inspected objected does not exist (anymore)
	 */
	public static LinkedHashMap<String, Object> inspect(Class classToInspect, LinkedHashMap<String, Object> map, String inspectionType) throws InvocationTargetException{
		
		LinkedHashMap<String, Object> linkedMap;
		if(map == null){
			linkedMap = new LinkedHashMap<String, Object>(); 
		} else {
			linkedMap = map;
		}
		
		ArrayList<Field[]> fieldList = new ArrayList<Field[]>();
		fieldList.add(classToInspect.getDeclaredFields());
		fieldList.add(classToInspect.getFields());

		ArrayList<Method[]> methodList = new ArrayList<Method[]>();
		methodList.add(classToInspect.getDeclaredMethods());
		methodList.add(classToInspect.getMethods());
		
		Class recursiveClass = classToInspect.getClass().getSuperclass();
		while(recursiveClass != null){
			fieldList.add(recursiveClass.getDeclaredFields());
			fieldList.add(recursiveClass.getFields());
			methodList.add(recursiveClass.getDeclaredMethods());
			methodList.add(recursiveClass.getMethods());
			recursiveClass = recursiveClass.getSuperclass();
		}
		return inspectSpecifiedFieldsAndMethods(linkedMap, fieldList, methodList, null, inspectionType);
	}
	
}
