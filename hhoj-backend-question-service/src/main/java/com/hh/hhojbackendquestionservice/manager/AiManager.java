package com.hh.hhojbackendquestionservice.manager;

/**
 * @author 黄昊
 * @version 1.0
 **/

import cn.hutool.core.collection.CollUtil;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendmodel.vo.AiQuestionVO;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于对接 AI 平台
 */
@Service
@Slf4j
public class AiManager {

    @Resource
    private ArkService aiService;

    private final String DEFAULT_MODEL = "deepseek-v3-241226";
//
//    @Resource
//    @Lazy
//    private DeepSeekClient deepSeekClient;

    @Resource
    private SparkClient sparkClient;

    public static final String PRECONDITION = "现在你是一位精通OJ竞赛题目的算法专家，接下来我会按照以下固定格式给你发送内容：\n" +
            "题目标题：\n" +
            "{该算法题的标题}\n" +
            "题目内容:\n" +
            "{该算法题的具体内容}\n" +
            "题目使用语言:\n" +
            "{解决该题目所使用的编程语言}\n" +
            "请认真根据这两部分内容，必须严格按照以下指定格式生成markdown内容（此外不要输出任何多余的开头、结尾、注释）\n" +
            "【【【【【\n" +
            "明确的代码分析，越详细越好，不要生成多余的注释\n" +
            "【【【【【\n" +
            "解答该题目对应的代码，代码相关参数是通过命令行传来的且写在main方法中，只需生成要求编程语言的代码\n";

    public static final String PRECONDITION_STREAM = "现在你是一位精通Leecode题目的算法专家，接下来我会按照以下固定格式给你发送内容：\n" +
            "题目标题：\n" +
            "{该算法题的标题}\n" +
            "题目内容:\n" +
            "{该算法题的具体内容}\n" +
            "题目使用语言:\n" +
            "{解决该题目所使用的编程语言}\n" +
            "请根据这两部分内容，严格按照以下指定格式生成markdown内容（此外不要输出任何多余的开头、结尾、注释）同时不要使用这个符号 】\n" +
            "{明确的代码分析，越详细越好，不要生成多余的注释}\n" +
            "{解答该题目对应的代码，代码相关参数是通过args参数的方式传来的，只需生成要求编程语言的代码}\n";

//    public String sendMesToAIUseDeepSeek(final String content) {
//        ChatCompletionRequest request = ChatCompletionRequest.builder()
//                .addUserMessage(content).build();
//        // 同步调用
//        return deepSeekClient.chatCompletion(request).execute().content();
//    }
    public String sendMesToAIUseXingHuo(final String content) {
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(content));
        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传,取值为[1,4096],默认为2048
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                // 指定请求版本，默认使用最新2.0版本
                .apiVersion(SparkApiVersion.V3_5)
                .build();
        // 同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        String responseContent = chatResponse.getContent();
        log.info("星火 AI 返回的结果 {}", responseContent);
        return responseContent;
    }
    /**
     * 调用 AI 接口，获取响应字符串
     *
     * @param userPrompt
     * @return
     */
    public String doChat(String userPrompt) {
        return doChat("", userPrompt, DEFAULT_MODEL);
    }

    /**
     * 调用 AI 接口，获取响应字符串
     *
     * @param systemPrompt
     * @param userPrompt
     * @return
     */
    public String doChat(String systemPrompt, String userPrompt) {
        return doChat(systemPrompt, userPrompt, DEFAULT_MODEL);
    }

    /**
     * 调用 AI 接口，获取响应字符串
     *
     * @param systemPrompt
     * @param userPrompt
     * @param model
     * @return
     */
    public String doChat(String systemPrompt, String userPrompt, String model) {
        // 构造消息列表
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemPrompt).build();
        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userPrompt).build();
        messages.add(systemMessage);
        messages.add(userMessage);
        // 构造请求
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
//                .model("deepseek-v3-241226")
                .model(model)
                .messages(messages)
                .build();
        // 调用接口发送请求
        List<ChatCompletionChoice> choices = aiService.createChatCompletion(chatCompletionRequest).getChoices();
        if (CollUtil.isNotEmpty(choices)) {
            return (String) choices.get(0).getMessage().getContent();
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 调用失败，没有返回结果");
//        // shutdown service after all requests is finished
//        aiService.shutdownExecutor();
    }

    /**
     * 获取 AI 生成结果
     *
     * @param
     * @param title
     * @param content
     * @return
     */
    public AiQuestionVO getGenResult(final String title, final String content, final String language, final Long questionId) {
        String promote = AiManager.PRECONDITION + "标题 " + title + " \n内容: " + content + "\n编程语言: " + language;
        String resultData = sendMesToAIUseXingHuo(promote);
        log.info("AI 生成的信息: {}", resultData);
        String genResult = null;
        String genCode = resultData;
        if (resultData.split("'【【【【【'").length >= 3) {
            genCode =resultData.split("'【【【【【'")[1].trim();;
            genResult = resultData.split("'【【【【【'")[2].trim();
        }
        return new AiQuestionVO(genResult, genCode, questionId);
    }
    /**
     * 获取 AI 生成结果
     *
     * @param
     * @param title
     * @param content
     * @return
     */
    public AiQuestionVO getGenResultByDeepSeek(final String title, final String content, final String language, final Long questionId) {
        String promote = AiManager.PRECONDITION + "标题 " + title + " \n内容: " + content + "\n编程语言: " + language;
        String resultData = doChat(promote);
        log.info("AI 生成的信息: {}", resultData);
        String genResult = null;
        String genCode = resultData;
        if (resultData.split("【【【【【").length >= 3) {
            genResult =resultData.split("【【【【【")[1].trim();;
            genCode = resultData.split("【【【【【")[2].trim();
        }
        return new AiQuestionVO(genResult, genCode, questionId);
    }


//    public Flux<String> getGenResultStream(final String title, final String content,
//                                           final String language) {
//        String promote = AiManager.PRECONDITION_STREAM + "标题 " + title + " \n内容: " + content + "\n编程语言: " + language;
//        return deepSeekClient.chatFluxCompletion(promote)
//                .flatMap(response -> {
//                    // 安全提取内容
//                    String contentChunk =
//                            Optional.ofNullable(response.choices())
//                                    .filter(choices -> !choices.isEmpty())
//                                    .map(choices -> choices.get(0))
//                                    .map(ChatCompletionChoice::delta)
//                                    .map(Delta::content)
//                                    .orElse("")
//                                    .replace("\\n", "\n")  // 处理转义换行符
//                                    .replace(" ", " ");  // 保留空格为HTML实体
//                    return Flux.just(contentChunk);
//                })
//                .onErrorResume(e -> {
//                    log.error("AI流处理异常", e);
//                    return Flux.just("[系统异常] 服务暂时不可用");
//                });
//    }

}
