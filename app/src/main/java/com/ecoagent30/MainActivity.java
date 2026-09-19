package com.ecoagent30;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.*;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;

    private ImageView preview;
    private TextView resultText;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText addressInput;
    private EditText totalInput;
    private EditText orderInput;

    private TextRecognizer textRecognizer;
    private BarcodeScanner barcodeScanner;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();

        textRecognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );

        barcodeScanner = BarcodeScanning.getClient();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ar"));
            }
        });
    }

    private void buildInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("🚚 Eco Agent 3.0");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 10, 10, 20);

        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "إدارة الطلبات والتواصل مع العملاء\n" +
                "معكم عامل التوصيل"
        );
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 0, 10, 20);

        content.addView(subtitle);

        Button cameraButton = new Button(this);
        cameraButton.setText("📷 تصوير الطلب");
        content.addView(cameraButton);

        Button galleryButton = new Button(this);
        galleryButton.setText("🖼️ اختيار صورة من الهاتف");
        content.addView(galleryButton);

        Button qrButton = new Button(this);
        qrButton.setText("🔳 قراءة QR / Barcode");
        content.addView(qrButton);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setPadding(0, 15, 0, 15);

        content.addView(preview);

        nameInput = createInput("اسم العميل");
        phoneInput = createInput("رقم الهاتف");
        addressInput = createInput("العنوان");
        totalInput = createInput("المبلغ الإجمالي");
        orderInput = createInput("رقم الطلب / التتبع");

        content.addView(nameInput);
        content.addView(phoneInput);
        content.addView(addressInput);
        content.addView(totalInput);
        content.addView(orderInput);

        Button saveButton = new Button(this);
        saveButton.setText("💾 حفظ الطلب");
        content.addView(saveButton);

        Button callButton = new Button(this);
        callButton.setText("📞 الاتصال بالعميل");
        content.addView(callButton);

        Button whatsappButton = new Button(this);
        whatsappButton.setText("💬 فتح WhatsApp");
        content.addView(whatsappButton);

        Button messageButton = new Button(this);
        messageButton.setText("✉️ رسالة تأكيد الطلب");
        content.addView(messageButton);

        Button deliveryButton = new Button(this);
        deliveryButton.setText("🚚 رسالة: الطلب في الطريق");
        content.addView(deliveryButton);

        Button locationButton = new Button(this);
        locationButton.setText("📍 فتح موقع العنوان");
        content.addView(locationButton);

        Button speakButton = new Button(this);
        speakButton.setText("🔊 قراءة بيانات الطلب");
        content.addView(speakButton);

        Button clearButton = new Button(this);
        clearButton.setText("🗑️ مسح البيانات");
        content.addView(clearButton);

        resultText = new TextView(this);
        resultText.setTextSize(15);
        resultText.setPadding(10, 20, 10, 20);

        content.addView(resultText);

        scroll.addView(content);
        root.addView(scroll);

        setContentView(root);

        cameraButton.setOnClickListener(v -> openCamera());

        galleryButton.setOnClickListener(v -> openGallery());

        qrButton.setOnClickListener(v -> showQrInstructions());

        saveButton.setOnClickListener(v -> saveOrder());

        callButton.setOnClickListener(v -> callCustomer());

        whatsappButton.setOnClickListener(v -> openWhatsApp());

        messageButton.setOnClickListener(v ->
                sendMessage(
                        "السلام عليكم، معكم عامل التوصيل بخصوص طلبكم."
                )
        );

        deliveryButton.setOnClickListener(v ->
                sendMessage(
                        "السلام عليكم، معكم عامل التوصيل. " +
                        "طلبكم الآن في الطريق إليكم."
                )
        );

        locationButton.setOnClickListener(v -> openLocation());

        speakButton.setOnClickListener(v -> speakOrder());

        clearButton.setOnClickListener(v -> clearFields());
    }

    private EditText createInput(String hint) {

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(16);
        input.setPadding(15, 10, 15, 10);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 5, 0, 5);
        input.setLayoutParams(params);

        return input;
    }

    private void openCamera() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(
                    this,
                    "الكاميرا غير متاحة",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                Intent.createChooser(intent, "اختر صورة الطلب"),
                GALLERY_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        try {

            if (requestCode == CAMERA_REQUEST) {

                Bitmap bitmap =
                        (Bitmap) data.getExtras().get("data");

                if (bitmap != null) {
                    preview.setImageBitmap(bitmap);
                    processImage(bitmap);
                }

            } else if (requestCode == GALLERY_REQUEST) {

                Uri uri = data.getData();

                if (uri != null) {

                    Bitmap bitmap =
                            MediaStore.Images.Media.getBitmap(
                                    getContentResolver(),
                                    uri
                            );

                    preview.setImageBitmap(bitmap);
                    processImage(bitmap);
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "تعذر قراءة الصورة",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void processImage(Bitmap bitmap) {

        resultText.setText(
                "⏳ جارٍ تحليل الطلب..."
        );

        InputImage image =
                InputImage.fromBitmap(bitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(result -> {

                    String text = result.getText();

                    parseOrderText(text);

                    resultText.setText(
                            "✅ تم استخراج النص:\n\n" + text
                    );
                })
                .addOnFailureListener(e -> {

                    resultText.setText(
                            "تعذر استخراج النص."
                    );
                });

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {

                    if (!barcodes.isEmpty()) {

                        StringBuilder codes =
                                new StringBuilder();

                        for (com.google.mlkit.vision.barcode.common.Barcode barcode
                                : barcodes) {

                            if (barcode.getRawValue() != null) {

                                codes.append(
                                        barcode.getRawValue()
                                ).append("\n");
                            }
                        }

                        if (codes.length() > 0) {

                            orderInput.setText(
                                    codes.toString().trim()
                            );

                            Toast.makeText(
                                    this,
                                    "🔳 تم العثور على Barcode / QR",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }

    private void parseOrderText(String text) {

        if (text == null) {
            return;
        }

        String clean =
                text.replace("\n", " ")
                        .replace("\r", " ");

        String phone =
                findPhone(clean);

        if (!phone.isEmpty()) {
            phoneInput.setText(phone);
        }

        String price =
                findPrice(clean);

        if (!price.isEmpty()) {
            totalInput.setText(price);
        }

        String order =
                findOrderNumber(clean);

        if (!order.isEmpty()) {
            orderInput.setText(order);
        }

        if (addressInput.getText().toString().trim().isEmpty()) {

            String[] lines =
                    text.split("\\r?\\n");

            if (lines.length > 0) {

                StringBuilder address =
                        new StringBuilder();

                for (String line : lines) {

                    String value = line.trim();

                    if (value.length() > 5) {
                        address.append(value).append(" ");
                    }
                }

                if (address.length() > 0) {
                    addressInput.setText(
                            address.toString().trim()
                    );
                }
            }
        }
    }

    private String findPhone(String text) {

        Pattern pattern = Pattern.compile(
                "(0[5-7][0-9]{8})|" +
                "(\\+213[5-7][0-9]{8})"
        );

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {

            String value =
                    matcher.group();

            if (value.startsWith("+213")) {
                return "0" + value.substring(4);
            }

            return value;
        }

        return "";
    }

    private String findPrice(String text) {

        Pattern pattern = Pattern.compile(
                "(\\d+[\\.,]?\\d*)\\s*" +
                "(DA|دج|DZD)"
        );

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    private String findOrderNumber(String text) {

        Pattern pattern = Pattern.compile(
                "(order|commande|طلب|tracking|track|رقم)" +
                "\\s*[:#-]?\\s*([A-Za-z0-9\\-_]{4,})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(2);
        }

        return "";
    }

    private void saveOrder() {

        getPreferences(MODE_PRIVATE)
                .edit()
                .putString(
                        "name",
                        nameInput.getText().toString()
                )
                .putString(
                        "phone",
                        phoneInput.getText().toString()
                )
                .putString(
                        "address",
                        addressInput.getText().toString()
                )
                .putString(
                        "total",
                        totalInput.getText().toString()
                )
                .putString(
                        "order",
                        orderInput.getText().toString()
                )
                .apply();

        Toast.makeText(
                this,
                "✅ تم حفظ الطلب",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void callCustomer() {

        String phone =
                phoneInput.getText().toString().trim();

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    "أدخل رقم العميل أولاً",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + phone)
                );

        startActivity(intent);
    }

    private void openWhatsApp() {

        String phone =
                phoneInput.getText().toString().trim();

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    "أدخل رقم العميل أولاً",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        phone = phone.replace("+", "");

        if (phone.startsWith("0")) {
            phone = "213" + phone.substring(1);
        }

        String url =
                "https://wa.me/" + phone;

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                );

        startActivity(intent);
    }

    private void sendMessage(String message) {

        String phone =
                phoneInput.getText().toString().trim();

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    "أدخل رقم العميل أولاً",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + phone)
                );

        intent.putExtra(
                "sms_body",
                message
        );

        startActivity(intent);
    }

    private void openLocation() {

        String address =
                addressInput.getText().toString().trim();

        if (address.isEmpty()) {

            Toast.makeText(
                    this,
                    "أدخل العنوان أولاً",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Uri uri =
                Uri.parse(
                        "geo:0,0?q=" +
                        Uri.encode(address)
                );

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        uri
                );

        startActivity(intent);
    }

    private void speakOrder() {

        String text =
                "اسم العميل " +
                nameInput.getText().toString() +
                ". رقم الهاتف " +
                phoneInput.getText().toString() +
                ". العنوان " +
                addressInput.getText().toString() +
                ". المبلغ " +
                totalInput.getText().toString();

        if (tts != null) {
            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "eco_order"
            );
        }
    }

    private void clearFields() {

        nameInput.setText("");
        phoneInput.setText("");
        addressInput.setText("");
        totalInput.setText("");
        orderInput.setText("");
        resultText.setText("");
        preview.setImageDrawable(null);

        Toast.makeText(
                this,
                "تم مسح البيانات",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showQrInstructions() {

        new AlertDialog.Builder(this)
                .setTitle("🔳 قراءة QR / Barcode")
                .setMessage(
                        "استخدم زر تصوير الطلب لالتقاط صورة تحتوي على QR أو Barcode. " +
                        "سيحاول التطبيق قراءة الرمز تلقائياً."
                )
                .setPositiveButton(
                        "حسناً",
                        null
                )
                .show();
    }

    @Override
    protected void onDestroy() {

        if (textRecognizer != null) {
            textRecognizer.close();
        }

        if (barcodeScanner != null) {
            barcodeScanner.close();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
