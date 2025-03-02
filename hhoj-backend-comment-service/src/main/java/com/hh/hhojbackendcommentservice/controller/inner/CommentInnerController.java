package com.hh.hhojbackendcommentservice.controller.inner;

import com.hh.hhojbackendserviceclient.service.CommentFeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 黄昊
 * @version 1.0
 **/
@RestController
@RequestMapping("/inner")
public class CommentInnerController implements CommentFeignClient {
}
