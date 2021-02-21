package com.patson.util

import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model


object AirplaneModelCache {
  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  //val simpleCache: LoadingCache[Int, Option[Model]] = CacheBuilder.newBuilder.maximumSize(1000).build(new SimpleLoader(ModelSource.loadAllModels()))
  val allModels = ModelSource.loadAllModels().map(model => (model.id, model)).toMap

  def getModel(modelId : Int) : Option[Model] = {
    allModels.get(modelId)
  }


//  class SimpleLoader(models: List[Model]) extends CacheLoader[Int, Option[Model]] {
//    val modelLookup = models.map(model => (model.id, model)).toMap
//    override def load(modelId: Int) = {
//      modelLookup.get(modelId)
//    }
//  }

}



