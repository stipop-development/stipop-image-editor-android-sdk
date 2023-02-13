package io.stipop_image_editor.demo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.stipop_image_editor.SEDelegate
import io.stipop_image_editor.StipopImageEditor
import io.stipop_image_editor.core.manager.SEAuthManager
import io.stipop_image_editor.demo.databinding.ActivityMainBinding
import io.stipop_image_editor.demo.stipop_auth.SAuthRepository
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.Locale


class MainActivity: AppCompatActivity(), SEDelegate {

    private val INTENT_SELECT_IMAGE = 1000

    private lateinit var binding: ActivityMainBinding

    private val writePermission: Int by lazy { ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE ) }
    private val readPermission: Int by lazy { ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE ) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        requestPermission()
        initListener()
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            1
        )
    }

    private fun initListener() {
        binding.selectImageButton.setOnClickListener {
            if (writePermission == PackageManager.PERMISSION_DENIED && readPermission == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Please allow permissions", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), INTENT_SELECT_IMAGE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                INTENT_SELECT_IMAGE -> {
                    val imageUri = data?.data
                    if(imageUri != null) {
                        showEditor(context = this,
                            imageUri = imageUri,
                            delegate = this)
                    }
                }
            }
        }
    }

    /**
     * StipopImageEditor.showEditor
     * :Move to Image Editing page.
     *
     * @param context: Put your current activity's context not applicationContext.
     * @param imageUri: Put the uri of the image you want to edit.
     * @param delegate: Set listener's delegate.
     * @param userId: Put userId if you want. (defValue: "-1")
     * @param locale: Put your locale info if you want. (defValue: Locale.getDefault())
     * @param fileName: You can custom your StipopImageEditor.json's fileName (defValue: "StipopImageEditor.json")
     */
    private fun showEditor(context: Context,
                           imageUri: Uri,
                           delegate: SEDelegate?,
                           userId: String = "-1",
                           locale: Locale = Locale.getDefault(),
                           fileName: String = "StipopImageEditor.json"){
        StipopImageEditor.showEditor(
            context = context,
            imageUri = imageUri,
            delegate = delegate,
            userId = userId,
            locale = locale,
            fileName = fileName)
    }

    /**
     * seCanceled
     * :If Image Editing is canceled, this method will be called.
     */
    override fun seCanceled() {

    }

    /**
     * seFinished
     * :If Image Editing is finished, finished image will be arrived in here.
     *
     * @param imageBitmap: finished image's bitmap.
     */
    override fun seFinished(imageBitmap: Bitmap) {
        binding.imageView.setImageBitmap(imageBitmap)
    }

    /**
     * seHttpException (For SAuth user only)
     * :If HttpException occurs in Stipop, occurred HttpException will be received in here.
     *
     * @process 1: Check whether error code is 401(UnAuthorization).
     *          2: If code is 401, issue new AccessToken.
     *          3: Set new AccessToken to StipopImageEditor. (Using StipopImageEditor.setAccessToken method)
     *          4: Rerequest to the API where error occurred. (Using SEAuthManager.reRequest method)
     * @param api: Where HttpException occurred.
     * @param exception: HttpException occurred.
     */
    override fun seHttpException(apiName: String, exception: HttpException) {
        when(exception.code()){
            401 -> {
                CoroutineScope(Job() + Dispatchers.IO).launch {
                    while(SAuthRepository.getIsSAuthWorking()){
                        delay(50)
                    }
                    val accessToken = SAuthRepository.getAccessTokenIfOverExpiryTime(userId = StipopImageEditor.user.userId)
                    StipopImageEditor.setAccessToken(accessToken = accessToken)
                    SEAuthManager.reRequest(apiName)
                }
            }
        }
    }
}