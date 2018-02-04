package com.timzaak.coin.huobi.api.request

import com.timzaak.coin.huobi.api.entity.Period.Period

case class KlineRequest(
  symbol:S,
  period: Period,
  size:Int = 150
){
  assert(size>0 &&size<=2000, "size should be in [0, 2000]")
}