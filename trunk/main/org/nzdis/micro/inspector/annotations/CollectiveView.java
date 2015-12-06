package org.nzdis.micro.inspector.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks methods or fields within agents that are collectively inspected using the Micro-Agent Platform Inspector.
 * This allows the real-time observation of all agent internals within the Platform Inspectors 'Collective Agent View', instead 
 * of concentrating on the property for an individual agent (see @Inspect annotation).
 * Note that inspection works recursively: all fields marked with this annotation can themselves hold methods or fields that carry 
 * the annotation. This way the experimenter can inspect properties used by the agent (e.g. memory structures implemented in separate modules). 
 * Note: When annotating methods, ensure that those do not require input parameters.
 * 
 * @author cfrantz
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface CollectiveView {

}
