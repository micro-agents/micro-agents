package org.nzdis.micro.util;

/**
 * Function interface for specification of Comprehension functions.
 * 
 * @author cfrantz
 *
 * @param <In> Type for input element
 * @param <Out> Type of returned (out) element.
 */
public interface Func<In, Out> {

	public Out apply(In in);

}
