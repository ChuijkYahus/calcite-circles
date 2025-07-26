package miyucomics.calcite.chalk

import miyucomics.calcite.CalciteMain
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.util.math.BlockPos

class ChalkBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(CalciteMain.CHALK_BLOCK_ENTITY, pos, state) {
	var iota: NbtCompound = NbtCompound()

	override fun writeNbt(compound: NbtCompound) {
		compound.put("iota", iota)
	}

	override fun readNbt(compound: NbtCompound) {
		iota = compound.getCompound("iota")
	}

	override fun toInitialChunkDataNbt(): NbtCompound = createNbt()
	override fun toUpdatePacket(): BlockEntityUpdateS2CPacket = BlockEntityUpdateS2CPacket.create(this)
}