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
        try {
            File file = ResourceUtils.getFile("classpath:badwords.txt");
            List<String> blackList = loadblackListFromFile(file);
            WORD_TREE.addWords(blackList);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> loadblackListFromFile(File file) {
        List<String> blackList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                blackList.add(line.trim());
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加载违禁词失败");
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
     * @param content
     * @return
     */
    public static List<String> extraForbbidWords(String content) {
        return WORD_TREE.matchAll(content);
    }
}
