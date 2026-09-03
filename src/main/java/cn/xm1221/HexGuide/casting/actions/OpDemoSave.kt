package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.HexGuide.demo.DemoGenerator

/** 取栈上 ListIota → 生成并保存演示配置文件，压入引用字符串（ns:name） */
class OpDemoSave : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment,
    ): List<Iota> {
        val list = args.getList(0, argc).toList()
        val name = "demo_${System.currentTimeMillis()}"
        val ref = DemoGenerator.save(list, name)
        return listOf()
    }
}