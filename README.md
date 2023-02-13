![Github_stipop_SDK](https://user-images.githubusercontent.com/42525347/145160731-acbe1005-48f5-4c9e-93b7-8ce2c7d6dcb8.png)

<h1>Stipop Image Editor SDK for Android</h1>

<a href="https://android-arsenal.com/api?level=21"><img alt="AndroidMinApi" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a></br>
Stipop Image Editor SDK provides over 150,000 stickers that can be easily integrated into mobile app. Bring fun to your mobile app with stickers loved by millions of users worldwide.

Requirements
-------------------
- Kotlin
- Android + (API level 21) or higher
- Java 7 or higher
- Support androidx only
- Gradle 3.4.0 or higher

Getting Started
--------
- Contact us to download StipopImageEditor.plist file. (tech-support@stipop.io)

Try Demo
--------
1. Clone this repository.<br/>
2. Add 'StipopImageEditor.json' file into the assets folder you created.
3. Build and run 'demo' on your device.<br/><br/>



Including in your project
--------
Use Gradle:

```gradle
allprojects {
  repositories {
     maven { url = "https://stipop.jfrog.io/artifactory/stipop-image-editor" }
  }
}

dependencies {
  // like 0.0.1.-beta.1 Please check latest release!
  implementation 'io.stipop:stipop-image-editor-sdk:{latest_version}'
  implementation 'com.google.android.material:material:1.4.0' // If you do not use Material Theme, StickerSearchView will not work.
  implementation 'androidx.paging:paging-runtime-ktx:3.1.1'   // If you do not implement this, StickerSearchView will not work.
}
```
<br/>

How do I use StipopImageEditor SDK?
-------------------

1. Add 'StipopImageEditor.json' file into the assets folder you created.
2. Update your 'AndroidManifest.xml' to specify application class.<br>
   Please put 'tools:replace="android:theme" to avoid conflict theme file in the application setting area.

```xml
<application
        android:name=".{YourApplicationClass}"
        ...
        tools:replace="android:theme">
```
3. Then implement 'SEDelegate' interface at where you want to call Image Editor.(If you do not use SAuth, put seHttpException empty)
```kotlin
class YourActivity : AppCompatActivity(), SEDelegate {

   private lateinit var binding: ActivityMainBinding

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
```

4. Then, call 'StipopImageEditor.showEditor()' method when you want to edit image.
```kotlin
class YourActivity: AppCompatActivity(), SEDelegate {

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
    ...
}
```
<br/>

## Contact us

- For more information, visit [Stipop Documentation][1].
- Email us at tech-support@stipop.io if you need our help.

[1]: https://docs.stipop.io/en/sdk/android/get-started/before-you-begin
