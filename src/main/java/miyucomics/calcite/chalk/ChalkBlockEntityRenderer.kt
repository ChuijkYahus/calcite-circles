package miyucomics.calcite.chalk

import net.minecraft.block.BlockState
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

class ChalkBlockEntityRenderer : BlockEntityRenderer<ChalkBlockEntity> {
	override fun render(chalk: ChalkBlockEntity, tickDelta: Float, matrices: MatrixStack, vertices: VertexConsumerProvider, light: Int, overlay: Int) {
		val pixelSize = 1 / 16f
		matrices.push()
		setUpTransformations(chalk.cachedState, matrices)
		matrices.translate(0f, 0f, -0.01f)
		val consumer = vertices.getBuffer(RenderLayer.getCutout())
		chalk.pixelCache.forEach { pixel ->
			val x0 = pixel.x * pixelSize
			val y0 = pixel.y * pixelSize
			val x1 = (pixel.x + 1) * pixelSize
			val y1 = (pixel.y + 1) * pixelSize
			drawQuad(consumer, matrices, x0, y0, x1, y1, light)
		}
		matrices.pop()
	}

	private fun drawQuad(consumer: VertexConsumer, matrices: MatrixStack, x0: Float, y0: Float, x1: Float, y1: Float, light: Int) {
		val position = matrices.peek().positionMatrix
		val normal = matrices.peek().normalMatrix
		consumer.vertex(position, x0, y0, 0f).color(255, 255, 255, 255).texture(0f, 0f).light(light).normal(normal, 0f, 0f, 1f).next()
		consumer.vertex(position, x0, y1, 0f).color(255, 255, 255, 255).texture(0f, 1f).light(light).normal(normal, 0f, 0f, 1f).next()
		consumer.vertex(position, x1, y1, 0f).color(255, 255, 255, 255).texture(1f, 1f).light(light).normal(normal, 0f, 0f, 1f).next()
		consumer.vertex(position, x1, y0, 0f).color(255, 255, 255, 255).texture(1f, 0f).light(light).normal(normal, 0f, 0f, 1f).next()
	}

	private fun setUpTransformations(state: BlockState, matrices: MatrixStack) {
		val onWall = state.get(ChalkBlock.ATTACH_FACE) == WallMountLocation.WALL
		val onCeiling = state.get(ChalkBlock.ATTACH_FACE) == WallMountLocation.CEILING
		val facing = state.get(ChalkBlock.FACING).horizontal

		if (onWall) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f))
			val translation = SLATE_FACINGS[facing % 4]
			matrices.translate(translation.x.toFloat(), translation.y.toFloat(), translation.z.toFloat())
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(WALL_ROTATIONS[facing % 4].toFloat()))
		} else {
			val translation = SLATE_FLOORCEIL_FACINGS[facing % 4]
			matrices.translate(translation.x.toFloat(), translation.y.toFloat(), translation.z.toFloat())
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((facing * -90).toFloat()))
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((90 * (if (onCeiling) -1 else 1)).toFloat()))
			if (onCeiling)
				matrices.translate(0f, -1f, 1f)
		}
	}

	companion object {
		val WALL_ROTATIONS = intArrayOf(180, 270, 0, 90)
		val SLATE_FACINGS = arrayOf(Vec3i(0, -1, 0), Vec3i(-1, -1, 0), Vec3i(-1, -1, 1), Vec3i(0, -1, 1))
		val SLATE_FLOORCEIL_FACINGS = arrayOf(Vec3i(0, 0, 0), Vec3i(1, 0, 0), Vec3i(1, 0, 1), Vec3i(0, 0, 1))
	}
}