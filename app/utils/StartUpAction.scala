/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package utils

import services.DatasetIngestionService

import javax.inject.Inject

class StartUpAction @Inject()(datasetIngestionService: DatasetIngestionService){
  datasetIngestionService.ingestDatasetFiles()
}
