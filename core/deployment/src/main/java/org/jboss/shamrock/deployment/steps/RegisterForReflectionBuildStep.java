/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.deployment.steps;

import javax.inject.Inject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.runtime.RegisterForReflection;

public class RegisterForReflectionBuildStep {

  @Inject BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

  @Inject BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

  @Inject CombinedIndexBuildItem combinedIndexBuildItem;

  @BuildStep
  public void build() throws Exception {
    for (AnnotationInstance i :
        combinedIndexBuildItem
            .getIndex()
            .getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {
      ClassInfo target = i.target().asClass();
      boolean methods = i.value("methods") == null || i.value("methods").asBoolean();
      boolean fields = i.value("fields") == null || i.value("fields").asBoolean();
      reflectiveClass.produce(
          new ReflectiveClassBuildItem(methods, fields, target.name().toString()));
    }
  }
}
