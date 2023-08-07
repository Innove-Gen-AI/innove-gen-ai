/*
 * Copyright 2023 Innové Gen AI
 *
 */

package utils

import services.DatasetIngestionService

import javax.inject.Inject

class StartUpAction @Inject()(datasetIngestionService: DatasetIngestionService){
  datasetIngestionService.ingestDatasetFiles()
}
