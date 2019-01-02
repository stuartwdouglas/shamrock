package org.jboss.shamrock.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given class can be injected as a configuration object.  A configuration group can contain multiple
 * nested items and configuration groups.
 * <p>
 * <em>Configuration group properties cannot be {@code Optional} or {@code null}.</em>  It is expected that, in the event
 * that a group property can be disabled, this should be done using a {@code boolean} property (preferably the unnamed
 * property or a {@code enabled} property).  For example:
<pre><code>
shamrock.extra-fast-mode         = yes
shamrock.extra-fast-mode.gadgets = 20
</code></pre>
 * Or:
<pre><code>
shamrock.laser-beam.enabled    = true
shamrock.laser-beam.mount-mode = sharks
 </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ConfigGroup {
}
