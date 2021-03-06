/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for tracking network configurations which specify how the discovered
 * network information should be interpreted and how the core or applications
 * should act on or configure the network.
 */
@Beta
public interface NetworkConfigService
        extends ListenerService<NetworkConfigEvent, NetworkConfigListener> {

    /**
     * Returns the set of subject classes for which configuration may be
     * available.
     *
     * @return set of subject classes
     */
    Set<Class> getSubjectClasses();

    /**
     * Returns the subject factory with the specified key.
     *
     * @param subjectKey subject class key
     * @return subject class
     */
    SubjectFactory getSubjectFactory(String subjectKey);

    /**
     * Returns the subject factory for the specified class.
     *
     * @param subjectClass subject class
     * @return subject class key
     */
    SubjectFactory getSubjectFactory(Class subjectClass);

    /**
     * Returns the configuration class with the specified key.
     *
     * @param subjectKey subject class key
     * @param configKey  subject class name
     * @return subject class
     */
    Class<? extends Config> getConfigClass(String subjectKey, String configKey);

    /**
     * Returns the set of subjects for which some configuration is available.
     *
     * @param subjectClass subject class
     * @param <S>          type of subject
     * @return set of configured subjects
     */
    <S> Set<S> getSubjects(Class<S> subjectClass);

    /**
     * Returns the set of subjects for which the specified configuration is
     * available.
     *
     * @param subjectClass subject class
     * @param configClass  configuration class
     * @param <S>          type of subject
     * @param <C>          type of configuration
     * @return set of configured subjects
     */
    <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass);

    /**
     * Returns all configurations for the specified subject.
     *
     * @param subject configuration subject
     * @param <S>     type of subject
     * @return set of configurations
     */
    <S> Set<? extends Config<S>> getConfigs(S subject);

    /**
     * Returns the configuration for the specified subject and configuration
     * class if one is available; null otherwise.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration or null if one is not available
     */
    <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass);

    /**
     * Creates a new configuration for the specified subject and configuration
     * class. If one already exists, it is simply returned.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration or null if one is not available
     */
    <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass);

    /**
     * Applies configuration for the specified subject and configuration
     * class using the raw JSON object. If configuration already exists, it
     * will be updated.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param json        raw JSON node containing the configuration data
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration or null if one is not available
     */
    <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass,
                                           ObjectNode json);

    /**
     * Clears any configuration for the specified subject and configuration
     * class. If one does not exist, this call has no effect.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     */
    <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass);

}
