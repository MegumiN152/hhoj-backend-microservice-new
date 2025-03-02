package com.hh.hhojbackendmodel.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *

 */
@Data
public class CommentDeleteRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    private static final long serialVersionUID = 1L;

}