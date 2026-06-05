package com.capstone.smartbite

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel (
    var profileImage: Uri? = null,
    var name: String? = null,
    var email: String? = null,
    var age: Int = 0,
    var phoneNumber: String? = null,
    var add : String? = null,
    var gender: String? = null,
    var height: Int = 0,
    var weight: Int = 0,
    var targetWeight: Int = 0,
    var goal: String? = "Weight Loss Focus",
    var activityLevel: String? = null
) : Parcelable