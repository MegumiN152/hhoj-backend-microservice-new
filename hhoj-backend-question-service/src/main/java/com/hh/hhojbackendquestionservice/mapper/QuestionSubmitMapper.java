package com.hh.hhojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.vo.UserLeaderboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author ybb
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2024-12-10 21:28:49
* @Entity com.yupi.hhoj.model.entity.QuestionSubmit
*/
@Mapper
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    @Select("SELECT\n" +
            "u.id as userId,\n" +
            "u.userName,\n" +
            "u.userAvatar,\n" +
            "COUNT(DISTINCT CASE WHEN qs.status = 2 THEN qs.questionId END) AS solvedCount,\n" +
            "COUNT(qs.id) as submitCount\n" +
            "FROM question_submit qs\n" +
            "JOIN `user` u ON qs.userId=u.id\n" +
            "GROUP BY u.id\n" +
            "ORDER BY solvedCount DESC,submitCount ASC\n" +
            "LIMIT #{limit}")
    List<UserLeaderboardVO> getLeaderBoard(@Param("limit") int i);
}




