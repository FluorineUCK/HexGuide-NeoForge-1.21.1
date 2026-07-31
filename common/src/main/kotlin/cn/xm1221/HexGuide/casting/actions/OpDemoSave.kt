package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import cn.xm1221.HexGuide.demo.DemoData
import cn.xm1221.HexGuide.demo.DemoRecorder
import kotlin.io.path.Path

class OpDemoSave: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val list = args.getList(0,argc)
        DemoRecorder.save(
            ns = "hexguide",
            name = "test",
            duration = list.size().toDouble()*10,  // 自动算+1秒缓冲
            hexSize = 28f,
            pauses = doubleArrayOf(),  // 无暂停
            iotas = list.toList() as List<PatternIota>,
            resRoot = Path("resources")
        )
        return listOf()
    }
}