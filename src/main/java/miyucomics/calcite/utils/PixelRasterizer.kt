package miyucomics.calcite.utils

import at.petrak.hexcasting.api.casting.math.HexPattern
import net.minecraft.util.math.Vec2f
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

data class PixelPos(val x: Int, val y: Int)

object PixelRasterizer {
	fun getNormalizedStrokes(pattern: HexPattern, flipHor: Boolean = false): List<Vec2f> {
		val lines = pattern.toLines(1f, pattern.getCenter(1f).negate()).toMutableList()
		val scaling = max(
			lines.maxBy { vector -> vector.x }.x - lines.minBy { vector -> vector.x }.x,
			lines.maxBy { vector -> vector.y }.y - lines.minBy { vector -> vector.y }.y
		)
		val xScale = if (flipHor) -1 else 1
		for (i in lines.indices)
			lines[i] = Vec2f(lines[i].x * xScale, -lines[i].y).multiply(1 / scaling)
		return lines.toList()
	}

	fun getLinePixels(points: List<Vec2f>, resolution: Int = 16): Set<PixelPos> {
		val pixels = mutableSetOf<PixelPos>()
		if (points.size < 2)
			return pixels
		val maxIdx = resolution - 1

		for (i in 0 until points.size - 1) {
			val start = points[i]
			val end = points[i + 1]

			val x0 = floor(start.x * maxIdx).toInt()
			val y0 = floor(start.y * maxIdx).toInt()
			val x1 = floor(end.x * maxIdx).toInt()
			val y1 = floor(end.y * maxIdx).toInt()

			rasterizeLine(x0, y0, x1, y1, pixels)
		}
		return pixels
	}

	private fun rasterizeLine(x0: Int, y0: Int, x1: Int, y1: Int, pixels: MutableSet<PixelPos>) {
		var x = x0
		var y = y0

		val dx = abs(x1 - x)
		val dy = -abs(y1 - y)
		val sx = if (x < x1) 1 else -1
		val sy = if (y < y1) 1 else -1
		var err = dx + dy

		while (true) {
			pixels.add(PixelPos(x, y))

			if (x == x1 && y == y1) break

			val e2 = 2 * err
			if (e2 >= dy) {
				err += dy
				x += sx
			}
			if (e2 <= dx) {
				err += dx
				y += sy
			}
		}
	}
}