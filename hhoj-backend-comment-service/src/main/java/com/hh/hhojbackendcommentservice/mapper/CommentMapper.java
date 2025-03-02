package com.hh.hhojbackendcommentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hh.hhojbackendmodel.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
* @author ybb
* @description 针对表【comment(评论表)】的数据库操作Mapper
* @createDate 2024-12-19 23:56:18
* @Entity com.hhpi.hhoj.model.entity.Comment
*/
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}




