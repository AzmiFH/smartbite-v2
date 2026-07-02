package com.capstone.smartbite.ui.kamera

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.capstone.smartbite.R
import com.capstone.smartbite.data.ApiConfig
import com.capstone.smartbite.data.FileUploadResponse
import com.capstone.smartbite.databinding.FragmentCameraBinding
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.tensorflow.lite.task.vision.classifier.Classifications
import retrofit2.HttpException
import java.io.File

class CameraFragment : Fragment(){

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!


    private var currentImageUri: Uri? = null
    private var imageUri: Uri? = null
    private var isGalleryScan: Boolean = false

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pulihkan data jika ada
        savedInstanceState?.let {
            currentImageUri = it.getParcelable(CURRENT_IMAGE_URI_KEY)
            imageUri = it.getParcelable(IMAGE_URI_KEY)
            isGalleryScan = it.getBoolean(IS_GALLERY_SCAN_KEY, false)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(top = systemBars.top)
            insets
        }

        startScanningAnimation()

        // Set klik listener
        binding.btnGallery.setOnClickListener { startGallery() }
        binding.btnReset.setOnClickListener { startCamera() } // Re-take photo
        binding.buttonAnalisa.setOnClickListener { uploadImage() }

        // Tampilkan gambar yang sudah ada
        imageUri?.let {
            binding.gambaruplode.setImageURI(it)
            binding.buttonAnalisa.visibility = View.VISIBLE
        } ?: run {
            binding.gambaruplode.setImageResource(R.drawable.baseline_image_24)
            binding.buttonAnalisa.visibility = View.GONE
        }

        return binding.root
    }

    private fun startScanningAnimation() {
        binding.scanLine.post {
            val parentHeight = binding.scanAreaContainer.height.toFloat()
            val animation = ObjectAnimator.ofFloat(
                binding.scanLine,
                "translationY",
                0f,
                parentHeight - binding.scanLine.height
            )
            animation.duration = 2000
            animation.interpolator = LinearInterpolator()
            animation.repeatCount = ValueAnimator.INFINITE
            animation.repeatMode = ValueAnimator.REVERSE
            animation.start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Simpan URI ke dalam Bundle
        currentImageUri?.let { outState.putParcelable(CURRENT_IMAGE_URI_KEY, it) }
        imageUri?.let { outState.putParcelable(IMAGE_URI_KEY, it) }
        outState.putBoolean(IS_GALLERY_SCAN_KEY, isGalleryScan)
    }


    fun startCamera() {
        if (allPermissionsGranted()) {
            currentImageUri = getImageUri(requireContext())
            isGalleryScan = false
            launcherIntentCamera.launch(currentImageUri!!)
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
            // Reset data cadangan karena pengambilan gambar baru berhasil
            lastResult = null
            lastImageUri = null
        } else {
            // Jika kamera dibatalkan DAN kita punya data cadangan dari Retake
            if (lastResult != null && lastImageUri != null) {
                val intent = Intent(requireContext(), ResultActivity::class.java)
                intent.putExtra("result", lastResult)
                intent.putExtra("imageUri", lastImageUri)
                intent.putExtra("is_gallery_scan", isGalleryScan)
                launcherResultActivity.launch(intent)
                
                // Reset setelah digunakan
                lastResult = null
                lastImageUri = null
            } else {
                currentImageUri = null
                showToast("Pengambilan gambar dibatalkan")
            }
        }
    }

    private fun startGallery() {
        isGalleryScan = true
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        val chooser = Intent.createChooser(intent, "Pilih gambar")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg = result.data?.data as Uri
            selectedImg.let { uri ->
                currentImageUri = uri
                startCrop(selectedImg)
            }
        } else {
            // Jika gallery dibatalkan DAN kita punya data cadangan dari Retake
            if (lastResult != null && lastImageUri != null) {
                val intent = Intent(requireContext(), ResultActivity::class.java)
                intent.putExtra("result", lastResult)
                intent.putExtra("imageUri", lastImageUri)
                intent.putExtra("is_gallery_scan", isGalleryScan)
                launcherResultActivity.launch(intent)

                // Reset setelah digunakan
                lastResult = null
                lastImageUri = null
            }
        }
    }

    private fun startCrop(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))

        val uCrop = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(224, 224)
            .getIntent(requireContext())

        cropImageResultLauncher.launch(uCrop)
    }

    private val cropImageResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                currentImageUri = it
                showImage()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            cropError?.let { showToast("Ada kesalahan crop gambar: ${it.message}") }
        } else if (result.resultCode == RESULT_CANCELED) {
            showToast("Crop dibatalkan")
            if (lastResult != null && lastImageUri != null) {
                val intent = Intent(requireContext(), ResultActivity::class.java)
                intent.putExtra("result", lastResult)
                intent.putExtra("imageUri", lastImageUri)
                intent.putExtra("is_gallery_scan", isGalleryScan)
                launcherResultActivity.launch(intent)

                lastResult = null
                lastImageUri = null
            } else {
                binding.gambaruplode.setImageResource(R.drawable.baseline_image_24)
                binding.buttonAnalisa.visibility = View.GONE
            }
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            binding.gambaruplode.setImageURI(it)
            binding.buttonAnalisa.visibility = View.VISIBLE
            // Pastikan imageUri (data cadangan) sinkron jika diperlukan
            imageUri = it
        }
    }

    private fun uploadImage() {
        val uriToUpload = currentImageUri ?: imageUri
        uriToUpload?.let { uri ->
            try {
                val imageFile = uriToFile(uri, requireContext())
                showLoading(true)
                val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    imageFile.name,
                    requestImageFile
                )

                lifecycleScope.launch {
                    try {
                        val apiService = ApiConfig.getApiService()
                        val response = apiService.uploadImage(multipartBody)

                        // Navigate to ResultActivity with data
                        val intent = Intent(requireContext(), ResultActivity::class.java)
                        intent.putExtra("result", response)
                        intent.putExtra("imageUri", uri.toString()) // Send image URI
                        intent.putExtra("is_gallery_scan", isGalleryScan)
                        launcherResultActivity.launch(intent)

                    } catch (e: HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        if (errorBody?.contains("LOW_CONFIDENCE") == true || errorBody?.contains("please try again later") == true) {
                            showToast("Makanan tidak terdeteksi. Coba foto makanan dengan jelas.")
                        } else {
                            try {
                                val errorResponse = Gson().fromJson(errorBody, FileUploadResponse::class.java)
                                showToast(errorResponse.message)
                            } catch (parseException: Exception) {
                                showToast("Terjadi kesalahan pada server.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Upload Image", "Unexpected error: ${e.message}")
                        showToast("An unexpected error occurred. Please try again.")
                    } finally {
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload Image", "Error converting URI to file: ${e.message}")
                showToast("Gagal memproses gambar. Silakan coba lagi.")
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private var lastResult: FileUploadResponse? = null
    private var lastImageUri: String? = null

    private val launcherResultActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ResultActivity.RESULT_RETAKE) {
            val intent = result.data
            lastResult = intent?.getSerializableExtra("last_result") as? FileUploadResponse
            lastImageUri = intent?.getStringExtra("last_imageUri")
            val wasGalleryScan = intent?.getBooleanExtra("is_gallery_scan", false) ?: false
            
            if (wasGalleryScan) {
                startGallery()
            } else {
                startCamera()
            }
        } else if (result.resultCode == ResultActivity.RESULT_GO_TO_DASHBOARD) {
            // Pindah ke Dashboard
            findNavController().navigate(R.id.navigation_dashboard)
        } else {
            // Jika user keluar dari ResultActivity (Back, Close, atau Add Meal)
            // Bersihkan data scan agar user mulai dari awal lagi
            clearScanData()
        }
    }

    private fun clearScanData() {
        currentImageUri = null
        imageUri = null
        binding.gambaruplode.setImageResource(R.drawable.baseline_image_24)
        binding.buttonAnalisa.visibility = View.GONE
    }

    fun onError(error: String) {
        binding.progressIndicator.visibility = View.GONE
        showToast("Terjadi kesalahan: $error")
    }

    fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        TODO("Not yet implemented")
    }

    private fun moveToResult(result: String) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
            putExtra(ResultActivity.EXTRA_RESULT, result)
        }
        startActivity(intent)
    }

    private fun showToast(message: String?) {
        if (!message.isNullOrBlank()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
        private const val CURRENT_IMAGE_URI_KEY = "currentImageUri"
        private const val IMAGE_URI_KEY = "imageUri"
        private const val IS_GALLERY_SCAN_KEY = "isGalleryScan"
    }

}
