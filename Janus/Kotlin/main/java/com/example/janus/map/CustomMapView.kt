
package com.example.janus.map
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
import com.example.janus.data.Node
import com.example.janus.data.NodeCost
import com.example.janus.data.distance
import com.example.janus.data.heuristic
import java.util.PriorityQueue
private const val TAG = "CustomMapView"
class CustomMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var fromSelect: String? = null
    private var toSelect: String? = null
    fun setFromSelect(roomId: String) {
        fromSelect = roomId
        invalidate()
    }
    fun setToSelect(roomId: String) {
        toSelect = roomId
        invalidate()
    }
    private val floorObjects = mutableMapOf<Int, List<MapObject>>()
    private var currentFloor: Int = 1
    private var onRoomClickListener: OnRoomClickListener? = null
    private val graphNodes = mutableListOf<Node>()
    private var currentPath: MutableMap<Int, List<Node>?> = mutableMapOf()
    private var scaleFactor = 1.0f
    private var posX = 0f
    private var posY = 0f
    private var lastPosX = 0f
    private var lastPosY = 0f
    private var activePointerId = -1
    private val minZoom = 0.5f
    private val maxZoom = 3.0f
    fun setPos(x: Float?, y: Float?) {
        if (x != null && y != null) {
        posX = x
        posY = y
        invalidate()
            }
    }
    private val buildingPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val roomPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val selectedPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    val borderPaint = Paint().apply {
        color =  Color.rgb(0,0,139)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 4f, 4f, Color.LTGRAY)
    }
    private val textOutlinePaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val pathPaint = Paint().apply {
        color = Color.parseColor("#EDACC2")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minZoom, maxZoom)
                invalidate()
                return true
            }
        }
    )
    fun setFloorObjects(floor: Int, objects: List<MapObject>) {
        floorObjects[floor] = objects
        Log.d(TAG, "Set floor objects for floor=$floor, total=${objects.size}")
        invalidate()
    }
    fun setAllFloorMapObjects(objects: Map<Int, List<MapObject>>) {
        floorObjects.clear()
        floorObjects.putAll(objects)
        Log.d(TAG, "Set all floor objects. Total: ${objects.size}")
        invalidate()
    }
    fun setGraphNodes(nodes: List<Node>) {
        graphNodes.clear()
        graphNodes.addAll(nodes)
        Log.d(TAG, "Graph nodes loaded. Total: ${nodes.size}")
    }
    fun decrementCurrentFloor() {
        Log.e(TAG, floorObjects.size.toString())
        floorObjects[currentFloor]?.let {
            if (currentFloor - 1 in floorObjects) {
                currentFloor--
                invalidate()
            }
        }
    }
    fun incrementCurrentFloor() {
        Log.e(TAG, floorObjects.size.toString())
        floorObjects[currentFloor]?.let {
            if (currentFloor + 1 in floorObjects) {
                currentFloor++
                invalidate()
            }
        }
    }
    fun setCurrentFloor(floor: Int) {
        Log.d(TAG, "Changing floor from $currentFloor to $floor")
        currentFloor = floor
        invalidate()
    }
    fun getCurrentFloor(): Int {
        return currentFloor
    }
    fun setOnRoomClickListener(listener: OnRoomClickListener) {
        this.onRoomClickListener = listener
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw called. currentFloor=$currentFloor, scaleFactor=$scaleFactor")
        canvas.withScale(scaleFactor, scaleFactor) {
            canvas.withTranslation(posX, posY) {
                val objectsOnCurrentFloor = floorObjects[currentFloor] ?: emptyList()
                if (objectsOnCurrentFloor.isEmpty()) {
                    Log.d(TAG, "No objects found for floor=$currentFloor")
                }
                objectsOnCurrentFloor.filter { it.objectType == MapObjectType.BUILDING }.forEach { obj ->
                    drawPolygon(obj, this, buildingPaint)
                }
                objectsOnCurrentFloor.filter { it.objectType != MapObjectType.BUILDING }.forEach { obj ->
                    Log.d(TAG, "Drawing object: name=${obj.name}, type=${obj.objectType}, floor=${obj.floor}")
                    when (obj.objectType) {
                        MapObjectType.ROOM -> {
                            val paint = roomPaint.apply { color = Color.BLUE }
                            drawRoom(obj, this, paint)
                        }
                        MapObjectType.ELEVATOR,
                        MapObjectType.STAIR,
                        MapObjectType.ESCALATOR -> {
                            drawTransition(obj, this)
                        }
                        else -> {
                        }
                    }
                }
                currentPath?.let { path ->
                    path[currentFloor]?.let {
                        drawDirectionalPathOnFloor(this, it, currentFloor)
                    }
                }
            }
        }
    }
    private fun drawPathOnFloor(canvas: Canvas, path: List<Node>, floor: Int) {
        for (i in 0 until path.size - 1) {
            val n1 = path[i]
            val n2 = path[i + 1]
            if (n1.floor == floor){
                canvas.drawLine(n1.position.x, n1.position.y,
                    n2.position.x, n2.position.y,
                    pathPaint)
            }
        }
    }
    private fun drawRoom(obj: MapObject, canvas: Canvas, paint: Paint) {
        val pts = obj.points
        if (pts.size < 3) return
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                lineTo(pts[i].x, pts[i].y)
            }
            close()
        }
        if (obj.roomId == fromSelect) {
            paint.color = Color.parseColor("#E37D7B")
            textPaint.color = Color.BLACK
        } else if (obj.roomId == toSelect) {
            paint.color = Color.parseColor("#64A667")
            textPaint.color = Color.BLACK
        } else {
            paint.color = Color.BLUE
            textPaint.color = Color.BLACK
        }
        canvas.drawPath(path, paint)
        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }
        val maxY = pts.maxOf { it.y }
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        drawFittedText(
            canvas = canvas,
            text = obj.name,
            boundingBoxWidth = maxX - minX,
            boundingBoxHeight = maxY - minY,
            centerX = centerX,
            centerY = centerY,
            textPaint = textPaint
        )
    }
    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        boundingBoxWidth: Float,
        boundingBoxHeight: Float,
        centerX: Float,
        centerY: Float,
        textPaint: Paint,
        maxTextSize: Float = 40f
    ) {
        val initialTextSize = (boundingBoxWidth.coerceAtMost(boundingBoxHeight) / 5).coerceAtMost(maxTextSize)
        textPaint.textSize = initialTextSize
        var displayText = text
        var textWidth = textPaint.measureText(displayText)
        if (textWidth > boundingBoxWidth * 0.85) {
            val scaleFactor = boundingBoxWidth * 0.85f / textWidth
            val newTextSize = (textPaint.textSize * scaleFactor).coerceAtLeast(8f)
            textPaint.textSize = newTextSize
            textWidth = textPaint.measureText(displayText)
            if (textWidth > boundingBoxWidth * 0.85) {
                var textLength = text.length
                while (textWidth > boundingBoxWidth * 0.85 && textLength > 3) {
                    textLength--
                    displayText = text.substring(0, textLength - 2) + ".."
                    textWidth = textPaint.measureText(displayText)
                }
            }
        }
        val textHeight = textPaint.descent() - textPaint.ascent()
        val textOffset = textHeight / 2 - textPaint.descent()
        val adjustedY = centerY + textOffset
        canvas.drawText(displayText, centerX, adjustedY, textPaint)
    }
    private fun drawPolygon(obj: MapObject, canvas: Canvas, paint: Paint) {
        val pts = obj.points
        if (pts.size < 3) return
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                lineTo(pts[i].x, pts[i].y)
            }
            close()
        }
        canvas.drawPath(path, paint)
    }
    private fun drawTransition(obj: MapObject, canvas: Canvas) {
        val center = calculateCentroid(obj.points) ?: return
        Log.d(TAG, "Drawing transition: ${obj.name} at center=($center)")
        val transitionPaint:Paint;
        val pts = obj.points
        if (pts.size < 3) return
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                lineTo(pts[i].x, pts[i].y)
            }
            close()
        }
        when (obj.objectType) {
            MapObjectType.ELEVATOR -> {
                transitionPaint = Paint().apply {
                    color = Color.rgb(173, 216, 230)
                    style = Paint.Style.FILL
                }
                canvas.drawPath(path, borderPaint)
                canvas.drawPath(path, transitionPaint)
            }
            MapObjectType.ESCALATOR -> {
                transitionPaint = Paint().apply {
                    color = Color.rgb(255, 165, 0)
                }
                canvas.drawPath(path, transitionPaint)
            }
            MapObjectType.STAIR -> {
                transitionPaint = Paint().apply {
                    color = Color.rgb(165, 42, 42)
                }
                canvas.drawPath(path, transitionPaint)
            }
            MapObjectType.BUILDING, MapObjectType.ROOM, MapObjectType.NODE -> {
                Log.d(TAG, "Skipping transition for ${obj.objectType}")
                return
            }
            else -> {
                Log.w(TAG, "Unhandled MapObjectType: ${obj.objectType}")
                return
            }
        }
    }
    private fun calculateCentroid(points: List<PointF>): PointF {
        var centroidX = 0f
        var centroidY = 0f
        var signedArea = 0f
        for (i in points.indices) {
            val x0 = points[i].x
            val y0 = points[i].y
            val x1 = points[(i + 1) % points.size].x
            val y1 = points[(i + 1) % points.size].y
            val a = x0 * y1 - x1 * y0
            signedArea += a
            centroidX += (x0 + x1) * a
            centroidY += (y0 + y1) * a
        }
        signedArea *= 0.5f
        if (signedArea == 0f) {
            val avgX = points.map { it.x }.average().toFloat()
            val avgY = points.map { it.y }.average().toFloat()
            return PointF(avgX, avgY)
        }
        centroidX /= (6f * signedArea)
        centroidY /= (6f * signedArea)
        return PointF(centroidX, centroidY)
    }
    private fun applyPanningConstraints() {
    }
    private fun detectRoomClick(x: Float, y: Float) {
        val objectsOnCurrentFloor = floorObjects[currentFloor] ?: emptyList()
        for (i in objectsOnCurrentFloor.size - 1 downTo 0) {
            val obj = objectsOnCurrentFloor[i]
            if (obj.objectType == MapObjectType.ROOM) {
                if (isInsidePolygon(obj.points, x, y)) {
                    Log.d(TAG, "Room clicked: ${obj.name}, ID=${obj.roomId}")
                    onRoomClickListener?.onRoomClicked(obj)
                    return
                }
            } else if (obj.objectType== MapObjectType.STAIR || obj.objectType== MapObjectType.ELEVATOR || obj.objectType== MapObjectType.ESCALATOR) {
                if (isInsidePolygon(obj.points, x, y)) {
                    Log.d(TAG, "Transition clicked: ${obj.name}")
                    onRoomClickListener?.onRoomClicked(obj)
                    return
                }
            }
        }
    }
    private fun isInsidePolygon(points: List<PointF>, x: Float, y: Float): Boolean {
        var count = 0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            val xi = points[i].x
            val yi = points[i].y
            val xj = points[j].x
            val yj = points[j].y
            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) count++
        }
        return (count % 2 == 1)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = (event.x / scaleFactor - posX)
                val touchY = (event.y / scaleFactor - posY)
                detectRoomClick(touchX, touchY)
                lastPosX = event.x
                lastPosY = event.y
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastPosX
                    val dy = event.y - lastPosY
                    posX += dx / scaleFactor
                    posY += dy / scaleFactor
                    applyPanningConstraints()
                    invalidate()
                    lastPosX = event.x
                    lastPosY = event.y
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastPosX = event.getX(newPointerIndex)
                    lastPosY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }
    fun findShortestPath(start: Node, end: Node): Pair<List<Node>, Float>? {
        val openSet = PriorityQueue<NodeCost>(compareBy { it.estimatedTotalCost })
        openSet.add(
            NodeCost(
                node = start,
                costSoFar = 0f,
                estimatedTotalCost = heuristic(start, end),
                path = listOf(start)
            )
        )
        val costSoFar = mutableMapOf<Node, Float>()
        costSoFar[start] = 0f
        val visited = mutableSetOf<Node>()
        while (openSet.isNotEmpty()) {
            val current = openSet.poll()
            if (current.node == end) {
                return Pair(current.path, current.costSoFar)
            }
            if (!visited.add(current.node)) {
                continue
            }
            for (neighbor in current.node.neighbors) {
                if (neighbor.floor != current.node.floor) {
                    if (!current.node.isElevationNode && !neighbor.isElevationNode) {
                        continue
                    }
                }
                val newCost = costSoFar[current.node]!! + distance(current.node, neighbor)
                if (newCost < costSoFar.getOrDefault(neighbor, Float.MAX_VALUE)) {
                    costSoFar[neighbor] = newCost
                    val priority = newCost + heuristic(neighbor, end)
                    val newPath = current.path + neighbor
                    openSet.add(NodeCost(neighbor, newCost, priority, newPath))
                }
            }
        }
        return null
    }
    fun showPathBetweenNodes(startNode: Node, endNode: Node) {
        val path = findShortestPath(startNode, endNode)
        if (path != null) {
            Log.d(TAG, "Path found: ${path.first.map { it.id }}")
        } else {
            Log.d(TAG, "No path found between $startNode and $endNode")
            toSelect = null;
            fromSelect=null;
        }
        if (path != null) {
            currentPath.clear()
            currentPath.set(currentFloor,path.first)
        } else {
            currentPath.clear()
        }
        invalidate()
    }
    fun showPathBetweenNodesDifferentElevation(startNode: Node, endNode: Node, startFloor: Int, endFloor: Int) {
        Log.d(TAG, "Finding path between floors: $startFloor → $endFloor")
        currentPath.clear()
        val completePath = findMultiFloorPath(startNode, endNode, startFloor, endFloor)
        if (completePath.isEmpty()) {
            Log.d(TAG, "No path found between $startNode and $endNode across floors $startFloor to $endFloor")
            return
        }
        currentPath.putAll(completePath)
        Log.d(TAG, "Path found across ${completePath.size} floors from $startFloor to $endFloor")
        if (startNode.id.isNotEmpty()) {
            setFromSelect(startNode.id)
        }
        if (endNode.id.isNotEmpty()) {
            setToSelect(endNode.id)
        }
        invalidate()
    }
    private fun findMultiFloorPath(
        startNode: Node,
        endNode: Node,
        startFloor: Int,
        endFloor: Int
    ): Map<Int, List<Node>> {
        Log.d(TAG, "Starting pathfinding from '${startNode.id}' to '${endNode.id}' (floors $startFloor → $endFloor)")
        if (startFloor == endFloor) {
            val path = findShortestPath(startNode, endNode)
            return if (path != null) {
                Log.d(TAG, "Same-floor path found with ${path.first.size} nodes:")
                logPathDetails(path.first, startFloor)
                mapOf(startFloor to path.first)
            } else {
                Log.e(TAG, "No path found between '${startNode.id}' and '${endNode.id}' on floor $startFloor")
                emptyMap()
            }
        }
        val visitedFloors = mutableSetOf<Int>()
        val result = findPathBetweenFloors(startNode, endNode, startFloor, endFloor, visitedFloors)
        if (result.isNotEmpty()) {
            Log.d(TAG, "Multi-floor path found across ${result.size} floors:")
            var totalNodes = 0
            result.forEach { (floor, nodes) ->
                totalNodes += nodes.size
                logPathDetails(nodes, floor)
            }
            Log.d(TAG, "Total path length: $totalNodes nodes")
        } else {
            Log.e(TAG, "No path found between '${startNode.id}' and '${endNode.id}' across floors")
        }
        return result
    }
    private fun logPathDetails(nodes: List<Node>, floor: Int) {
        Log.d(TAG, "Floor $floor path segment (${nodes.size} nodes):")
        if (nodes.isEmpty()) {
            Log.d(TAG, "  Empty path segment")
            return
        }
        val pathStr = nodes.joinToString(" → ") { it.id }
        Log.d(TAG, "  Path: $pathStr")
        val elevationNodes = nodes.filter { it.isElevationNode }
        if (elevationNodes.isNotEmpty()) {
            val elevationStr = elevationNodes.joinToString(", ") { it.id }
            Log.d(TAG, "  Elevation nodes: $elevationStr")
        }
        var distance = 0f
        for (i in 0 until nodes.size - 1) {
            distance += distance(nodes[i], nodes[i + 1])
        }
        Log.d(TAG, "  Segment distance: ${String.format("%.2f", distance)} units")
    }
    private fun findPathBetweenFloors(
        currentNode: Node,
        targetNode: Node,
        currentFloor: Int,
        targetFloor: Int,
        visitedFloors: MutableSet<Int>
    ): Map<Int, List<Node>> {
        visitedFloors.add(currentFloor)
        Log.d(TAG, "Exploring floor $currentFloor (visited floors: $visitedFloors)")
        if (currentFloor == targetFloor) {
            val directPath = findShortestPath(currentNode, targetNode)
            return if (directPath != null) {
                mapOf(currentFloor to directPath.first)
            } else {
                emptyMap()
            }
        }
        val elevationNodesOnCurrentFloor = graphNodes.filter {
            it.isElevationNode && it.floor == currentFloor
        }
        if (elevationNodesOnCurrentFloor.isEmpty()) {
            Log.d(TAG, "No elevation nodes found on floor $currentFloor")
            return emptyMap()
        }
        val allPossiblePaths = mutableListOf<Pair<Map<Int, List<Node>>, Float>>()
        for (elevNode in elevationNodesOnCurrentFloor) {
            val pathToElevNode = findShortestPath(currentNode, elevNode)
            if (pathToElevNode == null) {
                Log.d(TAG, "No path from $currentNode to elevation node $elevNode on floor $currentFloor")
                continue
            }
            val connectedElevNodes = elevNode.neighbors.filter {
                it.isElevationNode && it.floor != currentFloor && !visitedFloors.contains(it.floor)
            }
            for (connectedNode in connectedElevNodes) {
                val connectedFloor = connectedNode.floor
                Log.d(TAG, "Trying connection from floor $currentFloor to floor $connectedFloor via ${elevNode.id} → ${connectedNode.id}")
                val remainingPath = findPathBetweenFloors(
                    connectedNode,
                    targetNode,
                    connectedFloor,
                    targetFloor,
                    visitedFloors.toMutableSet()
                )
                if (remainingPath.isNotEmpty()) {
                    var totalCost = pathToElevNode.second
                    remainingPath.forEach { (_, nodes) ->
                        if (nodes.size > 1) {
                            for (i in 0 until nodes.size - 1) {
                                totalCost += distance(nodes[i], nodes[i + 1])
                            }
                        }
                    }
                    val combinedPath = remainingPath.toMutableMap()
                    combinedPath[currentFloor] = pathToElevNode.first
                    allPossiblePaths.add(Pair(combinedPath, totalCost))
                    Log.d(TAG, "Found a valid multi-floor path with cost $totalCost")
                }
            }
        }
        return if (allPossiblePaths.isNotEmpty()) {
            val bestPath = allPossiblePaths.minByOrNull { it.second }!!
            Log.d(TAG, "Selected best path with cost ${bestPath.second}")
            bestPath.first
        } else {
            Log.d(TAG, "No valid paths found from floor $currentFloor to $targetFloor")
            emptyMap()
        }
    }
    fun focusOnRoomById(roomId: String) {
        val objectsOnCurrentFloor = floorObjects[currentFloor] ?: emptyList()
        val room = objectsOnCurrentFloor.find { it.objectType == MapObjectType.ROOM && it.roomId == roomId }
        if (room != null) {
            Log.d(TAG, "Focusing on room: ${room.name}, ID=${room.roomId}")
            val centroid = calculateCentroid(room.points)
            Log.d(TAG, "Room centroid: (${centroid.x}, ${centroid.y})")
            val canvasCenterX = width / 2f
            val canvasCenterY = height / 2f
            posX = (canvasCenterX / scaleFactor) - centroid.x
            posY = (canvasCenterY / scaleFactor) - centroid.y
            applyPanningConstraints()
            Log.d(TAG, "Updated posX: $posX, posY: $posY")
            invalidate()
        } else {
            Log.d(TAG, "Room with ID $roomId not found on floor $currentFloor")
        }
    }
    private fun drawDirectionalPathOnFloor(canvas: Canvas, path: List<Node>, floor: Int) {
        if (path.size < 2) return
        val linePaint = Paint(pathPaint)
        linePaint.strokeWidth = 5f
        val arrowPaint = Paint(pathPaint)
        arrowPaint.color =Color.parseColor("#CC879D")
        arrowPaint.style = Paint.Style.FILL
        arrowPaint.strokeWidth = 8f
        val dotPaint = Paint(pathPaint)
        val smoothPath = Path()
        dotPaint.style = Paint.Style.FILL
        smoothPath.moveTo(path[0].position.x, path[0].position.y)
        canvas.drawCircle(path[0].position.x, path[0].position.y, 8f, dotPaint)
        for (i in 0 until path.size - 1) {
            val currentNode = path[i]
            val nextNode = path[i + 1]
            if (floor != currentNode.floor || floor != nextNode.floor) {
                continue
            }
            smoothPath.lineTo(nextNode.position.x, nextNode.position.y)
            val midX = (currentNode.position.x + nextNode.position.x) / 2
            val midY = (currentNode.position.y + nextNode.position.y) / 2
            val dx = nextNode.position.x - currentNode.position.x
            val dy = nextNode.position.y - currentNode.position.y
            val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
            drawArrow(canvas, midX, midY, angle, arrowPaint)
            val radius = if (i == path.size - 2) 8f else 4f
            canvas.drawCircle(nextNode.position.x, nextNode.position.y, radius, dotPaint)
        }
        canvas.drawPath(smoothPath, linePaint)
        addPathDirections(canvas, path, floor)
    }
    private fun drawArrow(canvas: Canvas, x: Float, y: Float, angle: Float, paint: Paint) {
        val arrowLength = 16f
        val arrowWidth = 10f
        val arrowPath = Path()
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        arrowPath.moveTo(arrowLength / 2, 0f)
        arrowPath.lineTo(-arrowLength / 2, -arrowWidth / 2)
        arrowPath.lineTo(-arrowLength / 2, arrowWidth / 2)
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)
        canvas.restore()
    }
    private fun addPathDirections(canvas: Canvas, path: List<Node>, floor: Int) {
        if (path.size < 3) return
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(3f, 1f, 1f, Color.WHITE)
        }
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            alpha = 180
            style = Paint.Style.FILL
        }
        for (i in 1 until path.size - 1) {
            val prevNode = path[i - 1]
            val currentNode = path[i]
            val nextNode = path[i + 1]
            if (currentNode.floor != floor) continue
            if (currentNode.isElevationNode) {
                var direction = "Use "
                direction += when {
                    currentNode.id.contains("E", ignoreCase = true) -> "Elevator"
                    currentNode.id.contains("S", ignoreCase = true) -> "Stairs"
                    else -> "Transition"
                }
                if (i < path.size - 1 && nextNode.floor != currentNode.floor) {
                    direction += " to Floor ${nextNode.floor}"
                }
                continue
            }
            val v1x = currentNode.position.x - prevNode.position.x
            val v1y = currentNode.position.y - prevNode.position.y
            val v2x = nextNode.position.x - currentNode.position.x
            val v2y = nextNode.position.y - currentNode.position.y
            val angle1 = Math.atan2(v1y.toDouble(), v1x.toDouble())
            val angle2 = Math.atan2(v2y.toDouble(), v2x.toDouble())
            var angleDiff = Math.toDegrees(angle2 - angle1).toFloat()
            while (angleDiff > 180) angleDiff -= 360
            while (angleDiff < -180) angleDiff += 360
            if (Math.abs(angleDiff) > 30) {
                val direction = when {
                    angleDiff > 30 && angleDiff < 150 -> "Turn Right"
                    angleDiff >= 150 -> "Turn Around"
                    angleDiff < -30 && angleDiff > -150 -> "Turn Left"
                    angleDiff <= -150 -> "Turn Around"
                    else -> null
                }
                if (direction != null) {
                }
            }
        }
    }
    private fun drawTextWithBackground(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textPaint: Paint,
        backgroundPaint: Paint
    ) {
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val padding = 10f
        val rect = RectF(
            x - textBounds.width() / 2 - padding,
            y - textBounds.height() - padding,
            x + textBounds.width() / 2 + padding,
            y + padding
        )
        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)
        canvas.drawText(text, x, y, textPaint)
    }
}
