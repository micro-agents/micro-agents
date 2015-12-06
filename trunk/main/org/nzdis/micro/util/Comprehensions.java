package org.nzdis.micro.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class Comprehensions {

	/**
	 * Applies a comprehension function to each element of a given list and 
	 * manipulates the list according to this function. If the comprehension
	 * function returns null, the element is removed from the list.
	 * @param list input list
	 * @param f comprehension function
	 */
	public static <T> void applyToListInPlace(List<T> list, Func<T, T> f) {
	    if(list != null && f != null){
			for(int i=0; i<list.size(); i++){
		    	T o = f.apply(list.get(i));
		    	if(o != null){
		    		list.set(i, o);
		    	} else {
		    		list.remove(i);
		    		i--;
		    	}
		    }
	    }
		/*ListIterator<T> itr = list.listIterator();
	    while (itr.hasNext()) {
	        T output = f.apply(itr.next());
	        if(output != null){
	        	itr.set(output);
	        } else {
	        	itr.remove();
	        }
	    }*/
	}
	
	/**
	 * Applies a comprehension function to each element of a given list and 
	 * returns a new list (with those modification) as a result. The given input list 
	 * is not modified. If the comprehension function returns null the according 
	 * list element is not included in the returned list.
	 * @param in input list
	 * @param f comprehension function
	 * @return new list that is a comprehension of in (the input list)
	 */
	public static <In, Out> List<Out> map(List<In> in, Func<In, Out> f) {
		if(in != null && f != null){
		List<Out> out = new ArrayList<Out>(in.size());
			for(int i=0; i<in.size(); i++){
		    	Out o = f.apply(in.get(i));
		    	if(o != null){
		    		out.add(o);
		    	}
		    }
		    return out;
		}
	    return null;
	}
	
	/**
	 * Applies a comprehension function to each element of a given ArrayList and 
	 * returns a new ArrayList (with those modification) as a result. The given input ArrayList 
	 * is not modified. If the comprehension function returns null the according 
	 * list element is not included in the returned ArrayList. This is the ArrayList 
	 * version of @link map(List...)
	 * @param in input ArrayList
	 * @param f comprehension function
	 * @return new list that is a comprehension of in (the input ArrayList)
	 */
	public static <In, Out> ArrayList<Out> map(ArrayList<In> in, Func<In, Out> f) {
	    if(in != null && f != null){
	    	ArrayList<Out> out = new ArrayList<Out>(in.size());
		    for(int i=0; i<in.size(); i++){
		    	Out o = f.apply(in.get(i));
		    	if(o != null){
		    		out.add(o);
		    	}
		    }
		    return out;
	    }
	    return null;
	}
	
	/**
	 * Applies a comprehension function to each element of a given set and 
	 * returns a new set (with those modifications) as a result. The given input set 
	 * is not modified. If the comprehension function returns null the according 
	 * set element is not included in the returned set.
	 * @param in input set
	 * @param f comprehension function
	 * @return new list that is a comprehension of in (the input set)
	 */
	public static <In, Out> Set<Out> map(Set<In> in, Func<In, Out> f) {
	    if(in != null){
			Set<Out> out = new HashSet<Out>(in.size());
		    for (In inObj : in) {
	    	Out o = f.apply(inObj);
		    	if(o != null){
		    		out.add(o);
		    	}
		    }
		    return out;
	    }
	    return null;
	}
	
	
	/**
	 * Applies a comprehension function to each entry of a given map and 
	 * returns a newly generated map of the result. The original map is not modified.
	 * Returns null if null is given as input.
	 * @param in input map
	 * @param f comprehension function
	 * @return map comprehension as new map
	 */
	public static <K, V> Map<K, V> map(Map<K, V> in, Func<Entry<K, V>, Entry<K, V>> f) {
		if(in != null){
			Map<K, V> out = new HashMap<K, V>(in.size());
		    for (Entry<K, V> inObj : in.entrySet()) {
		    	Entry<K, V> o = f.apply(inObj);
		    	if(o != null){
		    		out.entrySet().add(o);
		    	}
		    }
		    return out;
		}
		return null;
	}
	
	/**
	 * Decomposes a given collection containing nested collections and 
	 * iterates over the contained collections which can be manipulated with a user-
	 * specified function f.
	 * @param in Collection containing collection (i.e. Collection<Collection<type>>)
	 * @param f function manipulating nested collections (not necessarily individual 
	 * elements (@see decomposeCollectionToElements for that)
	 * @return collection of all individual elements (if f returns non-null for 
	 * individual element, else this element is excluded)
	 */
	public static <In, Out> Collection<Out> decomposeCollectionToNestedCollection(Collection<HashSet<In>> in, Func<Collection<In>, Collection<Out>> f){
		if(in != null){
			Set<Out> out = new HashSet<Out>(in.size());
		    for (Collection<In> inCol : in) {
	    		Collection<Out> o = f.apply(inCol);
		    	if(o != null){
		    		out.addAll(o);
		    	}
		    }
		    return out;
	    }
		return null;
	}
	
	/**
	 * Decomposes a given collection containing nested collections and 
	 * iterates over the elements which can be manipulated with a user-
	 * specified function f.
	 * @param in Collection containing collection (i.e. Collection<Collection<type>>)
	 * @param f function to be applied to individual elements
	 * @return collection of all individual elements (if f returns non-null for individual element, else this element is excluded)
	 */
	public static <In, Out> Collection<Out> decomposeCollectionToElements(Collection<HashSet<In>> in, Func<In, Out> f){
		if(in != null){
			Set<Out> out = new HashSet<Out>(in.size());
		    for (Collection<In> inCol : in) {
		    	for(In inObj: inCol ){
		    		Out o = f.apply(inObj);
			    	if(o != null){
			    		out.add(o);
			    	}
		    	}
		    }
		    return out;
	    }
		return null;
	}

}
