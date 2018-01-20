/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.kstream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.streams.kstream.KStream;

import org.springframework.cloud.stream.binder.kstream.config.KStreamConsumerProperties;
import org.springframework.cloud.stream.config.BindingProperties;

/**
 * A catalogue containing all the inbound and outboud KStreams.
 * It registers {@link BindingProperties} and {@link KStreamConsumerProperties}
 * for the bounded KStreams. This registry provides services for finding
 * specific binding level information for the bounded KStream. This includes
 * information such as the configured content type, destination etc.
 *
 * @since 2.0.0
 *
 * @author Soby Chacko
 */
public class KStreamBindingInformationCatalogue {

	private final Map<KStream<?, ?>, BindingProperties> bindingProperties = new ConcurrentHashMap<>();
	private final Map<KStream<?, ?>, KStreamConsumerProperties> consumerProperties = new ConcurrentHashMap<>();

	/**
	 * For a given bounded {@link KStream}, retrieve it's corresponding destination
	 * on the broker.
	 *
	 * @param bindingTarget KStream binding target
	 * @return destination topic on Kafka
	 */
	public String getDestination(KStream<?,?> bindingTarget) {
		BindingProperties bindingProperties = this.bindingProperties.get(bindingTarget);
		return bindingProperties.getDestination();
	}

	/**
	 * Is native decoding is enabled on this {@link KStream}.
	 *
	 * @param bindingTarget KStream binding target
	 * @return true if native decoding is enabled, fasle otherwise.
	 */
	public boolean isUseNativeDecoding(KStream<?,?> bindingTarget) {
		BindingProperties bindingProperties = this.bindingProperties.get(bindingTarget);
		return bindingProperties.getConsumer().isUseNativeDecoding();
	}

	/**
	 * Is DLQ enabled for this {@link KStream}
	 *
	 * @param bindingTarget KStream binding target
	 * @return true if DLQ is enabled, false otherwise.
	 */
	public boolean isEnableDlq(KStream<?,?> bindingTarget) {
		return consumerProperties.get(bindingTarget).isEnableDlq();
	}

	/**
	 * Retrieve the content type associated with a given {@link KStream}
	 *
	 * @param bindingTarget KStream binding target
	 * @return content Type associated.
	 */
	public String getContentType(KStream<?,?> bindingTarget) {
		BindingProperties bindingProperties = this.bindingProperties.get(bindingTarget);
		return bindingProperties.getContentType();
	}

	/**
	 * Retrieve any configured Serde error handling strategies for this {@link KStream}
	 *
	 * @param bindingTarget KStream binding target
	 * @return configured Serde error handling strategy
	 */
	public KStreamConsumerProperties.SerdeError getSerdeError(KStream<?,?> bindingTarget) {
		return consumerProperties.get(bindingTarget).getSerdeError();
	}

	/**
	 * Register a cache for bounded KStream -> {@link BindingProperties}
	 *
	 * @param bindingTarget KStream binding target
	 * @param bindingProperties {@link BindingProperties} for this KStream
	 */
	public void registerBindingProperties(KStream<?,?> bindingTarget, BindingProperties bindingProperties) {
		this.bindingProperties.put(bindingTarget, bindingProperties);
	}

	/**
	 * Register a cache for bounded KStream -> {@link KStreamConsumerProperties}
	 *
	 * @param bindingTarget KStream binding target
	 * @param kStreamConsumerProperties Consumer properties for this KStream
	 */
	public void registerConsumerProperties(KStream<?,?> bindingTarget, KStreamConsumerProperties kStreamConsumerProperties) {
		this.consumerProperties.put(bindingTarget, kStreamConsumerProperties);
	}

}
