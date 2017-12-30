package com.timzaak.coin.huobi.api.response

import com.timzaak.coin.huobi.api.entity.OrderType.OrderType

case class OrderMatchResultResponse(
  status:S,
  data:List[OrderMatchResult]
)

case class OrderMatchResult(
  id:L,
  `created-at`:L,
  `filled-amount`:S,
  `filled-fees`:S  ,
  `match-id`:L,
  `order-id`:L,
  price:L,
  source:S,
  symbol:S,
  `type`:OrderType
)
