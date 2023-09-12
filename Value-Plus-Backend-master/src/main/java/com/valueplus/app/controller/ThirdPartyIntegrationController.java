package com.valueplus.app.controller;

import com.valueplus.domain.model.AgentDto;
import com.valueplus.domain.service.concretes.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "v1/integration", produces = MediaType.APPLICATION_JSON_VALUE)
public class ThirdPartyIntegrationController {

    private final UserService userService;


    @GetMapping("/super-agents/{agentCode}/users")
    public List<AgentDto> getUserBySuperAgentCode(@PathVariable("agentCode") String superAgentCode, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {
        log.debug("getUser() referralCode = {}", superAgentCode.toLowerCase());

        return userService.findAllUserBySuperAgentCode(superAgentCode.toLowerCase(), pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider())).getContent();
    }

}
