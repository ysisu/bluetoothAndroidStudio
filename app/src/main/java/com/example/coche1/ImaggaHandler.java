package com.example.coche1;

import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImaggaHandler {

    private static final String API_KEY = "acc_878c9a2d8c937f1";
    private static final String API_SECRET = "42f9f46ad4fa3f95887af552cbf211a8";
    private static final String BASE_URL = "https://api.imagga.com/v2";

    public void uploadImageAsync(String filePath, UploadImageCallback callback) {
        new UploadImageTask(callback).execute(filePath);
    }

    private class UploadImageTask extends AsyncTask<String, Void, String> {
        private final UploadImageCallback callback;

        public UploadImageTask(UploadImageCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String filePath = params[0];
                return uploadImage(filePath);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                callback.onUploadComplete(result);
            }
        }
    }

    public String uploadImage(String filePath) throws IOException {
        String credentialsToEncode = API_KEY + ":" + API_SECRET;
        String basicAuth = Base64.encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        String endpoint = "/uploads";
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "Image Upload";

        URL urlObject = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        File fileToUpload = new File(filePath);

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + fileToUpload.getName() + "\"" + crlf);
        request.writeBytes(crlf);

        InputStream inputStream = new FileInputStream(fileToUpload);
        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
            request.write(dataBuffer, 0, bytesRead);
        }

        request.writeBytes(crlf);
        request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
        request.flush();
        request.close();

        // Obtener la respuesta
        InputStream responseStream = new BufferedInputStream(connection.getInputStream());
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        responseStreamReader.close();
        connection.disconnect();

        return stringBuilder.toString();
    }

    public void getTagsAsync(String imageUploadId, GetTagsCallback callback) {
        new GetTagsTask(callback).execute(imageUploadId);
    }

    private class GetTagsTask extends AsyncTask<String, Void, String> {
        private final GetTagsCallback callback;

        public GetTagsTask(GetTagsCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String imageUploadId = params[0];
                return getTags(imageUploadId);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                callback.onGetTagsComplete(result);
            }
        }
    }

    public String getTags(String imageUploadId) throws IOException {
        String credentialsToEncode = API_KEY + ":" + API_SECRET;
        String basicAuth = Base64.encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        String endpoint = "/tags";
        String url = BASE_URL + endpoint + "?image_upload_id=" + imageUploadId;

        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Error en la respuesta: " + responseCode);
        }

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = connectionInput.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        connectionInput.close();
        connection.disconnect();

        return stringBuilder.toString();
    }

    public String extractTopTags(String jsonResponse) {
        String tagNames = "";
        try {
            // Convertir la respuesta JSON a un objeto JSONObject
            JSONObject jsonObject = new JSONObject(jsonResponse);

            // Obtener el array de tags
            JSONArray tagsArray = jsonObject.getJSONObject("result").getJSONArray("tags");

            // Recorrer las primeras 6 etiquetas y agregarlas a la lista
            for (int i = 0; i < Math.min(6, tagsArray.length()); i++) {
                JSONObject tagObject = tagsArray.getJSONObject(i);
                // Ir guardando en una string las tags para imprimir posteriormente
                 tagNames = tagNames + tagObject.getJSONObject("tag").getString("en") + ", ";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tagNames;
    }

    public String parseImageUploadId(String response) {
        String regex = "\"upload_id\":\"(.*?)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public interface UploadImageCallback {
        void onUploadComplete(String result);
    }

    public interface GetTagsCallback {
        void onGetTagsComplete(String result);
    }
}
