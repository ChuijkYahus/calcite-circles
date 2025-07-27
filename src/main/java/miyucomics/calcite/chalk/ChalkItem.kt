package miyucomics.calcite.chalk

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.utils.*
import miyucomics.calcite.CalciteMain
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent

class ChalkItem : Item(Settings().maxCount(1)), IotaHolderItem {
	override fun appendTooltip(stack: ItemStack, world: World?, list: MutableList<Text>, context: TooltipContext) {
		if (stack.containsTag("queue"))
			list.add("calcite.chalk.queue".asTranslatedComponent(IotaType.getDisplay(stack.getCompound("queue"))))
	}

	override fun useOnBlock(context: ItemUsageContext): ActionResult {
		val context = ItemPlacementContext(context)
		val pos = context.blockPos
		val world = context.world
		val stack = context.stack

		if (!context.canPlace())
			return ActionResult.FAIL
		if (!stack.containsTag("queue"))
			return ActionResult.FAIL
		val state = CalciteMain.CHALK_BLOCK.getPlacementState(context) ?: return ActionResult.FAIL
		if (world.isClient)
			return ActionResult.success(true)
		val list = (IotaType.deserialize(stack.getCompound("queue"), world as ServerWorld) as ListIota).list
		if (!list.nonEmpty)
			return ActionResult.FAIL
		if (!world.setBlockState(pos, state, 11))
			return ActionResult.FAIL


		val player = context.player

		val newState = world.getBlockState(pos)
		if (newState.isOf(CalciteMain.CHALK_BLOCK)) {
			(world.getBlockEntity(pos) as ChalkBlockEntity).iota = IotaType.serialize(list.car)
			writeDatum(stack, ListIota(list.cdr))
			if (player is ServerPlayerEntity)
				Criteria.PLACED_BLOCK.trigger(player, pos, stack)
		}

		world.playSound(player, pos, BlockSoundGroup.CALCITE.placeSound, SoundCategory.BLOCKS, (BlockSoundGroup.CALCITE.volume + 1.0f) / 2.0f, BlockSoundGroup.CALCITE.pitch * 0.8f)
		world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, newState))
		return ActionResult.success(false)
	}

	override fun writeable(stack: ItemStack) = true
	override fun readIotaTag(stack: ItemStack) = stack.getCompound("queue")
	override fun canWrite(stack: ItemStack, iota: Iota?) = iota is ListIota || iota == null
	override fun writeDatum(stack: ItemStack, iota: Iota?) {
		if (iota == null) {
			stack.remove("queue")
			return
		}
		stack.putCompound("queue", IotaType.serialize(iota))
	}
}
