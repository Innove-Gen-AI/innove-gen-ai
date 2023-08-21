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

package utils

import com.google.api.core.ApiFuture
import play.api.Logging

import java.util.concurrent.{CompletableFuture, ExecutionException, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.ExecutionContext

trait FutureHelper extends Logging {

  def toCompletableFuture[T](apiFuture: ApiFuture[T]): CompletableFuture[T] = {

    val future = new CompletableFuture[T]
    apiFuture.addListener(
      () => {
        try {
          future.complete(apiFuture.get)
        } catch {
          case ex@(_: InterruptedException | _: ExecutionException) =>
            logger.error(ex.getMessage)
            future.completeExceptionally(ex)
        }
      }, ExecutionContext.global
    )
    future
  }
}
