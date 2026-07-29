package cn.xm1221.HexGuide.compat.inline

import com.samsthenerd.inline.api.InlineAPI
import com.samsthenerd.inline.api.client.InlineClientAPI

object InlineHexGuide {
    fun init(){
        InlineAPI.INSTANCE.addDataType(IotaInlineDataType.INSTANCE)
    }
}