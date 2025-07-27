package miyucomics.calcite.chalk

import miyucomics.calcite.CalciteMain
import miyucomics.calcite.wave.Wave
import miyucomics.calcite.wave.WaveManager
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.math.Direction

class ActivatorItem : Item(Settings().maxCount(1)) {
	override fun useOnBlock(context: ItemUsageContext): ActionResult {
		val world = context.world
		val pos = context.blockPos
		if (!world.getBlockState(pos).isOf(CalciteMain.CHALK_BLOCK))
			return ActionResult.FAIL
		if (world.isClient)
			return ActionResult.success(true)
		WaveManager.getManager(world as ServerWorld).waves.clear()
		WaveManager.getManager(world).waves.add(Wave(pos, mutableListOf(pos), mutableListOf(), 0L, Direction.UP, Direction.NORTH))
		return ActionResult.success(false)
	}
}
