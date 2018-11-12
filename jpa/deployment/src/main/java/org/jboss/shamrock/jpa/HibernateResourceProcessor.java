package org.jboss.shamrock.jpa;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.protean.impl.PersistenceUnitsHolder;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.ResourceBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorder;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;
import org.jboss.shamrock.jpa.runtime.ShamrockScanner;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public final class HibernateResourceProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<ResourceBuildItem> resources;
    @Inject
    BuildProducer<BytecodeTransformerBuildItem> transformers;

    @Inject
    CombinedIndexBuildItem index;


    @BuildStep
    void doParse(BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceProducer) {
        List<ParsedPersistenceXmlDescriptor> descriptors = PersistenceUnitsHolder.loadOriginalXMLParsedDescriptors();
        for (ParsedPersistenceXmlDescriptor i : descriptors) {
            persistenceProducer.produce(new PersistenceUnitDescriptorBuildItem(i));
        }
    }

    @BuildStep
    @Record(staticInit = true)
    public void build(BytecodeRecorder recorder, JPADeploymentTemplate template, BeanContainerBuildItem beanContainer, List<PersistenceUnitDescriptorBuildItem> descItems) throws Exception {

        List<ParsedPersistenceXmlDescriptor> descriptors = descItems.stream().map(PersistenceUnitDescriptorBuildItem::getDescriptor).collect(Collectors.toList());
        // Hibernate specific reflective classes; these are independent from the model and configuration details.
        HibernateReflectiveNeeds.registerStaticReflectiveNeeds(reflectiveClass);

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, descriptors, index.getIndex(), template);
        final KnownDomainObjects domainObjects = scavenger.discoverModelAndRegisterForReflection();

        //Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(domainObjects, transformers);

        //set up the scanner, as this scanning has already been done we need to just tell it about the classes we
        //have discovered. This scanner is bytecode serializable and is passed directly into the template
        ShamrockScanner scanner = new ShamrockScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getClassNames()) {
            ShamrockScanner.ClassDescriptorImpl desc = new ShamrockScanner.ClassDescriptorImpl(i, ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);
        for (ParsedPersistenceXmlDescriptor i : descriptors) {
            //add resources
            if (i.getProperties().containsKey("javax.persistence.sql-load-script-source")) {
                resources.produce(new ResourceBuildItem((String) i.getProperties().get("javax.persistence.sql-load-script-source")));
            } else {
                resources.produce(new ResourceBuildItem("import.sql"));
            }
        }

        //now we serialize the XML and class list to bytecode, to remove the need to re-parse the XML on JVM startup
        recorder.registerNonDefaultConstructor(ParsedPersistenceXmlDescriptor.class.getDeclaredConstructor(URL.class), (i) -> Collections.singletonList(i.getPersistenceUnitRootUrl()));
        template.initMetadata(descriptors, scanner, beanContainer.getValue());
    }

    private void enhanceEntities(final KnownDomainObjects domainObjects, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : domainObjects.getClassNames()) {
            transformers.produce(new BytecodeTransformerBuildItem(i, hibernateEntityEnhancer));
        }
    }
}
