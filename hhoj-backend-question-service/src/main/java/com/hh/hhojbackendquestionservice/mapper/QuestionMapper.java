package com.hh.hhojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hh.hhojbackendmodel.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
* @author ybb
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-12-10 21:28:35
* @Entity com.yupi.hhoj.model.entity.Question
*/
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

}




