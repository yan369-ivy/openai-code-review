package cn.yan.sdk.infrastructure.openai;

import cn.yan.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.yan.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

public interface IOpenAI {

    ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception;

}
