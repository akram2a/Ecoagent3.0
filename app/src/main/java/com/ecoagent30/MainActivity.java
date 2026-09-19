package com.ecoagent30;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int CAMERA_REQUEST = 1001;
    private static final int GALLERY_REQUEST = 1002;

    private EditText nameField;
    private EditText phoneField;
    private EditText addressField;
    private EditText orderField;
    private EditText totalField;

    private TextView resultText;
    private SharedPreferences preferences;

    private Bitmap currentBitmap;
    private Uri currentImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("orders", MODE_PRIVATE);

        buildInterface();
        loadSavedOrder();
    }

    private void buildInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setGravity(Gravity.TOP);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🚚 Eco Agent 3.0");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 25);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("إدارة الطلبات والتواصل مع العملاء");
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 25);
        root.addView(subtitle);

        Button cameraButton = new Button(this);
        cameraButton.setText("📷 تصوير الطلب");
        root.addView(cameraButton);

        Button galleryButton = new Button(this);
        galleryButton.setText("🖼️ اختيار صورة من الهاتف");
        root.addView(galleryButton);

        Button qrButton = new Button(this);
        qrButton.setText("🔎 قراءة QR / Barcode");
        root.addView(qrButton);

        nameField = createField("اسم العميل");
        phoneField = createPhoneField("رقم الهاتف");
        addressField = createField("العنوان");
        orderField = createField("تفاصيل الطلب");
        totalField = createField("المبلغ الإجمالي");

        root.addView(nameField);
        root.addView(phoneField);
        root.addView(addressField);
        root.addView(orderField);
        root.addView(totalField);

        Button saveButton = new Button(this);
        saveButton.setText("💾 حفظ الطلب");
        root.addView(saveButton);

        Button callButton = new Button(this);
        callButton.setText("📞 الاتصال بالعميل");
        root.addView(callButton);

        Button whatsappButton = new Button(this);
        whatsappButton.setText("💬 إرسال رسالة WhatsApp");
        root.addView(whatsappButton);

        Button smsButton = new Button(this);
        smsButton.setText("✉️ إرسال SMS");
        root.addView(smsButton);

        Button mapButton = new Button(this);
        mapButton.setText("📍 فتح العنوان على الخريطة");
        root.addView(mapButton);

        Button readButton = new Button(this);
        readButton.setText("🔊 قراءة بيانات الطلب");
        root.addView(readButton);

        Button clearButton = new Button(this);
        clearButton.setText("🗑️ مسح الطلب");
        root.addView(clearButton);

        resultText = new TextView(this);
        resultText.setTextSize(16);
        resultText.setPadding(10, 25, 10, 25);
        root.addView(resultText);

        setContentView(scrollView);

        cameraButton.setOnClickListener(v -> openCamera());

        galleryButton.setOnClickListener(v -> openGallery());

        qrButton.setOnClickListener(v -> scanBarcodeFromCurrentImage());

        saveButton.setOnClickListener(v -> saveOrder());

        callButton.setOnClickListener(v -> callCustomer());

        whatsappButton.setOnClickListener(v -> sendWhatsApp());

        smsButton.setOnClickListener(v -> sendSMS());

        mapButton.setOnClickListener(v -> openMap());

        readButton.setOnClickListener(v -> readOrder());

        clearButton.setOnClickListener(v -> clearOrder());
    }

    private EditText createField(String hint) {

        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(17);
        field.setPadding(20, 15, 20, 15);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);
        field.setLayoutParams(params);

        return field;
    }

    private EditText createPhoneField(String hint) {

        EditText field = createField(hint);
        field.setInputType(InputType.TYPE_CLASS_PHONE);

        return field;
    }

    private void openCamera() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(
                    this,
                    "لا توجد كاميرا متاحة",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openGallery() {

        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );

        intent.setType("image/*");

        startActivityForResult(intent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        try {

            if (requestCode == CAMERA_REQUEST) {

                Bundle extras = data.getExtras();

                if (extras != null) {

                    Object imageObject = extras.get("data");

                    if (imageObject instanceof Bitmap) {

                        currentBitmap = (Bitmap) imageObject;

                        resultText.setText(
                                "📷 تم التقاط صورة الطلب.\n" +
                                "جاري تحليل الصورة..."
                        );

                        processImage(currentBitmap);
                    }
                }

            } else if (requestCode == GALLERY_REQUEST) {

                currentImageUri = data.getData();

                if (currentImageUri != null) {

                    currentBitmap = MediaStore.Images.Media.get
