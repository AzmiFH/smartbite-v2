package com.capstone.smartbite.ui.profil

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.UserPreference

class ProfilViewModel : ViewModel() {

    private val _userModel = MutableLiveData<UserModel>()
    val userModel: LiveData<UserModel> = _userModel

    fun loadUser(context: Context, email: String?) {
        val userPreference = UserPreference(context, email)
        _userModel.value = userPreference.getUser()
    }
}
