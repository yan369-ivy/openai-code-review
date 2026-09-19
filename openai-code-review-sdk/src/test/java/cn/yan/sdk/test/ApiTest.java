package cn.yan.sdk.test;

import cn.yan.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import cn.yan.sdk.infrastructure.weixin.dto.TemplateMessageDTO;
import cn.yan.sdk.types.utils.BearerTokenUtils;
import cn.yan.sdk.types.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ApiTest {

    public static void main(String[] args) {
        System.out.println(BearerTokenUtils.getDeepSeekToken());
    }

    @Ignore("Manual test: calls DeepSeek API and requires DEEPSEEK_API_KEY.")
    @Test
    public void test_http() throws IOException {
        String authHeaderName = BearerTokenUtils.getDeepSeekAuthHeaderName();
        String authHeaderValue = BearerTokenUtils.getDeepSeekAuthHeaderValue();

        URL url = new URL(BearerTokenUtils.getDeepSeekBaseUrl() + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty(authHeaderName, authHeaderValue);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String code = "1+1";

        String jsonInpuString = "{"
                + "\"model\":\"deepseek-flash\","
                + "\"messages\": ["
                + "    {"
                + "        \"role\": \"user\","
                + "        \"content\": \"你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: " + code + "\""
                + "    }"
                + "],"
                + "\"stream\": false"
                + "}";

        try(OutputStream os = connection.getOutputStream()){
            byte[] input = jsonInpuString.getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null){
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();

        ChatCompletionSyncResponseDTO response = JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        System.out.println(response.getChoices().get(0).getMessage().getContent());

    }

    @Ignore("Manual test: sends a real WeChat template message.")
    @Test
    public void test_wx() {
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);

        TemplateMessageDTO message = new TemplateMessageDTO(
                System.getenv("WEIXIN_TOUSER"),
                System.getenv("WEIXIN_TEMPLATE_ID")
        );
        message.setUrl("https://github.com/yan369-ivy/openai-code-review-log/blob/main/2026-09-16/HZJQmE4eBGy9.md");
        message.put("project","big-market");
        message.put("review","feat: 新加功能");

        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }

    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            try (Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
