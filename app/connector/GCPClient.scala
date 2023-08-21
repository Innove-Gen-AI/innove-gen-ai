/*
 * Copyright 2023 Innové Gen AI
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
 *
 */

package connector

import com.google.cloud.aiplatform.v1beta1._

trait GCPClient {
  private val API_ENDPOINT = "us-central1-aiplatform.googleapis.com:443"
  private val predictionServiceSettings: PredictionServiceSettings = PredictionServiceSettings.newBuilder.setEndpoint(API_ENDPOINT).build
  val predictionServiceClient: PredictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)
}

object GCPClient extends GCPClient