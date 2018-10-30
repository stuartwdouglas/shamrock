package org.jboss.shamrock.deployment.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shamrock.annotations.BuildProcessor;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildResource;
import org.jboss.shamrock.deployment.BuildProcessingStep;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.CapabilityBuildItem;

@BuildProcessor
public class CapabilityStep implements BuildProcessingStep {

    @BuildResource
    List<CapabilityBuildItem> capabilitites;

    @BuildResource
    BuildProducer<Capabilities> producer;

    @Override
    public void build() throws Exception {
        Set<String> present = new HashSet<>();
        for (CapabilityBuildItem i : capabilitites) {
            present.add(i.getName());
        }

        producer.produce(new Capabilities(present));

    }
}