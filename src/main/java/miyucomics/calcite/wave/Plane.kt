package miyucomics.calcite.wave

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.joml.Vector2d
import org.joml.Vector2i

enum class Plane {
	XY, XZ, YZ
}

fun Direction.toPlane() = when (this.axis) {
	Direction.Axis.X -> Plane.YZ
	Direction.Axis.Y -> Plane.XZ
	Direction.Axis.Z -> Plane.XY
}

fun Plane.projectVector(vector: Vec3d) = when (this) {
	Plane.YZ -> Vector2d(vector.y, vector.z)
	Plane.XZ -> Vector2d(vector.x, vector.z)
	Plane.XY -> Vector2d(vector.x, vector.y)
}

fun Plane.projectVector(vector: BlockPos) = when (this) {
	Plane.YZ -> Vector2i(vector.y, vector.z)
	Plane.XZ -> Vector2i(vector.x, vector.z)
	Plane.XY -> Vector2i(vector.x, vector.y)
}