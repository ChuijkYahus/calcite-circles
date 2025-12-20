package miyucomics.calcite.chalk

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import miyucomics.calcite.CalciteMain
import miyucomics.calcite.utils.PixelPos
import miyucomics.calcite.utils.PixelRasterizer
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.util.math.BlockPos

class ChalkBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(CalciteMain.CHALK_BLOCK_ENTITY, pos, state) {
	var iota: NbtCompound = NbtCompound()
	var pixelCache: Set<PixelPos> = setOf()

	override fun writeNbt(compound: NbtCompound) {
		compound.put("iota", iota)
	}

	override fun readNbt(compound: NbtCompound) {
		iota = compound.getCompound("iota")

		val pattern = if (iota.getString("hexcasting:type") == "hexcasting:pattern") {
			HexPattern.fromNBT(iota.getCompound("hexcasting:data"))
		} else
			HexPattern.fromAngles("aa", HexDir.EAST)
		pixelCache = PixelRasterizer.getLinePixels(PixelRasterizer.getNormalizedStrokes(pattern), 16)
	}

	override fun toInitialChunkDataNbt(): NbtCompound = createNbt()
	override fun toUpdatePacket(): BlockEntityUpdateS2CPacket = BlockEntityUpdateS2CPacket.create(this)
}