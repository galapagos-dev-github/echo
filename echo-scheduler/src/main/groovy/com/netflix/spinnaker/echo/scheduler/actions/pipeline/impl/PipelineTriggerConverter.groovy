/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.echo.scheduler.actions.pipeline.impl

import com.netflix.scheduledactions.ActionInstance
import com.netflix.scheduledactions.triggers.CronTrigger
import com.netflix.spinnaker.echo.model.Pipeline
import com.netflix.spinnaker.echo.model.Trigger
import com.netflix.spinnaker.echo.pipelinetriggers.PipelineCache
import com.netflix.spinnaker.echo.scheduler.actions.pipeline.PipelineTriggerAction

class PipelineTriggerConverter {

  static Map<String, String> toParameters(Pipeline pipeline, Trigger trigger) {
    [
      id                   : pipeline.id,
      triggerId            : trigger.id,
      triggerType          : trigger.type,
      triggerCronExpression: trigger.cronExpression,
      triggerEnabled       : Boolean.toString(trigger.enabled)
    ]
  }

  static Pipeline fromParameters(PipelineCache pipelineCache, Map<String, String> parameters) {
    def trigger = Trigger
      .builder()
      .enabled(Boolean.parseBoolean(parameters.triggerEnabled))
      .id(parameters.triggerId)
      .type(Trigger.Type.CRON.toString())
      .cronExpression(parameters.triggerCronExpression)
      .build()

    def existingPipeline = pipelineCache.getPipelines().find { it.id == parameters.id }
    if (!existingPipeline) {
      throw new IllegalStateException("No pipeline found (id: ${parameters.id})")
    }
    return existingPipeline.withTrigger(trigger)
  }

  static ActionInstance toScheduledAction(Pipeline pipeline, Trigger trigger) {
    ActionInstance.ActionInstanceBuilder actionInstanceBuilder = ActionInstance.newActionInstance()
      .withId(trigger.id)
      .withName("Pipeline Trigger")
      .withGroup(pipeline.id)
      .withAction(PipelineTriggerAction)
      .withParameters(toParameters(pipeline, trigger))

    if (Trigger.Type.CRON.toString().equalsIgnoreCase(trigger.type)) {
      actionInstanceBuilder.withTrigger(new CronTrigger(trigger.cronExpression))
    }

    actionInstanceBuilder.build()
  }

  static boolean isInSync(ActionInstance actionInstance, Trigger trigger) {
    if (trigger.type == Trigger.Type.CRON.toString()) {
      return (
        actionInstance.trigger instanceof CronTrigger &&
          trigger.cronExpression == ((CronTrigger) actionInstance.trigger).cronExpression
      )
    }
    return true
  }
}
