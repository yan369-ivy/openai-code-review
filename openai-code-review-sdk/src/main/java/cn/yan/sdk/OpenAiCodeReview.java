package cn.yan.sdk;

import cn.yan.sdk.domain.model.ChatCompletionSyncResponse;
import cn.yan.sdk.types.utils.BearerTokenUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 入口
 */
public class OpenAiCodeReview {

    private static final String GITHUB_TOKEN = "GITHUB_TOKEN";
    private static final String CODE_TOKEN = "CODE_TOKEN";
    private static final String GITHUB_TOKEN_USERNAME = "x-access-token";

    public static void main(String[] args) throws Exception {
        System.out.println("测试执行");

        // 1. 代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        StringBuilder diffCode = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            diffCode.append(line);
        }

        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);

        System.out.println("diffCode：" + diffCode.toString());

        // 2. chatglm 代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code review：" + log);

        // 3. 写入评审日志
        String token = getGithubToken();
        String logUrl = writeLog(token, log);
        System.out.println("writeLog：" + logUrl);

    }

    private static String getGithubToken() {
        String token = System.getenv(GITHUB_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            token = System.getenv(CODE_TOKEN);
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Please set GITHUB_TOKEN or CODE_TOKEN.");
        }
        return token.trim();
    }

    private static String codeReview(String diffCode) throws Exception {

        String authHeaderName = BearerTokenUtils.getDeepSeekAuthHeaderName();
        String authHeaderValue = BearerTokenUtils.getDeepSeekAuthHeaderValue();

        URL url = new URL(BearerTokenUtils.getDeepSeekBaseUrl() + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty(authHeaderName, authHeaderValue);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-flash");
        requestBody.put("stream", false);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: " + diffCode);
        requestBody.put("messages", new JSONArray().fluentAdd(message));

        try(OutputStream os = connection.getOutputStream()){
            byte[] input = JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8);
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

        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);
        return response.getChoices().get(0).getMessage().getContent();

    }

    private static String writeLog(String token, String log) throws Exception {
        UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(GITHUB_TOKEN_USERNAME, token);

        Git git = Git.cloneRepository()
                .setURI("https://github.com/yan369-ivy/openai-code-review-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(credentialsProvider)
                .call();

        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdirs();
        }

        String fileName = generateRandomString(12) + ".md";
        File newFile = new File(dateFolder, fileName);
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(log);
        }

        git.add().addFilepattern(dateFolderName + "/" + fileName).call();
        git.commit().setMessage("Add new file via GitHub Actions").call();
        git.push().setCredentialsProvider(credentialsProvider).call();

        System.out.println("Changes have been pushed to the repository.");

        return "https://github.com/yan369-ivy/openai-code-review-log/blob/main/" + dateFolderName + "/" + fileName;
    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }


}
