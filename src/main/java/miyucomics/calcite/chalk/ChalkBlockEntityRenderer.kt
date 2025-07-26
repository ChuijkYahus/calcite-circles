package miyucomics.calcite.chalk

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.client.render.PatternColors
import at.petrak.hexcasting.client.render.WorldlyPatternRenderHelpers
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

class ChalkBlockEntityRenderer : BlockEntityRenderer<ChalkBlockEntity> {
	override fun render(chalk: ChalkBlockEntity, tickDelta: Float, matrices: MatrixStack, vertices: VertexConsumerProvider, light: Int, overlay: Int) {
		val state = chalk.cachedState
		val nbt = chalk.iota
		val pattern = if (nbt.getString("hexcasting:type") == "hexcasting:pattern")
			HexPattern.fromNBT(nbt.getCompound("hexcasting:data"))
		else
			HexPattern.fromAngles("aa", HexDir.EAST)

		val onWall = state.get(ChalkBlock.ATTACH_FACE) == WallMountLocation.WALL
		val onCeiling = state.get(ChalkBlock.ATTACH_FACE) == WallMountLocation.CEILING
		val facing = state.get(ChalkBlock.FACING).horizontal

		matrices.push()

		var normal: Vec3d? = null
		if (onWall) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f))
			val translation = SLATE_FACINGS[facing % 4]
			matrices.translate(translation.x.toFloat(), translation.y.toFloat(), translation.z.toFloat())
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(WALL_ROTATIONS[facing % 4].toFloat()))
			normal = WALL_NORMALS[facing % 4]
		} else {
			val translation = SLATE_FLOORCEIL_FACINGS[facing % 4]
			matrices.translate(translation.x.toFloat(), translation.y.toFloat(), translation.z.toFloat())
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((facing * -90).toFloat()))
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((90 * (if (onCeiling) -1 else 1)).toFloat()))
			if (onCeiling) matrices.translate(0f, -1f, 1f)
		}

		WorldlyPatternRenderHelpers.renderPattern(pattern, WorldlyPatternRenderHelpers.WORLDLY_SETTINGS, PatternColors(0xff_737373.toInt(), 0xff_f0f0f0.toInt()), chalk.getPos().hashCode().toDouble(), matrices, vertices, normal, null, light, 1)
		matrices.pop()
	}

	companion object {
		val WALL_ROTATIONS = intArrayOf(180, 270, 0, 90)
		val SLATE_FACINGS = arrayOf(Vec3i(0, -1, 0), Vec3i(-1, -1, 0), Vec3i(-1, -1, 1), Vec3i(0, -1, 1))
		val WALL_NORMALS = arrayOf(Vec3d(0.0, 0.0, -1.0), Vec3d(-1.0, 0.0, 0.0), Vec3d(0.0, 0.0, -1.0), Vec3d(-1.0, 0.0, 0.0))
		val SLATE_FLOORCEIL_FACINGS = arrayOf(Vec3i(0, 0, 0), Vec3i(1, 0, 0), Vec3i(1, 0, 1), Vec3i(0, 0, 1))
	}
}