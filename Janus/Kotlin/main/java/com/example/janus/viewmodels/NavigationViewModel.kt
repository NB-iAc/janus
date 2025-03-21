package com.example.janus.viewmodels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class NavigationViewModel : ViewModel() {
    val selectedDestination = MutableLiveData<String>()
}