package cucumber.runtime.java.hk2.impl;

import cucumber.api.java.ObjectFactory;
import cucumber.runtime.Env;
import cucumber.runtime.java.hk2.ScenarioScoped;
import cucumber.runtime.java.hk2.ServiceLocatorSource;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.HashSet;
import java.util.Set;

/**
 * Guice implementation of the <code>cucumber.api.java.ObjectFactory</code>.
 */
public class HK2Factory implements ObjectFactory {

    private final ServiceLocatorSource locatorSource;

    private Set<Class<?>> cucumberClasses = new HashSet<Class<?>>();

    private ScenarioContext context;
    private Binder binder;

    public HK2Factory() {
        this.locatorSource = new ServiceLocatorSourceFactory(Env.INSTANCE).create();
    }

    /**
     * Package private constructor that is called by the public constructor at runtime and is also called directly by
     * tests.
     *
     * @param serviceLocator a service locator configured with a ScenarioScoped binding
     */
    HK2Factory(final ServiceLocator serviceLocator) {
        this.locatorSource = new ServiceLocatorSource() {
            @Override
            public ServiceLocator getServiceLocator() {
                return serviceLocator;
            }
        };
    }

    public boolean addClass(Class<?> clazz) {
        cucumberClasses.add(clazz);
        return true;
    }

    public void start() {
        // Waiting as long as possible before making the first call to getServiceLocator()
        initContext();
        bindCucumberClasses();
    }

    private void initContext() {
        if (context != null) {
            return;
        }
        context = locatorSource.getServiceLocator().getService(ScenarioContext.class);
        if (context == null) {
            throw new CucumberHK2Exception("The ServiceLocator could not find a ScenarioContext, have you installed the ScenarioScopeModule?");
        }
    }

    private void bindCucumberClasses() {
        if (binder != null) {
            return;
        }
        binder = new AbstractBinder() {
            @Override
            protected void configure() {
                for (Class<?> clazz : cucumberClasses) {
                    bindAsContract(clazz).in(ScenarioScoped.class);
                }
            }
        };
        ServiceLocatorUtilities.bind(locatorSource.getServiceLocator(), binder);
    }

    public void stop() {
        context.shutdown();
    }

    public <T> T getInstance(Class<T> clazz) {
        return locatorSource.getServiceLocator().getService(clazz);
    }
}
