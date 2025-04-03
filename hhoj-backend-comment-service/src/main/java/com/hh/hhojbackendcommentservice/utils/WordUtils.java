package com.hh.hhojbackendcommentservice.utils;

import cn.hutool.dfa.WordTree;
import com.hh.hhojbackendcommentservice.exception.BusinessException;
import com.hh.hhojbackendcommon.common.ErrorCode;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
public class WordUtils {
    // 定义一个静态常量，用于存储违禁词树
    private static final WordTree WORD_TREE;

    // 静态代码块，用于初始化违禁词树
    static {
        WORD_TREE = new WordTree();
        try (InputStream inputStream = WordUtils.class.getClassLoader().getResourceAsStream("badwords.txt")) {
            // 如果未找到违禁词文件，抛出异常
            if (inputStream == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "违禁词文件未找到");
            }
            // 从文件中加载违禁词列表
            List<String> blackList = loadBlackListFromStream(inputStream);
            // 将违禁词列表添加到违禁词树中
            WORD_TREE.addWords(blackList);
        } catch (IOException e) {
            // 如果加载违禁词库失败，抛出异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "违禁词库初始化失败");
        }
    }

    // 从输入流中加载违禁词列表
    private static List<String> loadBlackListFromStream(InputStream inputStream) throws IOException {
        List<String> blackList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            // 逐行读取文件内容，并将每行内容添加到违禁词列表中
            while ((line = reader.readLine()) != null) {
                blackList.add(line.trim());
            }
        }
        return blackList;
    }

    /**
     * 检测字符串中是否有违禁词
     *
     * @param content
     * @return
     */
    public static boolean containsBadWords(String content) {
        // 调用WORD_TREE的matchAll方法，传入content参数，返回违禁词列表
        return WORD_TREE.matchAll(content).isEmpty();
    }

    /**
     * 提取字符串中的违禁词
     * @param content 需要提取违禁词的字符串
     * @return 违禁词列表
     */
    public static List<String> extraForbbidWords(String content) {
        // 调用WORD_TREE的matchAll方法，传入content参数，返回违禁词列表
        return WORD_TREE.matchAll(content);
    }
}
