package com.capstone.smartbite.ui.profil

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.capstone.smartbite.FormUserPreferenceActivity
import com.capstone.smartbite.Login.LoginActivity
import com.capstone.smartbite.R
import com.capstone.smartbite.UserModel
import com.capstone.smartbite.UserPreference
import com.capstone.smartbite.data.FirebaseService
import com.capstone.smartbite.databinding.FragmentProfilBinding
import com.capstone.smartbite.ui.kamera.getImageUri
import com.capstone.smartbite.utils.HealthMath
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class ProfilFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!



    private lateinit var mUserPreference: UserPreference
    private lateinit var userModel: UserModel

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth

    private var isPreferenceEmpty = false
    private var cameraImageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.data != null && result.resultCode == FormUserPreferenceActivity.RESULT_CODE) {
            userModel = result.data?.getParcelableExtra<UserModel>(FormUserPreferenceActivity.EXTRA_RESULT) as UserModel
            populateView(userModel)
            checkForm(userModel)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            cameraImageUri?.let { startCrop(it) }
        }
    }

    private val cropImageResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                userModel.profileImage = resultUri
                mUserPreference.setUser(userModel)
                
                // Sync to Cloud immediately
                lifecycleScope.launch {
                    try {
                        FirebaseService().saveUserProfile(userModel)
                    } catch (e: Exception) {
                        // Silent failure for sync, local is already updated
                    }
                }
                
                populateView(userModel)
                Toast.makeText(requireContext(), "Foto profil diperbarui", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Gagal memotong gambar: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().filesDir, "profile_crop_${System.currentTimeMillis()}.jpg"))

        val uCropIntent = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .getIntent(requireContext())

        cropImageResultLauncher.launch(uCropIntent)
    }

    private fun isGooglePhoto(uri: Uri?): Boolean {
        if (uri == null) return false
        val uriString = uri.toString()
        return uriString.contains("googleusercontent.com") || uriString.contains("google.com")
    }

    private fun showImageChoiceDialog() {
        val options = mutableListOf("Kamera", "Galeri")
        
        // Hanya munculkan "Hapus Foto" jika bukan foto Google dan profileImage tidak null
        if (userModel.profileImage != null && !isGooglePhoto(userModel.profileImage)) {
            options.add("Hapus Foto")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Foto Profil")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Kamera" -> {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    "Galeri" -> {
                        galleryLauncher.launch("image/*")
                    }
                    "Hapus Foto" -> {
                        userModel.profileImage = null
                        mUserPreference.setUser(userModel)
                        
                        // Sync removal to Cloud
                        lifecycleScope.launch {
                            try {
                                FirebaseService().saveUserProfile(userModel)
                            } catch (e: Exception) {
                                // Silent failure
                            }
                        }

                        populateView(userModel)
                        Toast.makeText(requireContext(), "Foto profil dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraImageUri = getImageUri(requireContext())
            cameraImageUri?.let { launcherIntentCamera.launch(it) }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(top = systemBars.top)
            insets
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        mUserPreference = UserPreference(requireContext(), mAuth.currentUser?.email)



        binding.ivEditProfile.setOnClickListener(this)
        binding.cardNutritionStrategy.setOnClickListener(this)
        binding.itemLanguage.setOnClickListener(this)
        binding.itemHelp.setOnClickListener(this)
        binding.itemDeleteAccount.setOnClickListener(this)

        // Switch listener
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.vNotifBg.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.badge_goal_bg)
                binding.ivNotifIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.badge_goal_text)
                Toast.makeText(requireContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show()
            } else {
                binding.vNotifBg.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.nav_inactive_gray)
                binding.ivNotifIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Set logout button click listener
        binding.logoutButton.setOnClickListener {
            signOutAndNavigateToSignIn()
        }
    }

    private fun signOutAndNavigateToSignIn() {
        mAuth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "gagal logout", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_account_title))
            .setMessage(getString(R.string.delete_account_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteAccount() {
        val email = mAuth.currentUser?.email
        mUserPreference.clearUser()
        
        lifecycleScope.launch {
            if (email != null) {
                try {
                    FirebaseService().deleteUserProfile(email)
                } catch (e: Exception) {
                    // Handle error
                }
            }
            signOutAndNavigateToSignIn()
            Toast.makeText(requireContext(), "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateView(userModel: UserModel) {
        val firebaseUser = mAuth.currentUser
        
        binding.tvName.text = userModel.name?.takeIf { it.isNotEmpty() } 
            ?: firebaseUser?.displayName ?: "Tidak Ada"
        
        binding.tvEmail.text = userModel.email?.takeIf { it.isNotEmpty() } 
            ?: firebaseUser?.email ?: "Tidak Ada"
            
        binding.tvGoal.text = userModel.goal ?: "Weight Loss Focus"
        
        binding.tvGenderValue.text = userModel.gender?.takeIf { it.isNotEmpty() } ?: "Belum Diatur"
        binding.tvAgeValue.text = if (userModel.age > 0) "${userModel.age} ${getString(R.string.years)}" else "28 ${getString(R.string.years)}"
        binding.tvWeightValue.text = if (userModel.weight > 0) "${userModel.weight} ${getString(R.string.kg)}" else "64 ${getString(R.string.kg)}"
        binding.tvHeightValue.text = if (userModel.height > 0) "${userModel.height} ${getString(R.string.cm)}" else "165 ${getString(R.string.cm)}"


        // Tampilkan gambar profil jika tersedia
        val photoUrl = userModel.profileImage ?: firebaseUser?.photoUrl
        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(R.drawable.th)
        }
    }

    private fun checkForm(userModel: UserModel) {
        isPreferenceEmpty = userModel.name.isNullOrEmpty()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.iv_edit_profile -> {
                showImageChoiceDialog()
            }

            R.id.card_nutrition_strategy -> {
                // TODO: Implement UpdateBodyMetricsActivity or link to FormUserPreferenceActivity
                // val intent = Intent(requireContext(), UpdateBodyMetricsActivity::class.java)
                // startActivity(intent)
                Toast.makeText(requireContext(), "Fitur Update Metrics akan segera hadir", Toast.LENGTH_SHORT).show()
            }

            R.id.item_language -> {
                Toast.makeText(requireContext(), "Language Settings", Toast.LENGTH_SHORT).show()
            }
            R.id.item_help -> {
                Toast.makeText(requireContext(), "Help Center", Toast.LENGTH_SHORT).show()
            }
            R.id.item_delete_account -> {
                showDeleteAccountConfirmation()
            }
        }
    }
}
