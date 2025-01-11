package com.example.coche1;

import com.google.gson.JsonObject;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class VisionApiDescription {
    // AIzaSyDji-Usyww-VewUsoQczyKeydnEM-0W6pc
    private static final String API_URL = "https://vision.googleapis.com/v1/images:annotate?key=AIzaSyDji-Usyww-VewUsoQczyKeydnEM-0W6pc";

    public static void sendImageToVisionAPI(String base64Image, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        // Construir el cuerpo de la solicitud
        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("content", base64Image);

        JsonObject feature = new JsonObject();
        feature.addProperty("type", "LABEL_DETECTION");

        JsonObject request = new JsonObject();
        request.add("image", imageContent);
        request.add("features", feature);

        JsonObject requests = new JsonObject();
        requests.add("requests", request);

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                requests.toString()
        );

        Request requestAPI = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();

        client.newCall(requestAPI).enqueue(callback);
    }

}
