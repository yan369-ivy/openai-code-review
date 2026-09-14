package cn.yan.sdk.types.utils;

public class BearerTokenUtils {

    public static final String DEEPSEEK_API_KEY = "DEEPSEEK_API_KEY";
    public static final String DEEPSEEK_BASE_URL = "DEEPSEEK_BASE_URL";
    public static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";

    private BearerTokenUtils() {
    }

    /**
     * DeepSeek does not need GLM-style JWT signing.
     * Keep this method so old call sites can pass through an existing token value.
     */
    public static String getToken(String token) {
        return requireText(token, "DeepSeek token must not be blank.");
    }

    /**
     * DeepSeek does not use apiKey + apiSecret JWT signing.
     * The first argument is returned as-is for compatibility with old GLM call sites.
     */
    public static String getToken(String token, String ignoredSecret) {
        return getToken(token);
    }

    public static String getDeepSeekToken() {
        String apiKey = System.getenv(DEEPSEEK_API_KEY);
        if (hasText(apiKey)) {
            return apiKey.trim();
        }

        throw new IllegalStateException("Please set DEEPSEEK_API_KEY.");
    }

    public static String getDeepSeekAuthHeaderName() {
        return "Authorization";
    }

    public static String getDeepSeekAuthHeaderValue() {
        return "Bearer " + getDeepSeekToken();
    }

    public static String getDeepSeekBaseUrl() {
        String baseUrl = System.getenv(DEEPSEEK_BASE_URL);
        return hasText(baseUrl) ? trimTrailingSlash(baseUrl.trim()) : DEFAULT_DEEPSEEK_BASE_URL;
    }

    public static String getOpenAiToken() {
        return getDeepSeekToken();
    }

    public static String getOpenAiAuthHeaderName() {
        return getDeepSeekAuthHeaderName();
    }

    public static String getOpenAiAuthHeaderValue() {
        return getDeepSeekAuthHeaderValue();
    }

    public static String getOpenAiBaseUrl() {
        return getDeepSeekBaseUrl();
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

}
