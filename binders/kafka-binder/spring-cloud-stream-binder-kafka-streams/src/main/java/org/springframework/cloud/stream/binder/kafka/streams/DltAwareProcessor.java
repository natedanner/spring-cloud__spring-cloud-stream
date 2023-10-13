/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.kafka.streams;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Custom {@link Processor} implementation that is capable of sending a record
 * to a DLT if the processing fails.
 *
 * @author Soby Chacko
 * @since 4.1.0
 */
public class DltAwareProcessor<KIn, VIn, KOut, VOut> implements Processor<KIn, VIn, KOut, VOut> {

	/**
	 * Delegate {@link Function} that is responsible for processing the data.
	 */
	private final Function<Record<KIn, VIn>, Record<KOut, VOut>> delegateFunction;

	/**
	 * DLT destination.
	 */
	private String dltDestination;

	/**
	 * {@link DltPublishingContext} used for DLT publishing needs.
	 */
	private DltPublishingContext dltPublishingContext;

	/**
	 * A {@link BiConsumer} that does the recovery of a failed record.
	 */
	private BiConsumer<Record<KIn, VIn>, Exception> processorRecordRecoverer;

	/**
	 * {@link ProcessorContext} used in the processor.
	 */
	private ProcessorContext<KOut, VOut> context;

	/**
	 *
	 * @param delegateFunction {@link Function} to process the data
	 * @param dltDestination DLT destination
	 * @param dltPublishingContext {@link DltPublishingContext}
	 */
	public DltAwareProcessor(Function<Record<KIn, VIn>, Record<KOut, VOut>> delegateFunction, String dltDestination,
							DltPublishingContext dltPublishingContext) {
		this.delegateFunction = delegateFunction;
		Assert.isTrue(StringUtils.hasText(dltDestination), "DLT Destination topic must be provided.");
		this.dltDestination = dltDestination;
		Assert.notNull(dltPublishingContext, "DltSenderContext cannot be null");
		this.dltPublishingContext = dltPublishingContext;
	}

	/**
	 *
	 * @param delegateFunction {@link Function} to process the data
	 * @param processorRecordRecoverer {@link BiConsumer} that recovers failed records
	 */
	public DltAwareProcessor(Function<Record<KIn, VIn>, Record<KOut, VOut>> delegateFunction,
							BiConsumer<Record<KIn, VIn>, Exception> processorRecordRecoverer) {
		this.delegateFunction = delegateFunction;
		Assert.notNull(processorRecordRecoverer, "You must provide a valid processor recoverer");
		this.processorRecordRecoverer = processorRecordRecoverer;
	}

	@Override
	public void init(ProcessorContext<KOut, VOut> context) {
		Processor.super.init(context);
		this.context = context;
	}

	@Override
	public void process(Record<KIn, VIn> record) {
		try {
			Record<KOut, VOut> downstreamRecord = this.delegateFunction.apply(record);
			this.context.forward(downstreamRecord);
		}
		catch (Exception exception) {
			if (this.processorRecordRecoverer == null) {
				this.processorRecordRecoverer = defaultProcessorRecordRecoverer();
			}
			this.processorRecordRecoverer.accept(record, exception);
		}
	}

	@Override
	public void close() {
		Processor.super.close();
	}

	BiConsumer<Record<KIn, VIn>, Exception> defaultProcessorRecordRecoverer() {
		return (r, e) -> {
			StreamBridge streamBridge = this.dltPublishingContext.getStreamBridge();
			if (streamBridge != null) {
				Message<VIn> message = MessageBuilder.withPayload(r.value())
					.setHeader(KafkaHeaders.KEY, r.key()).build();
				streamBridge.send(this.dltDestination, message);
			}
		};
	}

}