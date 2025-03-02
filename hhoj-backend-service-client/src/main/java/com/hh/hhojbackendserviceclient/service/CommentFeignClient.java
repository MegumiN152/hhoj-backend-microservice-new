package com.hh.hhojbackendserviceclient.service;

import org.springframework.cloud.openfeign.FeignClient;

/**
* @author ybb
* @description 针对表【comment(评论表)】的数据库操作Service
* @createDate 2024-12-19 23:56:18
 * hhoj-backend-comment-service
*/
@FeignClient(name="hhoj-backend-service-comment-service",path = "/api/comment/inner")
public interface CommentFeignClient {


}
