package cn.yan.sdk.infrastructure.openai.impl;

import cn.yan.sdk.infrastructure.openai.IOpenAI;
import cn.yan.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.yan.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import cn.yan.sdk.types.utils.BearerTokenUtils;
import com.alibaba.fastjson2.JSON;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatDeepSeek implements IOpenAI {

    private final String apiHost;
    private final String apiKey;

    public ChatDeepSeek(String apiHost, String apiKey) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        String token = BearerTokenUtils.getToken(apiKey);

        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        try {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("OpenAI request failed with HTTP " + responseCode + ": " + content);
            }
            return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        } finally {
            connection.disconnect();
        }
    }
}
