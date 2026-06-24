import { http } from './http'
import type { BaseResponse } from '@/types/analysis'

export interface AptDeal {
  regionCode: string
  dong:       string
  aptName:    string
  jibun:      string
  dealYear:   number
  dealMonth:  number
  dealDay:    number
  area:       number
  floor:      number
  dealAmount: number
  buildYear:  number
}

export async function getAptDeals(
  regionCode: string,
  yearMonth: string,
): Promise<BaseResponse<AptDeal[]>> {
  const res = await http.get<BaseResponse<AptDeal[]>>('/deals/apt', {
    params: { regionCode, yearMonth },
  })
  return res.data
}
