package com.example.janus.map
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.janus.R
import com.example.janus.data.MapObject
import com.example.janus.data.MapObjectType
import com.example.janus.data.Repository
private const val TAG = "RoomDetailFragment"
class RoomDetailFragment : Fragment(R.layout.fragment_room_detail) {
    companion object {
        private const val ARG_ROOM_ID = "room_id"
        fun newInstance(roomId: String) = RoomDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ROOM_ID, roomId)
            }
        }
    }
    private var roomId: String? = null
    private lateinit var tvRoomDetail: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = arguments?.getString(ARG_ROOM_ID)
        Log.d(TAG, "RoomDetailFragment created for roomId: $roomId")
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvRoomDetail = view.findViewById(R.id.tvRoomDetail)
        val room = findRoomById(roomId)
        room?.let {
            Log.d(TAG, "Displaying details for room: ${it.name}")
            var contents = "Room Name: ${it.name}\n" +
                    "Name: ${it.name}\n" +
                    "Category: ${it.category}\n" +
                    "Floor: ${it.floor}\n"
            if (it.contactDetails.length>5) contents += "Contact Details: ${it.contactDetails}\n"
            tvRoomDetail.text = contents
        } ?: run {
            Log.d(TAG, "Room not found for roomId: $roomId")
            tvRoomDetail.text = "Room not found."
        }
    }
    private fun findRoomById(roomId: String?): MapObject? {
        if (roomId == null) return null
        for (floor in 1..3) {
            val rooms = Repository.getFloorMapObjects(floor)
            val room = rooms.find { it.objectType == MapObjectType.ROOM && it.roomId == roomId }
            if (room != null) return room
        }
        return null
    }
}
