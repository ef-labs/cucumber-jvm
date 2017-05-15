package cucumber.runtime.java.hk2.impl;

import cucumber.api.java.ObjectFactory;
import cucumber.runtime.java.hk2.ScenarioScoped;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.After;
import org.junit.Test;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static cucumber.runtime.java.hk2.matcher.ElementsAreAllEqualMatcher.elementsAreAllEqual;
import static cucumber.runtime.java.hk2.matcher.ElementsAreAllUniqueMatcher.elementsAreAllUnique;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HK2FactoryTest {

    private ObjectFactory factory;
    private List<?> instancesFromSameScenario;
    private List<?> instancesFromDifferentScenarios;

    @After
    public void tearDown() {
        // If factory is left in start state it can cause cascading failures due to scope being left open
        try {
            factory.stop();
        } catch (Exception e) {
        }
    }

    @Test
    public void factoryCanBeIntantiatedWithDefaultConstructor() throws Exception {
        factory = new HK2Factory();
        assertThat(factory, notNullValue());
    }

    @Test
    public void factoryCanBeIntantiatedWithArgConstructor() {
        factory = new HK2Factory(ServiceLocatorFactory.getInstance().create("test"));
        assertThat(factory, notNullValue());
    }

    @Test
    public void factoryStartFailsIfScenarioScopeIsNotBound() {
        factory = new HK2Factory(ServiceLocatorFactory.getInstance().create("test"));
        try {
            factory.start();
            fail();
        } catch (CucumberHK2Exception e) {
            assertThat(e.getMessage(),
                    containsString("could not find a ScenarioContext"));
        }
    }

    static class UnscopedClass {
    }

    static class UnscopedClassBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(UnscopedClass.class);
        }
    }

    @Test
    public void shouldGiveNewInstancesOfUnscopedClassWithinAScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new UnscopedClassBinder()));
        instancesFromSameScenario = getInstancesFromSameScenario(factory, UnscopedClass.class);
        assertThat(instancesFromSameScenario, elementsAreAllUnique());
    }

    @Test
    public void shouldGiveNewInstanceOfUnscopedClassForEachScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new UnscopedClassBinder()));
        instancesFromDifferentScenarios = getInstancesFromDifferentScenarios(factory, UnscopedClass.class);
        assertThat(instancesFromDifferentScenarios, elementsAreAllUnique());
    }

    static class AnnotatedScenarioScopedClass {
    }

    static class AnnotatedScenarioScopedClassBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(AnnotatedScenarioScopedClass.class).in(ScenarioScoped.class);
        }
    }

    @Test
    public void shouldGiveTheSameInstanceOfAnnotatedScenarioScopedClassWithinAScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new AnnotatedScenarioScopedClassBinder()));
        instancesFromSameScenario = getInstancesFromSameScenario(factory, AnnotatedScenarioScopedClass.class);
        assertThat(instancesFromSameScenario, elementsAreAllEqual());
    }

    @Test
    public void shouldGiveNewInstanceOfAnnotatedScenarioScopedClassForEachScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new AnnotatedScenarioScopedClassBinder()));
        instancesFromDifferentScenarios = getInstancesFromDifferentScenarios(factory, AnnotatedScenarioScopedClass.class);
        assertThat(instancesFromDifferentScenarios, elementsAreAllUnique());
    }

    @Singleton
    static class AnnotatedSingletonClass {
    }

    static class AnnotatedSingletonClassBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(AnnotatedSingletonClass.class).in(Singleton.class);
        }
    }

    @Test
    public void shouldGiveTheSameInstanceOfAnnotatedSingletonClassWithinAScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new AnnotatedSingletonClassBinder()));
        instancesFromSameScenario = getInstancesFromSameScenario(factory, AnnotatedSingletonClass.class);
        assertThat(instancesFromSameScenario, elementsAreAllEqual());
    }

    @Test
    public void shouldGiveTheSameInstanceOfAnnotatedSingletonClassForEachScenario() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new AnnotatedSingletonClassBinder()));
        instancesFromDifferentScenarios = getInstancesFromDifferentScenarios(factory, AnnotatedSingletonClass.class);
        assertThat(instancesFromDifferentScenarios, elementsAreAllEqual());
    }

    @Test
    public void shouldGiveNewInstanceOfAnnotatedSingletonClassForEachScenarioWhenOverridingModuleBindingIsScenarioScope() {
        factory = new HK2Factory(locator(new ScenarioScopeModule(), new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(AnnotatedSingletonClass.class).in(ScenarioScoped.class);
            }
        }));
        instancesFromDifferentScenarios = getInstancesFromDifferentScenarios(factory, AnnotatedSingletonClass.class);
        assertThat(instancesFromDifferentScenarios, elementsAreAllUnique());
    }

    private ServiceLocator locator(Binder... binders) {
        ServiceLocator test = ServiceLocatorFactory.getInstance().create(UUID.randomUUID().toString());
        ServiceLocatorUtilities.bind(test, binders);
        return test;
    }


    private <E> List<E> getInstancesFromSameScenario(ObjectFactory factory, Class<E> aClass) {

        // Scenario
        factory.start();
        E o1 = factory.getInstance(aClass);
        E o2 = factory.getInstance(aClass);
        E o3 = factory.getInstance(aClass);
        factory.stop();

        return Arrays.asList(o1, o2, o3);
    }

    private <E> List<E> getInstancesFromDifferentScenarios(ObjectFactory factory, Class<E> aClass) {

        // Scenario 1
        factory.start();
        E o1 = factory.getInstance(aClass);
        factory.stop();

        // Scenario 2
        factory.start();
        E o2 = factory.getInstance(aClass);
        factory.stop();

        // Scenario 3
        factory.start();
        E o3 = factory.getInstance(aClass);
        factory.stop();

        return Arrays.asList(o1, o2, o3);
    }

}