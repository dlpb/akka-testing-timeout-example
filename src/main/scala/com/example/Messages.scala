package com.example


case object SellerToolsRequest
case object FileExchangeSubscriptionRequest

case class SellerToolsAggregatedResponse(storeType: Option[StoreType], fileExchangeSubscriptionStatus: SubscriptionStatus)
case object SellerToolsAggregationRequested

sealed trait StoreType
case object NextGenStoreType extends StoreType
sealed trait SubscriptionStatus
case object SubscribedToFileExchange extends SubscriptionStatus
case object NotSubscribedToFileExchange extends SubscriptionStatus
case object IndeterminateSubscriptionStatus extends SubscriptionStatus

case object TimeOut

case object Dependency1Request
case object Dependency2Request
case object Dependency1Response
case object Dependency2Response