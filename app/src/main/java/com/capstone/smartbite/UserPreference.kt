package com.capstone.smartbite

import android.content.Context
import android.net.Uri

internal class UserPreference(context: Context) {
    companion object {
        private const val PREFS_NAME = "user_pref"
        private const val NAME = "name"
        private const val EMAIL = "email"
        private const val AGE = "age"
        private const val PHONE_NUMBER = "phone"
        private const val ADD = "alamat"
        private const val PROFILE_IMAGE_URI = "profile_image_uri"
        private const val IS_ONBOARDING_FINISHED = "is_onboarding_finished"
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setUser(value: UserModel) {
        val editor = preferences.edit()
        editor.putString(NAME, value.name)
        editor.putString(EMAIL, value.email)
        editor.putInt(AGE, value.age)
        editor.putString(PHONE_NUMBER, value.phoneNumber)
        editor.putString(ADD, value.add)
        editor.putString(PROFILE_IMAGE_URI, value.profileImage?.toString())
        editor.apply()
    }

    fun setOnboardingFinished(isFinished: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(IS_ONBOARDING_FINISHED, isFinished)
        editor.apply()
    }

    fun isOnboardingFinished(): Boolean {
        return preferences.getBoolean(IS_ONBOARDING_FINISHED, false)
    }

    fun getUser(): UserModel {
        val model = UserModel()
        model.name = preferences.getString(NAME, "")
        model.email = preferences.getString(EMAIL, "")
        model.age = preferences.getInt(AGE, 0)
        model.phoneNumber = preferences.getString(PHONE_NUMBER, "")
        model.add = preferences.getString(ADD, "")
        model.profileImage = preferences.getString(PROFILE_IMAGE_URI, null)?.let { Uri.parse(it) }
        return model
    }
}
