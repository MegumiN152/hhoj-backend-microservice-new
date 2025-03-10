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
    private static final WordTree WORD_TREE;

    static {
        WORD_TREE = new WordTree();
        try (InputStream inputStream = WordUtils.class.getClassLoader().getResourceAsStream("badwords.txt")) {
            if (inputStream == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "违禁词文件未找到");
            }
            List<String> blackList = loadBlackListFromStream(inputStream);
            WORD_TREE.addWords(blackList);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "违禁词库初始化失败");
        }
    }

    private static List<String> loadBlackListFromStream(InputStream inputStream) throws IOException {
        List<String> blackList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
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
