package com.example.gymappv10

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedUserViewModel : ViewModel() {
    val name = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val phone = MutableLiveData<String>()
    val year = MutableLiveData<String>()
    val month = MutableLiveData<String>()
}
