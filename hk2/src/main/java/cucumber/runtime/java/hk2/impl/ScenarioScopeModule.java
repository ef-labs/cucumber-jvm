package cucumber.runtime.java.hk2.impl;

import cucumber.runtime.java.hk2.ScenarioScoped;
import cucumber.runtime.java.hk2.ServiceLocatorSource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Enables the {@link ScenarioScoped} context, must be installed in all user specified {@link ServiceLocatorSource}
 */
public class ScenarioScopeModule extends AbstractBinder {

    @Override
    protected void configure() {
        addActiveDescriptor(ScenarioContext.class);
    }
}
