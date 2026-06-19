package com.capstone.smartbite

import android.content.Context
import android.net.Uri

internal class UserPreference(context: Context, email: String? = null) {
    companion object {
        private const val PREFS_NAME = "user_pref"
        private const val NAME = "name"
        private const val EMAIL = "email"
        private const val AGE = "age"
        private const val PHONE_NUMBER = "phone"
        private const val ADD = "alamat"
        private const val PROFILE_IMAGE_URI = "profile_image_uri"
        private const val GENDER = "gender"
        private const val HEIGHT = "height"
        private const val WEIGHT = "weight"
        private const val TARGET_WEIGHT = "target_weight"
        private const val GOAL = "goal"
        private const val ACTIVITY_LEVEL = "activity_level"
        private const val IS_ONBOARDING_FINISHED = "is_onboarding_finished"
        private const val IS_DARK_MODE = "is_dark_mode"
        private const val LAST_GOAL_RESET_DATE = "last_goal_reset_date"
        private const val LANGUAGE_CODE = "language_code"
    }

    private val preferences = context.getSharedPreferences(
        if (email != null) "${PREFS_NAME}_${email.replace(".", "_")}" else PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun setLanguage(langCode: String) {
        val editor = preferences?.edit()
        editor?.putString(LANGUAGE_CODE, langCode)
        editor?.apply()
    }

    fun getLanguage(): String {
        return preferences?.getString(LANGUAGE_CODE, "en") ?: "en"
    }

    fun setLastGoalResetDate(date: String) {
        val editor = preferences?.edit()
        editor?.putString(LAST_GOAL_RESET_DATE, date)
        editor?.apply()
    }

    fun getLastGoalResetDate(): String? {
        return preferences?.getString(LAST_GOAL_RESET_DATE, null)
    }

    fun setDarkMode(isEnabled: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(IS_DARK_MODE, isEnabled)
        editor.apply()
    }

    fun isDarkMode(): Boolean {
        return preferences.getBoolean(IS_DARK_MODE, false)
    }

    fun setUser(value: UserModel) {
        val editor = preferences.edit()
        editor.putString(NAME, value.name)
        editor.putString(EMAIL, value.email)
        editor.putInt(AGE, value.age)
        editor.putString(PHONE_NUMBER, value.phoneNumber)
        editor.putString(ADD, value.add)
        editor.putString(PROFILE_IMAGE_URI, value.profileImage?.toString())
        editor.putString(GENDER, value.gender)
        editor.putInt(HEIGHT, value.height)
        editor.putInt(WEIGHT, value.weight)
        editor.putInt(TARGET_WEIGHT, value.targetWeight)
        editor.putString(GOAL, value.goal)
        editor.putString(ACTIVITY_LEVEL, value.activityLevel)
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

    fun clearUser() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    fun getUser(): UserModel {
        val model = UserModel()
        model.name = preferences.getString(NAME, "")
        model.email = preferences.getString(EMAIL, "")
        model.age = preferences.getInt(AGE, 0)
        model.phoneNumber = preferences.getString(PHONE_NUMBER, "")
        model.add = preferences.getString(ADD, "")
        model.profileImage = preferences.getString(PROFILE_IMAGE_URI, null)?.let { Uri.parse(it) }
        model.gender = preferences.getString(GENDER, "")
        model.height = preferences.getInt(HEIGHT, 0)
        model.weight = preferences.getInt(WEIGHT, 0)
        model.targetWeight = preferences.getInt(TARGET_WEIGHT, 0)
        model.goal = preferences.getString(GOAL, null)
        model.activityLevel = preferences.getString(ACTIVITY_LEVEL, "")
        return model
    }
}
